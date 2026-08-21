#!/usr/bin/env python3
"""Extract CGM samples from vector daily-glucose curves in an AGP PDF.

This is intentionally a preview-only tool. It writes JSON and never connects to
or changes an Android device archive.
"""

from __future__ import annotations

import argparse
import json
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
FIRST_DAY_EXPECTED_SAMPLES = 35


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


def parse(pdf: Path, timezone: ZoneInfo) -> dict[str, object]:
    reader = PdfReader(pdf)
    curves: list[list[tuple[float, float]]] = []
    curves_by_page = [curve_paths(page) for page in reader.pages[1:5]]
    # A partial day can be emitted as multiple disjoint vector paths. The first
    # report day has one valid short path and one similarly shaped path from the
    # daily-summary thumbnail above it. Keep the segment whose point count is
    # consistent with the 2.92 hours reported for that day.
    first_page = curves_by_page[0]
    short_curves = [
        curve for curve in first_page
        if max(x for x, _ in curve) - min(x for x, _ in curve) < 200
    ]
    full_curves = [curve for curve in first_page if curve not in short_curves]
    if len(short_curves) == 2:
        # sample_curve returns 288 slots, so compare actual populated slots.
        first_day_curve = min(
            short_curves,
            key=lambda curve: abs(sum(value is not None for value in sample_curve(curve)) - FIRST_DAY_EXPECTED_SAMPLES),
        )
        curves_by_page[0] = [first_day_curve, *full_curves]
    curves = [curve for page_curves in curves_by_page for curve in page_curves]
    if len(curves) != 15:
        details = [
            [
                (round(min(x for x, _ in curve), 1), round(max(x for x, _ in curve), 1), len(curve))
                for curve in page_curves
            ]
            for page_curves in curves_by_page
        ]
        raise ValueError(f"Expected 15 daily curves, found {len(curves)}: {details}")

    start = date(2026, 6, 15)
    readings = []
    days = []
    for day_index, curve in enumerate(curves):
        current_date = start + timedelta(days=day_index)
        values = sample_curve(curve)
        present = [value for value in values if value is not None]
        days.append({
            "date": current_date.isoformat(),
            "samples": len(present),
            "meanMmolPerL": round(sum(present) / len(present), 2) if present else None,
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
