#!/usr/bin/env python3
"""Compile the human-maintained food JSON into sharded Android assets."""

import argparse
import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/assets/food_nutrition.json"
SOURCE_DIR = ROOT / "tools/food_catalog/source"
SOURCE_FILES = {
    "ingredient": SOURCE_DIR / "ingredients.json",
    "food": SOURCE_DIR / "foods.json",
    "dish": SOURCE_DIR / "dishes.json",
}
NUTRIENTS = ROOT / "app/src/main/assets/DRIs/nutrients_meta.json"
OUTPUT = ROOT / "app/src/main/assets/food_catalog"
IMAGE_SOURCE = ROOT / "tools/food_catalog/images/source"
IMAGE_OUTPUT = OUTPUT / "images"


def safe_id(identifier):
    return identifier.replace(":", "_")


def normalize_tags(tags):
    unique = list(dict.fromkeys(tags))
    return [tag for tag in unique if not any(other != tag and other.startswith(tag + ".") for other in unique)]


def read_source(path=SOURCE):
    if path == SOURCE and all(source_path.is_file() for source_path in SOURCE_FILES.values()):
        foods = []
        for source_path in SOURCE_FILES.values():
            value = json.loads(source_path.read_text(encoding="utf-8"))
            foods.extend(value.get("foods", []) if isinstance(value, dict) else value)
        return {"foods": foods}
    return json.loads(path.read_text(encoding="utf-8"))


