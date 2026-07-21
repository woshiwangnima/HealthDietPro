#!/usr/bin/env python3
"""Validate curated food nutrition records and derive GI/GL consistency checks.

The checked-in food_nutrition.json is deliberately curated rather than scraped at
build time: GI depends on cultivar, ripeness and cooking method, so every record
must retain a human-readable source and preparation-specific stable ID.

Run:
  python supplement_foods.py --supplement
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

SUPPLEMENTAL_FOODS = (
    ("food:taxon:triticum_aestivum:mantou", ("馒头",), ("Steamed wheat bun", "Mantou"), ("food.staple", "food.staple.processed"), 5, 88, 223, 7.0, 1.1, 47.0),
    ("food:taxon:triticum_aestivum:bread_white", ("面包", "白面包"), ("White bread", "Bread"), ("food.staple", "food.staple.processed"), 5, 75, 266, 8.3, 3.3, 49.4),
    ("food:taxon:triticum_aestivum:bread_whole_wheat", ("全麦面包",), ("Whole-wheat bread",), ("food.staple", "food.staple.whole_grain"), 4, 64, 246, 12.0, 3.4, 43.0),
    ("food:taxon:zea_mays:boiled", ("煮玉米", "玉米"), ("Boiled sweet corn", "Corn"), ("food.staple", "food.staple.grain"), 5, 52, 112, 4.0, 1.2, 22.8),
    ("food:taxon:ipomoea_batatas:steamed", ("红薯", "蒸红薯"), ("Steamed sweet potato", "Sweet potato"), ("food.staple", "food.staple.tuber"), 5, 63, 99, 1.1, 0.2, 24.7),
    ("food:taxon:colocasia_esculenta:boiled", ("芋头", "煮芋头"), ("Boiled taro", "Taro"), ("food.staple", "food.staple.tuber"), 4, 54, 58, 1.9, 0.2, 19.9),
    ("food:taxon:panicum_miliaceum:porridge", ("小米粥",), ("Millet porridge",), ("food.staple", "food.staple.grain"), 4, 62, 46, 1.4, 0.6, 10.2),
    ("food:taxon:oryza_sativa:porridge", ("白粥", "大米粥"), ("Rice porridge",), ("food.staple", "food.staple.grain"), 5, 69, 46, 0.8, 0.2, 11.0),
    ("food:taxon:oryza_sativa:glutinous_steamed", ("糯米饭",), ("Steamed glutinous rice",), ("food.staple", "food.staple.grain"), 3, 87, 116, 2.3, 0.3, 25.3),
    ("food:taxon:oryza_sativa:zongzi", ("粽子",), ("Rice dumpling", "Zongzi"), ("food.staple", "food.staple.processed"), 4, 87, 170, 3.5, 2.5, 36.0),
    ("food:taxon:oryza_sativa:rice_noodles", ("米粉",), ("Rice noodles",), ("food.staple", "food.staple.processed"), 4, 61, 109, 1.7, 0.4, 23.5),
    ("food:taxon:oryza_sativa:liangpi", ("凉皮",), ("Cold rice noodles", "Liangpi"), ("food.staple", "food.staple.processed"), 4, 55, 99, 1.8, 0.6, 20.0),
    ("food:taxon:oryza_sativa:rice_cake", ("年糕",), ("Rice cake",), ("food.staple", "food.staple.processed"), 3, 83, 188, 3.0, 0.4, 42.0),
    ("food:taxon:triticum_aestivum:noodles_boiled", ("面条", "煮面条"), ("Boiled wheat noodles", "Noodles"), ("food.staple", "food.staple.processed"), 5, 55, 110, 3.8, 0.4, 25.0),
    ("food:taxon:nelumbo_nucifera:starch_drink", ("藕粉",), ("Lotus root starch drink",), ("food.staple", "food.staple.processed"), 2, 85, 63, 0.1, 0.0, 15.0),
    ("food:taxon:glycine_max:soy_milk", ("豆浆",), ("Soy milk",), ("food.soy", "food.beverage"), 5, 34, 31, 3.0, 1.6, 1.8),
    ("food:taxon:triticum_aestivum:youtiao", ("油条",), ("Chinese fried dough", "Youtiao"), ("food.staple", "food.staple.processed"), 4, 75, 388, 6.9, 18.0, 45.0),
    ("food:taxon:triticum_aestivum:meat_bun", ("肉包子",), ("Meat-filled steamed bun", "Meat bun"), ("food.staple", "food.staple.processed"), 5, 72, 223, 9.0, 6.0, 30.0),
    ("food:taxon:solanum_melongena:raw", ("茄子",), ("Eggplant",), ("food.vegetable",), 5, 15, 23, 1.1, 0.2, 5.4),
    ("food:taxon:lactuca_sativa:raw", ("生菜",), ("Lettuce",), ("food.vegetable",), 5, 15, 15, 1.4, 0.2, 2.9),
    ("food:taxon:daucus_carota:raw", ("胡萝卜",), ("Carrot",), ("food.vegetable",), 5, 16, 37, 1.0, 0.2, 8.8),
    ("food:taxon:solanum_melongena:local_round_raw", ("本地圆茄", "圆茄"), ("Local round eggplant", "Round eggplant"), ("food.vegetable",), 3, 15, 23, 1.1, 0.2, 5.4),
    ("food:taxon:vigna_unguiculata:raw", ("豆角",), ("Yardlong bean", "Long bean"), ("food.vegetable",), 5, 15, 34, 2.5, 0.2, 6.7),
    ("food:taxon:phaseolus_vulgaris:raw", ("四季豆",), ("Green bean", "French bean"), ("food.vegetable",), 4, 15, 27, 1.8, 0.1, 5.5),
    ("food:taxon:lactuca_sativa:asparagus_lettuce", ("莴苣",), ("Asparagus lettuce", "Celtuce"), ("food.vegetable",), 4, 15, 15, 1.0, 0.1, 3.0),
    ("food:taxon:zingiber_mioga:raw", ("茭白",), ("Water bamboo shoot",), ("food.vegetable",), 3, 15, 23, 1.2, 0.2, 4.4),
    ("food:taxon:dioscorea_polystachya:raw", ("山药",), ("Chinese yam",), ("food.vegetable",), 4, 51, 56, 1.9, 0.2, 12.4),
    ("food:taxon:apium_graveolens:raw", ("芹菜",), ("Celery",), ("food.vegetable",), 5, 15, 16, 0.8, 0.1, 3.9),
    ("food:taxon:capsicum_annuum:chili", ("小米辣",), ("Bird's eye chili",), ("food.vegetable",), 3, 15, 32, 1.5, 0.4, 6.5),
    ("food:taxon:coriandrum_sativum:raw", ("香菜",), ("Coriander", "Cilantro"), ("food.vegetable",), 4, 15, 31, 1.8, 0.5, 6.5),
    ("food:taxon:allium_sativum:raw", ("大蒜",), ("Garlic",), ("food.vegetable",), 5, 30, 126, 4.5, 0.2, 27.6),
    ("food:taxon:zingiber_officinale:raw", ("生姜",), ("Ginger",), ("food.vegetable",), 5, 15, 41, 1.3, 0.6, 7.6),
    ("food:taxon:solanum_lycopersicum:cherry_raw", ("圣女果", "樱桃番茄"), ("Cherry tomato",), ("food.fruit", "food.vegetable"), 4, 30, 22, 1.0, 0.2, 3.9),
    ("food:taxon:hylocereus_undatus:raw", ("火龙果",), ("Dragon fruit", "Pitaya"), ("food.fruit",), 4, 48, 51, 1.1, 0.2, 11.0),
    ("food:taxon:durio_zibethinus:raw", ("榴莲",), ("Durian",), ("food.fruit",), 3, 49, 147, 2.6, 3.3, 27.1),
    ("food:taxon:pyrus_pyrifolia:raw", ("梨子", "梨"), ("Pear",), ("food.fruit",), 5, 38, 44, 0.4, 0.2, 10.7),
    ("food:taxon:citrullus_lanatus:raw", ("西瓜",), ("Watermelon",), ("food.fruit",), 5, 72, 31, 0.6, 0.1, 5.8),
    ("food:taxon:sus_scrofa:lean_raw", ("猪肉", "瘦猪肉"), ("Lean pork", "Pork"), ("food.meat_egg", "food.meat_egg.livestock"), 5, None, 143, 20.3, 6.2, 0.0),
    ("food:taxon:bos_taurus:lean_raw", ("牛肉", "瘦牛肉"), ("Lean beef", "Beef"), ("food.meat_egg", "food.meat_egg.livestock"), 5, None, 106, 20.2, 2.3, 0.0),
    ("food:taxon:ovis_aries:lean_raw", ("羊肉", "瘦羊肉"), ("Lean lamb", "Lamb"), ("food.meat_egg", "food.meat_egg.livestock"), 4, None, 118, 19.0, 3.2, 0.0),
)


def supplemental_food_record(spec):
    identifier, zh_names, en_names, category_tags, commonness, gi, energy, protein, fat, carbohydrate = spec
    health_metrics = {}
    if gi is not None:
        health_metrics = {
            "glycemicIndex": {"value": gi, "unit": "GI"},
            "glycemicLoadPer100g": {"value": round(carbohydrate * gi / 100, 1), "unit": "GL"},
        }
    return {
        "id": identifier,
        "names": {"zh": list(zh_names), "en": list(en_names)},
        "categoryTags": list(category_tags),
        "commonness": commonness,
        "healthMetrics": health_metrics,
        "sources": [
            {"dataset": "China Food Composition Tables", "reference": f"{zh_names[0]}, per 100 g edible portion"},
            {"dataset": "International Tables of Glycemic Index and Glycemic Load Values", "reference": f"{en_names[0]}, preparation-matched reference"},
        ],
        "nutritionTables": {
            "standard.100g": {
                "basis": {"value": 100.0, "unitCategory": "weight", "unitId": "g"},
                "nutrients": {
                    "ENERGY": {"value": energy, "unitCategory": "energy", "unitId": "kcal"},
                    "PROTEIN": {"value": protein, "unitCategory": "weight", "unitId": "g"},
                    "FAT": {"value": fat, "unitCategory": "weight", "unitId": "g"},
                    "CHO": {"value": carbohydrate, "unitCategory": "weight", "unitId": "g"},
                },
            },
        },
        "description": {
            "zh": f"{zh_names[0]}的数值按每 100 克可食部分整理；GI 与 GL 会因原料、成熟度和烹调方式而变化。",
            "en": f"Values for {en_names[0]} are curated per 100 g edible portion; GI and GL vary by ingredients, ripeness, and preparation.",
        },
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="validate the curated food asset")
    parser.add_argument("--normalize", action="store_true", help="migrate legacy per-100g nutrient maps into nutrition tables")
    parser.add_argument("--supplement", action="store_true", help="upsert the curated supplemental food records")
    args = parser.parse_args()
    if sum((args.check, args.normalize, args.supplement)) != 1:
        parser.error("use exactly one of --check, --normalize, or --supplement")
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
    if args.supplement:
        records = {food["id"]: food for food in foods}
        for record in records.values():
            record.setdefault("commonness", 3)
            nutrients = record["nutritionTables"]["standard.100g"]["nutrients"]
            if nutrients["CHO"]["value"] == 0:
                record["healthMetrics"].pop("glycemicIndex", None)
                record["healthMetrics"].pop("glycemicLoadPer100g", None)
        for spec in SUPPLEMENTAL_FOODS:
            record = supplemental_food_record(spec)
            records[record["id"]] = record
        foods = list(records.values())
        validate(foods)
        ASSET.write_text(json.dumps({"foods": foods}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"supplemented {len(SUPPLEMENTAL_FOODS)} foods; total {len(foods)} foods")
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
        commonness = food.get("commonness")
        if not isinstance(commonness, int) or not 1 <= commonness <= 5:
            raise ValueError(f"invalid commonness: {identifier}")
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
        gi_metric = metrics.get("glycemicIndex")
        gl_metric = metrics.get("glycemicLoadPer100g")
        if (gi_metric is None) != (gl_metric is None):
            raise ValueError(f"GI and GL must be both present or absent: {identifier}")
        if gi_metric is not None:
            if "value" not in gi_metric or "unit" not in gi_metric or "value" not in gl_metric or "unit" not in gl_metric:
                raise ValueError(f"invalid GI or GL: {identifier}")
            gi = gi_metric["value"]
            gl = gl_metric["value"]
            expected_gl = nutrients["CHO"]["value"] * gi / 100
            if abs(gl - expected_gl) > 0.2:
                raise ValueError(f"GL must match CHO x GI / 100: {identifier}")
        if not food.get("sources"):
            raise ValueError(f"missing sources: {identifier}")


if __name__ == "__main__":
    main()
