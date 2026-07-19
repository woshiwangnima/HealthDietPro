#!/usr/bin/env python3
"""Generate province boundaries and city/district centroids for reverse lookup.

Data comes from DataV's public Chinese administrative-boundary GeoJSON API. The
generated files retain the Android asset schemas consumed by ProvinceRepository
and RegionRepository:

  provinces.json: 34 province-level codes, centroid, and boundary polygons
  regions.json: city and district/county centroids keyed by administrative code

Run from any directory:
  python supplement_regions.py --refresh
  python supplement_regions.py --check

``--refresh`` downloads the source hierarchy and writes both JSON files.
``--check`` validates the checked-in files without accessing the network.
"""

import argparse
import json
import time
from collections import defaultdict
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parent
REGIONS_PATH = ROOT / "regions.json"
PROVINCES_PATH = ROOT / "provinces.json"
BASE_URL = "https://geo.datav.aliyun.com/areas_v3/bound"
PROVINCE_CODES = {
    "11", "12", "13", "14", "15", "21", "22", "23", "31", "32", "33",
    "34", "35", "36", "37", "41", "42", "43", "44", "45", "46", "50",
    "51", "52", "53", "54", "61", "62", "63", "64", "65", "71", "81", "82",
}
DIRECT_PROVINCES = {"11", "12", "31", "50", "71", "81", "82"}


