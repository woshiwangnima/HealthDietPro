#!/usr/bin/env python3
"""Extract CGM samples from vector daily-glucose curves in an AGP PDF.

This is intentionally a preview-only tool. It writes JSON and never connects to
or changes an Android device archive.
"""

from __future__ import annotations

import argparse
import json
import re
import unicodedata
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

from pypdf import PdfReader
from pypdf.generic import ContentStream

PLOT_LEFT = 75.0
PLOT_RIGHT = 617.0
PLOT_TOP = 10.0
PLOT_BOTTOM = 170.0
PLOT_VALUE_MAX = 25.0
SAMPLE_INTERVAL_MINUTES = 5

REPORT_RANGE = re.compile(
    r"\u76d1\u6d4b\u65f6\u95f4\s*:\s*(\d{4})/(\d{1,2})/(\d{1,2})\s*-\s*"
    r"(\d{4})/(\d{1,2})/(\d{1,2})"
)
DAILY_SUMMARY = re.compile(
    r"MBG\s*\u5e73\u5747\s*\u8461\s*\u8404\u7cd6\u503c\s*(\d+(?:\.\d+)?)\s*mmol/L.*?"
    r"(\d{1,2})\u6708(\d{1,2})\u65e5",
    re.DOTALL,
)


def endpoint(command: bytes, values: list[object]) -> tuple[float, float] | None:
    if command in {b"m", b"l"}:
        return float(values[0]), float(values[1])
    if command in {b"c", b"v", b"y"}:
        return float(values[-2]), float(values[-1])
    return None


def curve_paths(page) -> list[list[tuple[float, float]]]:
    content = ContentStream(page.get_contents(), page.pdf)
    path: list[tuple[bytes, list[object]]] = []
    curves: list[list[tuple[float, float]]] = []
    for values, command in content.operations:
        if command in {b"m", b"l", b"c", b"v", b"y", b"re"}:
            path.append((command, values))
            continue
        if command not in {b"S", b"s"}:
            if command in {b"f", b"f*", b"B", b"B*", b"b", b"b*", b"n"}:
                path = []
            continue

        points = [point for item in path if (point := endpoint(*item)) is not None]
        path = []
        if len(points) < 10:
            continue
        xs = [point[0] for point in points]
        ys = [point[1] for point in points]
        if min(xs) < PLOT_LEFT - 1 or max(xs) > PLOT_RIGHT + 1:
            continue
        if min(ys) < PLOT_TOP - 1 or max(ys) > PLOT_BOTTOM + 1:
            continue
        if max(xs) - min(xs) < 10:
            continue
        curves.append(points)
    return curves


def sample_curve(points: list[tuple[float, float]]) -> list[float | None]:
    """Use the path endpoint at each five-minute x coordinate.

    The report emits a five-minute point every 542 / 288 PDF units. Cubic path
    segments retain the same endpoints, so no image interpolation is needed.
    """
    by_slot: dict[int, list[float]] = {}
    for x, y in points:
        slot = round((x - PLOT_LEFT) / (PLOT_RIGHT - PLOT_LEFT) * 288)
        if 0 <= slot < 288:
            by_slot.setdefault(slot, []).append(y)
    result: list[float | None] = []
    for slot in range(288):
        ys = by_slot.get(slot)
        if not ys:
            result.append(None)
            continue
        y = sum(ys) / len(ys)
        value = (PLOT_BOTTOM - y) / (PLOT_BOTTOM - PLOT_TOP) * PLOT_VALUE_MAX
        result.append(round(value, 2))
    return result


def page_text(page) -> str:
    return unicodedata.normalize("NFKC", page.extract_text() or "")


def report_dates(reader: PdfReader) -> list[date]:
    match = REPORT_RANGE.search(page_text(reader.pages[0]))
    if match is None:
        raise ValueError("Could not find the report monitoring date range on page 1")
    start = date(*map(int, match.group(1, 2, 3)))
    end = date(*map(int, match.group(4, 5, 6)))
    if end < start:
        raise ValueError(f"Invalid report monitoring date range: {start} - {end}")
    return [start + timedelta(days=offset) for offset in range((end - start).days + 1)]


def daily_summaries(pages, expected_dates: list[date]) -> list[tuple[date, float]]:
    summaries = []
    dates_by_month_day = {(item.month, item.day): item for item in expected_dates}
    for page in pages:
        for mean, month, day in DAILY_SUMMARY.findall(page_text(page)):
            current_date = dates_by_month_day.get((int(month), int(day)))
            if current_date is None:
                raise ValueError(
                    f"Daily chart date {month}-{day} is outside the report monitoring range"
                )
            summaries.append((current_date, float(mean)))
    summary_dates = [item[0] for item in summaries]
    if summary_dates != expected_dates:
        raise ValueError(
            "Daily chart dates do not exactly match the report monitoring range: "
            f"expected {[item.isoformat() for item in expected_dates]}, "
            f"found {[item.isoformat() for item in summary_dates]}"
        )
    return summaries


def parse(pdf: Path, timezone: ZoneInfo) -> dict[str, object]:
    reader = PdfReader(pdf)
    expected_dates = report_dates(reader)
    daily_pages = reader.pages[1:]
    summaries = daily_summaries(daily_pages, expected_dates)
    curves_by_page = [curve_paths(page) for page in daily_pages]
    curves = [curve for page_curves in curves_by_page for curve in page_curves]
    if len(curves) != len(summaries):
        details = [
            [
                (round(min(x for x, _ in curve), 1), round(max(x for x, _ in curve), 1), len(curve))
                for curve in page_curves
            ]
            for page_curves in curves_by_page
        ]
        raise ValueError(
            f"Expected {len(summaries)} daily curves from the report dates, "
            f"found {len(curves)}: {details}"
        )

    readings = []
    days = []
    for (current_date, reported_mean), curve in zip(summaries, curves, strict=True):
        values = sample_curve(curve)
        present = [value for value in values if value is not None]
        calculated_mean = round(sum(present) / len(present), 2) if present else None
        if calculated_mean is None or abs(calculated_mean - reported_mean) > 0.02:
            raise ValueError(
                f"{current_date}: reconstructed mean {calculated_mean} does not match "
                f"reported MBG {reported_mean} within 0.02 mmol/L"
            )
        days.append({
            "date": current_date.isoformat(),
            "samples": len(present),
            "meanMmolPerL": calculated_mean,
            "reportedMeanMmolPerL": reported_mean,
        })
        for slot, value in enumerate(values):
            if value is None:
                continue
            instant = datetime.combine(current_date, time.min, timezone) + timedelta(minutes=slot * SAMPLE_INTERVAL_MINUTES)
            readings.append({
                "timestamp": instant.isoformat(),
                "valueMmolPerL": value,
            })
    return {
        "format": "agp-vector-preview-v1",
        "source": str(pdf),
        "timezone": timezone.key,
        "sampleIntervalMinutes": SAMPLE_INTERVAL_MINUTES,
        "days": days,
        "readings": readings,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--timezone", default="Asia/Shanghai")
    args = parser.parse_args()
    result = parse(args.pdf, ZoneInfo(args.timezone))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(result['readings'])} readings to {args.output}")
    for day in result["days"]:
        print(f"{day['date']}: {day['samples']} samples, mean {day['meanMmolPerL']} mmol/L")


if __name__ == "__main__":
    main()
