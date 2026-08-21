#!/usr/bin/env python3
"""Render parsed AGP preview JSON as one daily-chart image."""

from __future__ import annotations

import argparse
import json
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

WIDTH = 1_800
HEIGHT = 1_600
MARGIN_X = 70
MARGIN_Y = 70
COLUMN_GAP = 30
ROW_GAP = 45
PLOT_WIDTH = (WIDTH - MARGIN_X * 2 - COLUMN_GAP * 2) // 3
PLOT_HEIGHT = (HEIGHT - MARGIN_Y * 2 - ROW_GAP * 4) // 5


def font(size: int):
    return ImageFont.truetype("arial.ttf", size)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("preview", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    preview = json.loads(args.preview.read_text(encoding="utf-8"))
    values_by_day: dict[str, list[tuple[float, float]]] = defaultdict(list)
    for reading in preview["readings"]:
        instant = datetime.fromisoformat(reading["timestamp"])
        hour = instant.hour + instant.minute / 60
        values_by_day[instant.date().isoformat()].append((hour, reading["valueMmolPerL"]))

    days = preview["days"]
    image = Image.new("RGB", (WIDTH, HEIGHT), "white")
    draw = ImageDraw.Draw(image)
    for index, day in enumerate(days):
        row, column = divmod(index, 3)
        left = MARGIN_X + column * (PLOT_WIDTH + COLUMN_GAP)
        top = MARGIN_Y + row * (PLOT_HEIGHT + ROW_GAP)
        right = left + PLOT_WIDTH
        bottom = top + PLOT_HEIGHT
        points = sorted(values_by_day[day["date"]])
        y = lambda value: bottom - value / 25 * PLOT_HEIGHT
        x = lambda hour: left + hour / 24 * PLOT_WIDTH
        draw.rectangle((left, y(10.0), right, y(3.9)), fill="#E8F5E9")
        for value in range(0, 26, 5):
            draw.line((left, y(value), right, y(value)), fill="#DDDDDD")
            draw.text((left - 28, y(value) - 7), str(value), fill="#555555", font=font(12))
        for hour in range(0, 25, 4):
            draw.line((x(hour), top, x(hour), bottom), fill="#F0F0F0")
            draw.text((x(hour) - 10, bottom + 4), f"{hour:02}", fill="#555555", font=font(12))
        draw.line((left, y(10.0), right, y(10.0)), fill="#F57C00", width=1)
        draw.line((left, y(13.9), right, y(13.9)), fill="#E53935", width=1)
        if points:
            draw.line([(x(hour), y(value)) for hour, value in points], fill="#4E93BF", width=2)
            for hour, value in points:
                draw.ellipse((x(hour) - 1, y(value) - 1, x(hour) + 1, y(value) + 1), fill="#4E93BF")
        draw.rectangle((left, top, right, bottom), outline="#888888")
        draw.text(
            (left, top - 21),
            f"{day['date']} | {day['samples']} samples | MBG {day['meanMmolPerL']}",
            fill="#222222",
            font=font(14),
        )
    draw.text((MARGIN_X, 18), "AGP daily glucose curves reconstructed from PDF vector paths", fill="#111111", font=font(24))
    draw.text((WIDTH - 365, 23), "Local time: Asia/Shanghai | mmol/L", fill="#444444", font=font(14))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    image.save(args.output)
    print(f"Wrote chart to {args.output}")


if __name__ == "__main__":
    main()
