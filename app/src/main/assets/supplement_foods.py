#!/usr/bin/env python3
"""Validate curated food nutrition records and derive GI/GL consistency checks.

The checked-in food_nutrition.json is deliberately curated rather than scraped at
build time: GI depends on cultivar, ripeness and cooking method, so every record
must retain a human-readable source and preparation-specific stable ID.

Run:
  python supplement_foods.py --check

For a newly researched food, add a full record to food_nutrition.json, including
the source references. This script rejects ambiguous IDs, bare nutrient numbers,
missing aliases, missing category tags, and missing GI/GL provenance.
"""

import argparse
import json
import re
from pathlib import Path


ASSET = Path(__file__).resolve().parent / "food_nutrition.json"
ID_PATTERN = re.compile(r"^food:taxon:[a-z0-9_]+:[a-z0-9_]+(?::[a-z0-9_]+)?$")
REQUIRED_NUTRIENTS = {"ENERGY", "PROTEIN", "FAT", "CHO"}
DESCRIPTIONS = {
    "food:taxon:oryza_sativa:polished:steamed": ("蒸熟的精白米主食，主要提供碳水化合物；实际升糖反应受品种、米饭软硬和同餐食物影响。", "Cooked polished rice is a carbohydrate-rich staple; glycemic response varies with variety, texture, and the accompanying meal."),
    "food:taxon:oryza_sativa:wholegrain:steamed": ("保留麸皮和胚芽的熟糙米，膳食纤维通常高于精白米；烹调软硬会影响消化速度。", "Cooked brown rice retains bran and germ and typically provides more dietary fiber than polished rice; cooking texture affects digestion."),
    "food:taxon:avena_sativa:rolled:dry": ("压片燕麦属于全谷物，可提供可溶性膳食纤维；加糖、煮制时间和冲泡方式会改变实际能量和升糖反应。", "Rolled oats are a whole grain and provide soluble fiber; added sugar and preparation affect actual energy and glycemic response."),
    "food:taxon:solanum_tuberosum:boiled": ("煮马铃薯是富含淀粉的薯类食物；品种、熟化程度及冷却后再食用都会影响血糖反应。", "Boiled potato is a starchy tuber; variety, doneness, and cooling before eating affect glycemic response."),
    "food:taxon:cucumis_sativus:commercial:raw": ("生黄瓜含水量高、能量密度低，适合作为蔬菜搭配；应注意清洗和可食部分。", "Raw cucumber has high water content and low energy density; use it as part of a varied vegetable intake and wash before eating."),
    "food:taxon:cucumis_sativus:landrace:raw": ("露地或地方品系黄瓜的外观和风味可能不同；营养值采用同类生黄瓜数据，品种间会有自然差异。", "Field or landrace cucumber may differ in appearance and flavor; values use raw cucumber data because cultivar composition naturally varies."),
    "food:taxon:solanum_lycopersicum:raw": ("生番茄可同时作为蔬菜或水果食用，含有类胡萝卜素等植物化学物；成熟度和加工方式会影响成分利用。", "Raw tomato may be eaten as a vegetable or fruit and contains carotenoids; ripeness and processing affect nutrient availability."),
    "food:taxon:brassica_oleracea:broccoli:raw": ("西兰花属于十字花科蔬菜，含维生素和多种植物化学物；烹调时间会影响部分热敏营养素。", "Broccoli is a cruciferous vegetable with vitamins and phytochemicals; cooking duration affects some heat-sensitive nutrients."),
    "food:taxon:malus_domestica:raw": ("带皮鲜苹果提供水分和膳食纤维；大小、品种和可食比例不同，单个重量仅为估算。", "Raw apple with peel provides water and dietary fiber; fruit size, cultivar, and edible yield vary, so per-fruit portions are estimates."),
    "food:taxon:musa_acuminata:ripe:raw": ("成熟香蕉提供碳水化合物和钾；成熟度越高，糖和淀粉的比例及升糖反应可能变化。", "Ripe banana provides carbohydrate and potassium; ripeness changes the balance of sugars and starch and may alter glycemic response."),
    "food:taxon:glycine_max:tofu:firm": ("北豆腐以大豆为原料，是植物蛋白来源；凝固剂和含水量会使不同产品的营养值不同。", "Firm tofu is a soy-based source of plant protein; coagulant choice and water content cause product-to-product variation."),
    "food:taxon:gallus_gallus:egg:whole": ("全鸡蛋提供优质蛋白质和脂溶性营养素；烹调用油和调味会显著改变实际摄入。", "Whole egg provides high-quality protein and fat-soluble nutrients; cooking oil and seasoning substantially change intake."),
    "food:taxon:gallus_gallus:breast:steamed": ("去皮熟鸡胸肉是高蛋白、低碳水的禽肉选择；数据不包含额外烹调用油或酱料。", "Cooked skinless chicken breast is a high-protein, low-carbohydrate poultry option; values exclude added oil and sauces."),
    "food:taxon:salmo_salar:raw": ("大西洋鲑是脂肪鱼，含蛋白质和不饱和脂肪；生食应符合食品安全和冷链要求。", "Atlantic salmon is an oily fish with protein and unsaturated fat; raw consumption requires appropriate food-safety and cold-chain controls."),
    "food:taxon:bos_taurus:milk:whole": ("全脂牛奶提供蛋白质、脂肪和乳糖；包装规格不同，杯和盒的体积仅作为常见份量估算。", "Whole milk provides protein, fat, and lactose; cup and carton sizes are common serving estimates and vary by package."),
    "food:taxon:arachis_hypogaea:roasted": ("烤花生能量密度较高，含脂肪、蛋白质和膳食纤维；食用时应控制份量并留意过敏风险。", "Roasted peanuts are energy-dense and provide fat, protein, and fiber; manage portions and consider allergy risk."),
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="validate the curated food asset")
    parser.add_argument("--normalize", action="store_true", help="migrate legacy per-100g nutrient maps into nutrition tables")
    args = parser.parse_args()
    if args.check == args.normalize:
        parser.error("use exactly one of --check or --normalize")
    with ASSET.open(encoding="utf-8") as source:
        foods = json.load(source)["foods"]
    if args.normalize:
        for food in foods:
            legacy_nutrients = food.pop("nutrients", None)
            if legacy_nutrients is not None:
                food["nutritionTables"] = {
                    "standard.100g": {
                        "basis": {"value": 100.0, "unitCategory": "weight", "unitId": "g"},
                        "nutrients": legacy_nutrients,
                    },
                }
            density = food.get("densityGramsPerMl")
            if food["id"] in DESCRIPTIONS:
                zh, en = DESCRIPTIONS[food["id"]]
                food["description"] = {"zh": zh, "en": en}
            tables = food.get("nutritionTables", {})
            if density and "standard.100g" in tables and "standard.100ml" not in tables:
                mass_table = tables["standard.100g"]
                tables["standard.100ml"] = {
                    "basis": {"value": 100.0, "unitCategory": "volume", "unitId": "ml"},
                    "nutrients": {
                        code: {**amount, "value": amount["value"] * density}
                        for code, amount in mass_table["nutrients"].items()
                    },
                }
                for serving in food.get("servings", []):
                    if serving.get("nutritionTableKey") == "standard.100g":
                        serving["nutritionTableKey"] = "standard.100ml"
                        serving["ratioToTable"] = serving["ratioToTable"] / density
            for serving in food.get("servings", []):
                mass = serving.pop("edibleMassGrams", None)
                if mass is not None:
                    serving["nutritionTableKey"] = "standard.100ml" if density else "standard.100g"
                    serving["ratioToTable"] = mass / (density * 100.0) if density else mass / 100.0
        ASSET.write_text(json.dumps({"foods": foods}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return
    validate(foods)
    print(f"validated {len(foods)} foods")


def validate(foods):
    ids = set()
    for food in foods:
        identifier = food["id"]
        if not ID_PATTERN.fullmatch(identifier):
            raise ValueError(f"unstable food ID: {identifier}")
        if identifier in ids:
            raise ValueError(f"duplicate food ID: {identifier}")
        ids.add(identifier)
        if not food["categoryTags"] or any(not tag.startswith("food.") for tag in food["categoryTags"]):
            raise ValueError(f"invalid category tags: {identifier}")
        for language, names in food["names"].items():
            if not language or not isinstance(names, list) or not names or any(not name.strip() for name in names):
                raise ValueError(f"invalid names: {identifier}")
        if not food.get("description", {}).get("zh") or not food.get("description", {}).get("en"):
            raise ValueError(f"missing localized description: {identifier}")
        tables = food.get("nutritionTables", {})
        if not tables:
            raise ValueError(f"missing nutrition tables: {identifier}")
        for table_key, table in tables.items():
            if set(table) != {"basis", "nutrients"}:
                raise ValueError(f"invalid nutrition table: {identifier}/{table_key}")
            basis = table["basis"]
            if set(basis) != {"value", "unitCategory", "unitId"} or basis["value"] <= 0:
                raise ValueError(f"invalid nutrition table basis: {identifier}/{table_key}")
        nutrients = tables["standard.100g"]["nutrients"]
        if not REQUIRED_NUTRIENTS <= nutrients.keys():
            raise ValueError(f"missing core nutrients: {identifier}")
        for code, amount in nutrients.items():
            if set(amount) != {"value", "unitCategory", "unitId"} or amount["value"] < 0:
                raise ValueError(f"invalid nutrient {code}: {identifier}")
        for serving in food.get("servings", []):
            if set(serving) != {"id", "nutritionTableKey", "ratioToTable", "labels"} or serving["ratioToTable"] <= 0 or not serving["labels"] or serving["nutritionTableKey"] not in tables:
                raise ValueError(f"invalid serving: {identifier}")
        metrics = food["healthMetrics"]
        for metric_name in ("glycemicIndex", "glycemicLoadPer100g", "inflammatoryPotential"):
            metric = metrics.get(metric_name)
            if metric is None or "value" not in metric or "unit" not in metric:
                raise ValueError(f"missing {metric_name}: {identifier}")
        gi = metrics["glycemicIndex"]["value"]
        gl = metrics["glycemicLoadPer100g"]["value"]
        expected_gl = nutrients["CHO"]["value"] * gi / 100
        if abs(gl - expected_gl) > 0.2:
            raise ValueError(f"GL must match CHO x GI / 100: {identifier}")
        if not food.get("sources"):
            raise ValueError(f"missing sources: {identifier}")


if __name__ == "__main__":
    main()