def load_json(path: Path):
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def write_json(path: Path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def request_json(url: str, allow_missing: bool = False):
    request = Request(url, headers={"User-Agent": "HealthDietPro location generator/1.0"})
    last_error = None
    for attempt in range(3):
        try:
            with urlopen(request, timeout=30) as response:
                return json.load(response)
        except HTTPError as error:
            if allow_missing and error.code == 404:
                return None
            last_error = error
            time.sleep(attempt + 1)
        except (URLError, TimeoutError, json.JSONDecodeError) as error:
            last_error = error
            time.sleep(attempt + 1)
    raise RuntimeError(f"could not download {url}: {last_error}")


def fetch_boundary(adcode: str, full: bool = False, allow_missing: bool = False):
    suffix = "_full" if full else ""
    return request_json(f"{BASE_URL}/{adcode}{suffix}.json", allow_missing)


def properties(feature):
    return feature["properties"]


def code_for(properties_value):
    return str(properties_value["adcode"])


def compact_code(adcode: str, digits: int):
    return adcode.zfill(6)[:digits]


def feature_center(feature):
    point = properties(feature).get("centroid") or properties(feature).get("center")
    if not isinstance(point, list) or len(point) != 2:
        raise ValueError(f"missing centroid for {properties(feature)!r}")
    return [round(float(point[0]), 6), round(float(point[1]), 6)]


def province_polygons(feature):
    geometry = feature.get("geometry") or {}
    geometry_type = geometry.get("type")
    coordinates = geometry.get("coordinates")
    if geometry_type == "Polygon":
        polygons = [coordinates]
    elif geometry_type == "MultiPolygon":
        polygons = coordinates
    else:
        raise ValueError(f"unsupported province geometry: {geometry_type}")

    # ProvinceRepository treats each item as a filled ring. Keep outer rings only;
    # holes are lakes/enclaves and must not become additional filled polygons.
    rings = []
    for polygon in polygons:
        if not polygon:
            continue
        outer = polygon[0]
        if len(outer) < 4:
            continue
        rings.append([[round(float(lng), 6), round(float(lat), 6)] for lng, lat in outer])
    if not rings:
        raise ValueError(f"no outer polygon rings for {properties(feature)!r}")
    return rings


def validate(provinces, regions):
    province_codes = {item["code"] for item in provinces}
    if province_codes != PROVINCE_CODES or len(provinces) != 34:
        raise ValueError("provinces.json must contain exactly the 34 supported province codes")

    city_codes = set()
    region_codes = set()
    for province in provinces:
        if set(province) != {"code", "name", "polygons", "lng", "lat"}:
            raise ValueError(f"invalid province fields: {province!r}")
        if not province["polygons"]:
            raise ValueError(f"province has no polygons: {province['code']}")
        validate_coordinate(province["code"], province["lng"], province["lat"])

    for region in regions:
        if set(region) != {"level", "code", "name", "parentCode", "lng", "lat"}:
            raise ValueError(f"invalid region fields: {region!r}")
        if region["level"] not in {"city", "district"}:
            raise ValueError(f"invalid level: {region['level']}")
        if region["code"] in region_codes:
            raise ValueError(f"duplicate region code: {region['code']}")
        region_codes.add(region["code"])
        validate_coordinate(region["code"], region["lng"], region["lat"])
        if region["level"] == "city":
            if region["parentCode"] not in province_codes:
                raise ValueError(f"unknown province parent for {region['code']}")
            city_codes.add(region["code"])

    for region in regions:
        if region["level"] == "district" and region["parentCode"] not in city_codes:
            raise ValueError(f"unknown city parent for {region['code']}")


def validate_coordinate(code, lng, lat):
    if not -180 <= lng <= 180 or not -90 <= lat <= 90:
        raise ValueError(f"invalid coordinate for {code}")


def generate():
    national = fetch_boundary("100000", full=True)
    province_features = {
        compact_code(code_for(properties(feature)), 2): feature
        for feature in national["features"]
        if compact_code(code_for(properties(feature)), 2) in PROVINCE_CODES
    }
    if set(province_features) != PROVINCE_CODES:
        missing = sorted(PROVINCE_CODES - set(province_features))
        raise RuntimeError(f"national source is missing provinces: {missing}")

    provinces = []
    cities = {}
    districts = {}
    for province_code in sorted(PROVINCE_CODES):
        province_feature = province_features[province_code]
        province_properties = properties(province_feature)
        lng, lat = feature_center(province_feature)
        provinces.append({
            "code": province_code,
            "name": province_properties["name"],
            "polygons": province_polygons(province_feature),
            "lng": lng,
            "lat": lat,
        })

        province_adcode = code_for(province_properties)
        province_full = fetch_boundary(province_adcode, full=True, allow_missing=True)
        child_features = province_full["features"] if province_full is not None else []
        city_features = [feature for feature in child_features if properties(feature).get("level") == "city"]

        if province_code in DIRECT_PROVINCES:
            # Municipalities and special administrative regions expose districts
            # directly. A synthetic city preserves RegionRepository's two-level
            # city -> district index without inventing a geographic centroid.
            city_code = compact_code(province_adcode, 4)
            cities[city_code] = {
                "level": "city", "code": city_code, "name": province_properties["name"],
                "parentCode": province_code, "lng": lng, "lat": lat,
            }
            for district_feature in child_features:
                district_properties = properties(district_feature)
                if district_properties.get("level") != "district":
                    continue
                district_code = code_for(district_properties)
                dlng, dlat = feature_center(district_feature)
                districts[district_code] = {
                    "level": "district", "code": district_code, "name": district_properties["name"],
                    "parentCode": city_code, "lng": dlng, "lat": dlat,
                }
            continue

        if province_full is None:
            city_code = compact_code(province_adcode, 4)
            cities[city_code] = {
                "level": "city", "code": city_code, "name": province_properties["name"],
                "parentCode": province_code, "lng": lng, "lat": lat,
            }
            continue

        for city_feature in city_features:
            city_properties = properties(city_feature)
            city_adcode = code_for(city_properties)
            city_code = compact_code(city_adcode, 4)
            city_lng, city_lat = feature_center(city_feature)
            cities[city_code] = {
                "level": "city", "code": city_code, "name": city_properties["name"],
                "parentCode": province_code, "lng": city_lng, "lat": city_lat,
            }
            if city_properties.get("childrenNum", 0) == 0:
                continue
            city_full = fetch_boundary(city_adcode, full=True, allow_missing=True)
            if city_full is None:
                continue
            for district_feature in city_full["features"]:
                district_properties = properties(district_feature)
                if district_properties.get("level") != "district":
                    continue
                district_code = code_for(district_properties)
                district_lng, district_lat = feature_center(district_feature)
                districts[district_code] = {
                    "level": "district", "code": district_code, "name": district_properties["name"],
                    "parentCode": city_code, "lng": district_lng, "lat": district_lat,
                }

    # Hainan's province-level direct counties have no prefecture-city feature.
    # Group them under their four-digit administrative prefix so every district
    # remains reachable through RegionRepository.districtsOf(cityCode).
    for province_code in sorted(PROVINCE_CODES - DIRECT_PROVINCES):
        province_adcode = province_code + "0000"
        province_full = fetch_boundary(province_adcode, full=True, allow_missing=True)
        if province_full is None:
            continue
        for feature in province_full["features"]:
            item = properties(feature)
            if item.get("level") != "district":
                continue
            district_code = code_for(item)
            city_code = compact_code(district_code, 4)
            if city_code not in cities:
                lng, lat = feature_center(feature)
                cities[city_code] = {
                    "level": "city", "code": city_code, "name": item["name"],
                    "parentCode": province_code, "lng": lng, "lat": lat,
                }
            lng, lat = feature_center(feature)
            districts[district_code] = {
                "level": "district", "code": district_code, "name": item["name"],
                "parentCode": city_code, "lng": lng, "lat": lat,
            }

    regions = sorted([*cities.values(), *districts.values()], key=lambda item: (item["level"], item["code"]))
    provinces.sort(key=lambda item: item["code"])
    validate(provinces, regions)
    return provinces, regions


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--refresh", action="store_true", help="download and regenerate both location assets")
    parser.add_argument("--check", action="store_true", help="validate existing location assets")
    args = parser.parse_args()
    if args.refresh == args.check:
        parser.error("choose exactly one of --refresh or --check")
    if args.check:
        validate(load_json(PROVINCES_PATH), load_json(REGIONS_PATH))
        return
    provinces, regions = generate()
    write_json(PROVINCES_PATH, provinces)
    write_json(REGIONS_PATH, regions)


if __name__ == "__main__":
    main()