def write_source(asset):
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    groups = {kind: [] for kind in SOURCE_FILES}
    for food in asset.get("foods", []):
        groups.setdefault(food.get("kind", "ingredient"), []).append(food)
    for kind, path in SOURCE_FILES.items():
        path.write_text(json.dumps({"foods": groups[kind]}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def source_bytes():
    paths = list(SOURCE_FILES.values()) if all(path.is_file() for path in SOURCE_FILES.values()) else [SOURCE]
    return b"".join(path.read_bytes() for path in paths)


def nutrient_codes():
    return {item["code"] for item in json.loads(NUTRIENTS.read_text(encoding="utf-8"))}


def validate(asset):
    foods = asset.get("foods", [])
    by_id = {}
    allowed = nutrient_codes()
    for food in foods:
        identifier = food.get("id", "")
        if not identifier or identifier in by_id:
            raise ValueError(f"invalid or duplicate food id: {identifier}")
        by_id[identifier] = food
        if food.get("kind", "ingredient") not in {"ingredient", "food", "dish"}:
            raise ValueError(f"invalid food kind: {identifier}")
        if safe_id(identifier) != identifier.replace(":", "_"):
            raise ValueError(f"unable to create safe filename: {identifier}")
        for table in food.get("nutritionTables", {}).values():
            unknown = set(table.get("nutrients", {})) - allowed
            if unknown:
                raise ValueError(f"unknown nutrients in {identifier}: {sorted(unknown)}")
        unknown = set(food.get("nutrients", {})) - allowed
        if unknown:
            raise ValueError(f"unknown legacy nutrients in {identifier}: {sorted(unknown)}")
        derived = food.get("derivedFrom") or {}
        unknown = set(derived.get("nutrientOverrides", {})) - allowed
        if unknown:
            raise ValueError(f"unknown nutrient overrides in {identifier}: {sorted(unknown)}")
        for component in food.get("components", []):
            if component.get("foodId") not in by_id:
                # Forward references are checked after all records are indexed.
                continue
        derived = food.get("derivedFrom")
        if derived and derived.get("ingredientId") not in by_id:
            continue
    for food in foods:
        identifier = food["id"]
        kind = food.get("kind", "ingredient")
        if kind == "ingredient" and not food.get("nutritionTables"):
            raise ValueError(f"ingredient has no nutrition tables: {identifier}")
        if kind == "dish" and food.get("categoryTags"):
            raise ValueError(f"dish must not have category tags: {identifier}")
        if kind == "food" and food.get("nutritionTables"):
            raise ValueError(f"prepared food must not store nutrition tables: {identifier}")
        tags = food.get("categoryTags", [])
        if normalize_tags(tags) != tags:
            raise ValueError(f"category tags must contain leaf tags only: {identifier}")
        for component in food.get("components", []):
            if component["foodId"] not in by_id:
                raise ValueError(f"missing component {component['foodId']} in {identifier}")
        derived = food.get("derivedFrom")
        if derived and derived["ingredientId"] not in by_id:
            raise ValueError(f"missing ingredient {derived['ingredientId']} in {identifier}")
    graph = {food["id"]: [x["foodId"] for x in food.get("components", [])] for food in foods}
    visiting, visited = set(), set()

    def visit(identifier):
        if identifier in visiting:
            raise ValueError(f"food component cycle at {identifier}")
        if identifier in visited:
            return
        visiting.add(identifier)
        for child in graph.get(identifier, []):
            visit(child)
        visiting.remove(identifier)
        visited.add(identifier)

    for identifier in graph:
        visit(identifier)
    filenames = {}
    for food in foods:
        filename = safe_id(food["id"]) + ".json"
        if filename in filenames and filenames[filename] != food["id"]:
            raise ValueError(f"safe filename collision: {filename}")
        filenames[filename] = food["id"]
    return foods


def indexes(foods):
    by_id = {food["id"]: record_path(food) for food in foods}
    search = {}
    categories = {}
    related = {}
    for food in foods:
        identifier = food["id"]
        for token in (identifier, safe_id(identifier)):
            search.setdefault(token.lower(), []).append(identifier)
        for names in food.get("names", {}).values():
            for name in names:
                token = "".join(name.lower().split())
                if token:
                    search.setdefault(token, []).append(identifier)
        for tag in food.get("categoryTags", []):
            categories.setdefault(tag, []).append(identifier)
        for component in food.get("components", []):
            related.setdefault(component["foodId"], []).append(identifier)
    return by_id, search, categories, related


def record_path(food):
    kind = food.get("kind", "ingredient")
    folder = {"ingredient": "ingredients", "food": "foods", "dish": "dishes"}[kind]
    return f"records/{folder}/{safe_id(food['id'])}.json"


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def compile_catalog(asset):
    foods = validate(asset)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for directory in (OUTPUT / "records", OUTPUT / "indexes"):
        if directory.exists():
            shutil.rmtree(directory)
    manifest_path = OUTPUT / "manifest.json"
    if manifest_path.exists():
        manifest_path.unlink()
    by_id, search, categories, related = indexes(foods)
    for food in foods:
        write_json(OUTPUT / record_path(food), food)
    for name, value in (("by_id", by_id), ("search", search), ("categories", categories), ("related_dishes", related)):
        write_json(OUTPUT / "indexes" / f"{name}.json", value)
    image_manifest = OUTPUT / "images" / "manifest.json"
    if not image_manifest.is_file():
        write_json(image_manifest, {"schemaVersion": 1, "images": {}})
    source_data = source_bytes()
    manifest = {
        "schemaVersion": 1,
        "catalogVersion": 1,
        "nutrientMetaSha256": hashlib.sha256(NUTRIENTS.read_bytes()).hexdigest(),
        "sourceSha256": hashlib.sha256(source_data).hexdigest(),
        "recordCount": {kind: sum(food.get("kind", "ingredient") == kind for food in foods) for kind in ("ingredient", "food", "dish")},
        "records": by_id,
        "indexes": {name: f"indexes/{name}.json" for name in ("by_id", "search", "categories", "related_dishes")},
    }
    write_json(OUTPUT / "manifest.json", manifest)
    return manifest


def normalize_source():
    asset = read_source()
    changed = 0
    for food in asset.get("foods", []):
        tags = food.get("categoryTags")
        if tags is not None:
            normalized = normalize_tags(tags)
            if normalized != tags:
                food["categoryTags"] = normalized
                changed += 1
    if all(path.is_file() for path in SOURCE_FILES.values()):
        write_source(asset)
    else:
        SOURCE.write_text(json.dumps(asset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"normalized category tags in {changed} foods")


def split_source():
    asset = json.loads(SOURCE.read_text(encoding="utf-8"))
    validate(asset)
    write_source(asset)
    print(f"split {len(asset.get('foods', []))} foods into {len(SOURCE_FILES)} source files")


def export_catalog():
    manifest = json.loads((OUTPUT / "manifest.json").read_text(encoding="utf-8"))
    foods = [json.loads((OUTPUT / path).read_text(encoding="utf-8")) for path in manifest["records"].values()]
    return {"foods": foods}


def build_images():
    """Normalize mapped source images when Pillow is available.

    The mapping is deliberately explicit because source filenames are not a
    stable identity. It is optional until food image assets are introduced.
    """
    mapping_path = IMAGE_SOURCE / "mapping.json"
    if not mapping_path.is_file():
        print("no image mapping found; nothing to build")
        return
    try:
        from PIL import Image
    except ImportError as error:
        raise SystemExit("build-images requires Pillow in the uv environment") from error
    mapping = json.loads(mapping_path.read_text(encoding="utf-8"))
    if len(mapping) != len(set(mapping.values())):
        raise ValueError("each food must have at most one mapped source image")
    valid_ids = {food["id"] for food in read_source().get("foods", [])}
    for filename, identifier in mapping.items():
        if identifier not in valid_ids:
            raise ValueError(f"image references unknown food: {identifier}")
        source = IMAGE_SOURCE / filename
        if not source.is_file():
            raise ValueError(f"missing source image: {source}")
        safe = safe_id(identifier)
        with Image.open(source) as image:
            image = image.convert("RGB")
            for variant, max_size, quality in (("thumb", 160, 82), ("detail", 640, 86)):
                target = IMAGE_OUTPUT / variant / f"{safe}.webp"
                target.parent.mkdir(parents=True, exist_ok=True)
                copy = image.copy()
                copy.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)
                copy.save(target, "WEBP", quality=quality, method=6)
    manifest = {
        "schemaVersion": 1,
        "images": {
            identifier: {
                "thumb": f"images/thumb/{safe_id(identifier)}.webp",
                "detail": f"images/detail/{safe_id(identifier)}.webp",
            }
            for identifier in mapping.values()
        },
    }
    write_json(IMAGE_OUTPUT / "manifest.json", manifest)
    print(f"built {len(mapping)} image variants")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("validate", "normalize", "split", "compile", "export", "inspect", "verify-roundtrip", "build-images"))
    args = parser.parse_args()
    if args.command == "normalize":
        normalize_source()
    elif args.command == "split":
        split_source()
    elif args.command == "validate":
        foods = validate(read_source())
        print(f"validated {len(foods)} foods")
    elif args.command == "compile":
        manifest = compile_catalog(read_source())
        print(json.dumps(manifest["recordCount"], ensure_ascii=False))
    elif args.command == "export":
        print(json.dumps(export_catalog(), ensure_ascii=False, indent=2))
    elif args.command == "inspect":
        source = sum(path.stat().st_size for path in SOURCE_FILES.values()) if all(path.is_file() for path in SOURCE_FILES.values()) else SOURCE.stat().st_size
        generated = sum(path.stat().st_size for path in OUTPUT.rglob("*.json")) if OUTPUT.exists() else 0
        foods = validate(read_source())
        print(json.dumps({"records": len(foods), "sourceBytes": source, "generatedBytes": generated}, ensure_ascii=False))
    elif args.command == "verify-roundtrip":
        original = read_source()
        rebuilt = export_catalog()
        if original != rebuilt:
            raise SystemExit("round-trip mismatch")
        print(f"round-trip verified {len(rebuilt['foods'])} foods")
    else:
        build_images()


if __name__ == "__main__":
    main()
