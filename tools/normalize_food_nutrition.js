const fs = require("fs");

const assetPath = "app/src/main/assets/food_nutrition.json";
const asset = JSON.parse(fs.readFileSync(assetPath, "utf8"));
const foods = asset.foods;
const byId = new Map(foods.map((food) => [food.id, food]));

const amount = (value, unitCategory, unitId) => ({ value, unitCategory, unitId });
const quantity = (value) => amount(value, "weight", "g");
const table = (energy, protein, fat, cho, fiber) => ({
  "standard.100g_edible": {
    basis: quantity(100),
    nutrients: {
      ENERGY: amount(energy, "energy", "kcal"),
      PROTEIN: amount(protein, "weight", "g"),
      FAT: amount(fat, "weight", "g"),
      CHO: amount(cho, "weight", "g"),
      ...(fiber === undefined ? {} : { FIBER: amount(fiber, "weight", "g") }),
    },
  },
});

function addIngredient(id, zh, en, categoryTags, nutrition, healthMetrics = {}) {
  if (byId.has(id)) return;
  const food = {
    id,
    kind: "ingredient",
    names: { zh: [zh], en: [en] },
    categoryTags,
    systemTags: ["common"],
    edibleRatio: 1,
    nutritionTables: nutrition,
    healthMetrics,
    sources: [{ dataset: "China Food Composition Tables", reference: `${en}; per 100 g edible portion` }],
    description: { zh: `${zh}为配方计算使用的基础食材。`, en: `${en} is a base ingredient used for recipe resolution.` },
    commonness: 3,
  };
  foods.push(food);
  byId.set(id, food);
}

function addBeverage(id, zh, en, nutrition, hydrationMlPer100g, healthMetrics = {}) {
  addIngredient(id, zh, en, ["food.beverage"], nutrition, healthMetrics);
  const beverage = byId.get(id);
  beverage.hydrationMlPer100g = hydrationMlPer100g;
  beverage.densityGramsPerMl = 1.0;
  beverage.servings = [
    { id: "per_100ml", nutritionTableKey: "standard.100g_edible", ratioToTable: 1.0, labels: { zh: "100 毫升", en: "100 ml" } },
    { id: "cup_250ml", nutritionTableKey: "standard.100g_edible", ratioToTable: 2.5, labels: { zh: "1 杯（250 毫升）", en: "1 cup (250 ml)" } },
  ];
}

function asPrepared(id, ingredientId, cookingMethodId) {
  const food = byId.get(id);
  if (!food) throw new Error(`missing food ${id}`);
  food.kind = "food";
  food.derivedFrom = { ingredientId, cookingMethodId };
  delete food.components;
  delete food.nutritionTables;
  delete food.nutrients;
}

function asDish(id, components, metadata = {}) {
  const food = byId.get(id);
  if (!food) throw new Error(`missing food ${id}`);
  food.kind = "dish";
  food.components = components.map(([foodId, grams]) => ({ foodId, quantity: quantity(grams) }));
  Object.assign(food, metadata);
  delete food.categoryTags;
  delete food.derivedFrom;
  delete food.nutritionTables;
  delete food.nutrients;
}

function asRecipeFood(id, categoryTags, components, metadata = {}) {
  const food = byId.get(id);
  if (!food) throw new Error(`missing food ${id}`);
  food.kind = "food";
  food.categoryTags = categoryTags;
  food.components = components.map(([foodId, grams]) => ({ foodId, quantity: quantity(grams) }));
  Object.assign(food, metadata);
  delete food.derivedFrom;
  delete food.nutritionTables;
  delete food.nutrients;
  delete food.cuisine;
  delete food.dishCategories;
  delete food.tastes;
  delete food.difficulty;
  delete food.seasons;
}

function addRecipeBeverage(id, zh, en, components, hydrationMlPer100g, metadata = {}) {
  if (byId.has(id)) return;
  const food = {
    id,
    kind: "food",
    names: { zh: [zh], en: [en] },
    categoryTags: ["food.beverage"],
    components: components.map(([foodId, grams]) => ({ foodId, quantity: quantity(grams) })),
    hydrationMlPer100g,
    densityGramsPerMl: 1.0,
    servings: [
      { id: "per_100ml", nutritionTableKey: "whole_food", ratioToTable: 1.0, labels: { zh: "100 毫升", en: "100 ml" } },
      { id: "cup_250ml", nutritionTableKey: "whole_food", ratioToTable: 2.5, labels: { zh: "1 杯（250 毫升）", en: "1 cup (250 ml)" } },
    ],
    sources: [{ dataset: "Recipe composition model", reference: "nutrition and hydration resolved from listed components" }],
    description: { zh: `${zh}的营养与饮水量按配方组分计算。`, en: `Nutrition and hydration for ${en} are resolved from the listed components.` },
    commonness: 4,
    ...metadata,
  };
  foods.push(food);
  byId.set(id, food);
}

// Raw sources required by the derived-food migration.
addIngredient("food:taxon:oryza_sativa:wholegrain:raw", "糙米", "Brown rice, raw", ["food.staple", "food.staple.grain", "food.staple.whole_grain"], table(348, 7.5, 2.7, 74.3, 3.5), { glycemicIndex: { value: 50, unit: "GI" } });
addIngredient("food:taxon:solanum_tuberosum:raw", "生土豆", "Potato, raw", ["food.staple.tuber", "food.vegetable"], table(77, 2.0, 0.1, 17.5, 2.2), { glycemicIndex: { value: 56, unit: "GI" } });
addIngredient("food:taxon:gallus_gallus:breast:raw", "生鸡胸肉", "Chicken breast, raw", ["food.meat_egg.poultry"], table(118, 22.5, 1.9, 0.0));
addIngredient("food:taxon:arachis_hypogaea:raw", "生花生", "Peanuts, raw", ["food.nut", "food.soy"], table(567, 25.0, 47.0, 21.7, 8.5), { glycemicIndex: { value: 14, unit: "GI" } });
addIngredient("food:taxon:zea_mays:raw", "玉米", "Sweet corn on the cob", ["food.staple", "food.staple.grain"], table(112, 4.0, 1.2, 22.8, 2.9), { glycemicIndex: { value: 52, unit: "GI" } });
addIngredient("food:taxon:zea_mays:kernels:raw", "玉米粒", "Sweet corn kernels, raw", ["food.staple", "food.staple.grain"], table(112, 4.0, 1.2, 22.8, 2.9), { glycemicIndex: { value: 52, unit: "GI" } });
addIngredient("food:taxon:ipomoea_batatas:raw", "生红薯", "Sweet potato, raw", ["food.staple", "food.staple.tuber"], table(86, 1.6, 0.1, 20.1, 3.0), { glycemicIndex: { value: 54, unit: "GI" } });
addIngredient("food:taxon:colocasia_esculenta:raw", "生芋头", "Taro, raw", ["food.staple", "food.staple.tuber"], table(112, 1.5, 0.2, 26.5, 4.1), { glycemicIndex: { value: 48, unit: "GI" } });
addIngredient("food:taxon:panicum_miliaceum:raw", "小米", "Millet, raw", ["food.staple", "food.staple.grain"], table(358, 9.0, 3.1, 73.5, 1.6), { glycemicIndex: { value: 71, unit: "GI" } });
addIngredient("food:taxon:oryza_sativa:glutinous:raw", "糯米", "Glutinous rice, raw", ["food.staple", "food.staple.grain"], table(348, 7.3, 1.0, 77.0, 0.7), { glycemicIndex: { value: 87, unit: "GI" } });
addIngredient("food:taxon:triticum_aestivum:noodles:dry", "干小麦面条", "Dried wheat noodles", ["food.staple", "food.staple.processed"], table(347, 10.0, 1.0, 72.0, 1.5), { glycemicIndex: { value: 55, unit: "GI" } });
addIngredient("food:taxon:triticum_aestivum:flour:whole", "全麦粉", "Whole-wheat flour", ["food.staple", "food.staple.grain", "food.staple.whole_grain"], table(340, 13.2, 2.5, 66.0, 10.7), { glycemicIndex: { value: 69, unit: "GI" } });
addIngredient("food:taxon:glycine_max:soybean:dried", "黄豆", "Soybeans, dried", ["food.soy"], table(390, 35.0, 16.0, 34.2, 15.5), { glycemicIndex: { value: 15, unit: "GI" } });
addIngredient("food:taxon:nelumbo_nucifera:starch", "莲藕淀粉", "Lotus root starch", ["food.staple", "food.staple.processed"], table(351, 0.5, 0.2, 86.8, 0.3), { glycemicIndex: { value: 85, unit: "GI" } });
addIngredient("food:plant_oil:generic", "植物油", "Vegetable oil", ["food.oil"], table(900, 0.0, 100.0, 0.0));
addIngredient("food:animal_fat:lard", "猪油", "Lard", ["food.oil"], table(897, 0.0, 99.8, 0.0));
addIngredient("food:animal_fat:beef_tallow", "牛油", "Beef tallow", ["food.oil"], table(902, 0.0, 100.0, 0.0));
addIngredient("food:animal_fat:chicken_fat", "鸡油", "Chicken fat", ["food.oil"], table(900, 0.0, 100.0, 0.0));
addIngredient("food:taxon:camellia_sinensis:leaves:dried", "茶叶", "Tea leaves, dried", ["food.beverage"], table(311, 20.0, 5.0, 47.0, 30.0));
addIngredient("food:taxon:coffea_arabica:grounds", "咖啡粉", "Coffee grounds", ["food.beverage"], table(430, 14.0, 15.0, 52.0, 22.0));
addIngredient("food:taxon:citrus_limon:raw", "柠檬", "Lemon", ["food.fruit"], table(35, 1.1, 0.2, 8.5, 1.3), { glycemicIndex: { value: 20, unit: "GI" } });
addIngredient("food:seasoning:brown_sugar", "红糖", "Brown sugar", ["food.seasoning"], table(389, 0.0, 0.0, 97.0), { glycemicIndex: { value: 65, unit: "GI" } });

addBeverage("food:beverage:cola", "可乐", "Cola", table(43, 0.0, 0.0, 10.7), 89.3, { glycemicIndex: { value: 63, unit: "GI" } });
addBeverage("food:beverage:fruit_juice", "果汁", "Fruit juice", table(45, 0.3, 0.1, 10.8), 88.5, { glycemicIndex: { value: 50, unit: "GI" } });
addBeverage("food:beverage:yogurt:plain", "原味酸奶", "Plain yogurt", table(72, 3.2, 3.0, 8.5), 82.0, { glycemicIndex: { value: 36, unit: "GI" } });
addBeverage("food:beverage:milk_tea", "奶茶", "Milk tea", table(67, 1.2, 1.8, 11.2), 85.0, { glycemicIndex: { value: 55, unit: "GI" } });

const drinkingWater = byId.get("food:water:drinking");
drinkingWater.hydrationMlPer100g = 100.0;
drinkingWater.densityGramsPerMl = 1.0;
const wholeMilk = byId.get("food:taxon:bos_taurus:milk:whole");
wholeMilk.hydrationMlPer100g = 87.0;
wholeMilk.densityGramsPerMl = wholeMilk.densityGramsPerMl || 1.03;
addIngredient("food:taxon:allium_fistulosum:raw", "大葱", "Welsh onion", ["food.seasoning", "food.vegetable"], table(30, 1.7, 0.3, 6.5, 1.3), { glycemicIndex: { value: 15, unit: "GI" } });
addIngredient("food:taxon:allium_fistulosum:scallion", "小葱", "Scallion", ["food.seasoning", "food.vegetable"], table(32, 1.8, 0.4, 6.0, 2.6), { glycemicIndex: { value: 15, unit: "GI" } });
addIngredient("food:taxon:houttuynia_cordata:raw", "折耳根", "Houttuynia", ["food.seasoning", "food.vegetable"], table(39, 1.6, 0.2, 8.6, 1.8), { glycemicIndex: { value: 15, unit: "GI" } });
addIngredient("food:seasoning:salt", "食盐", "Table salt", ["food.seasoning"], table(0.0, 0.0, 0.0, 0.0));
addIngredient("food:seasoning:soy_sauce", "酱油", "Soy sauce", ["food.seasoning"], table(63, 5.6, 0.1, 8.8));
addIngredient("food:seasoning:vinegar", "食醋", "Vinegar", ["food.seasoning"], table(31, 0.1, 0.0, 2.1));
addIngredient("food:seasoning:sugar", "白砂糖", "White sugar", ["food.seasoning"], table(400, 0.0, 0.0, 100.0), { glycemicIndex: { value: 65, unit: "GI" } });
addIngredient("food:seasoning:cooking_wine", "料酒", "Cooking wine", ["food.seasoning"], table(105, 1.5, 0.0, 3.0));
addIngredient("food:seasoning:black_pepper", "黑胡椒", "Black pepper", ["food.seasoning"], table(251, 10.4, 3.3, 64.8, 25.3), { glycemicIndex: { value: 15, unit: "GI" } });

const marketCorn = byId.get("food:taxon:zea_mays:raw");
marketCorn.names = { zh: ["玉米", "鲜玉米", "带芯玉米"], en: ["Sweet corn on the cob", "Fresh corn"] };
marketCorn.edibleRatio = 0.46;
marketCorn.servings = [
  { id: "edible_100g", nutritionTableKey: "standard.100g_edible", ratioToTable: 1.0, labels: { zh: "可食 100 克", en: "100 g edible kernels" } },
  { id: "purchased_100g", nutritionTableKey: "standard.100g_edible", ratioToTable: 0.46, labels: { zh: "购买 100 克（约 46 克可食）", en: "100 g purchased (about 46 g edible)" } },
];
marketCorn.description = {
  zh: "菜市场购买的带苞叶、玉米芯的鲜玉米。营养表按玉米粒每 100 克可食部计；购买重量记录时，先按约 46% 换算为可食玉米粒。",
  en: "Fresh market corn sold with husk and cob. Nutrition is per 100 g edible kernels; convert purchased mass using an edible ratio of about 46%.",
};
const packagedKernels = byId.get("food:taxon:zea_mays:kernels:raw");
packagedKernels.names = { zh: ["玉米粒", "包装玉米粒", "剥粒玉米"], en: ["Sweet corn kernels", "Packaged corn kernels"] };
packagedKernels.edibleRatio = 1.0;
packagedKernels.servings = [
  { id: "net_100g", nutritionTableKey: "standard.100g_edible", ratioToTable: 1.0, labels: { zh: "净含量 100 克", en: "100 g net weight" } },
];
packagedKernels.description = {
  zh: "已剥粒的冷藏、冷冻或保鲜膜包装玉米粒。包装净含量通常就是可食重量，不再扣除玉米芯或苞叶。",
  en: "Pre-shelled chilled, frozen, or packaged corn kernels. Package net weight is normally edible mass; do not deduct cob or husk.",
};

[
  "food:taxon:zingiber_officinale:raw",
  "food:taxon:allium_sativum:raw",
  "food:taxon:coriandrum_sativum:raw",
  "food:taxon:capsicum_annuum:chili",
].forEach((id) => {
  const food = byId.get(id);
  food.categoryTags = ["food.seasoning", "food.vegetable"];
});

// Single-source foods resolve from their raw ingredients and a cooking method.
[
  ["food:taxon:oryza_sativa:wholegrain:steamed", "food:taxon:oryza_sativa:wholegrain:raw", "steamed_grain"],
  ["food:taxon:solanum_tuberosum:boiled", "food:taxon:solanum_tuberosum:raw", "boiled"],
  ["food:taxon:gallus_gallus:breast:steamed", "food:taxon:gallus_gallus:breast:raw", "steamed"],
  ["food:taxon:arachis_hypogaea:roasted", "food:taxon:arachis_hypogaea:raw", "grilled"],
  ["food:taxon:zea_mays:boiled", "food:taxon:zea_mays:raw", "boiled"],
  ["food:taxon:ipomoea_batatas:steamed", "food:taxon:ipomoea_batatas:raw", "steamed"],
  ["food:taxon:colocasia_esculenta:boiled", "food:taxon:colocasia_esculenta:raw", "boiled"],
  ["food:taxon:oryza_sativa:glutinous_steamed", "food:taxon:oryza_sativa:glutinous:raw", "steamed_grain"],
  ["food:taxon:triticum_aestivum:noodles_boiled", "food:taxon:triticum_aestivum:noodles:dry", "boiled"],
].forEach(([id, source, method]) => asPrepared(id, source, method));

// Multi-material everyday foods retain food-tree placement and resolve from their recipes.
const recipe = (steps, techniqueId, servesPeople = 1) => ({ recipeSteps: steps.map((text) => ({ text })), techniqueId, servesPeople });
asRecipeFood("food:taxon:triticum_aestivum:mantou", ["food.staple", "food.staple.processed"], [["food:taxon:triticum_aestivum:flour:refined", 58], ["food:water:drinking", 42]], recipe(["面粉加水揉成面团并醒发。", "分剂整形后蒸熟。"], "steamed"));
asRecipeFood("food:taxon:triticum_aestivum:bread_white", ["food.staple", "food.staple.processed"], [["food:taxon:triticum_aestivum:flour:refined", 57], ["food:water:drinking", 29], ["food:taxon:gallus_gallus:egg:whole", 8], ["food:taxon:bos_taurus:milk:whole", 6]], recipe(["混合原料揉成面团并醒发。", "整形后烘烤至熟。"], "baked"));
asRecipeFood("food:taxon:triticum_aestivum:bread_whole_wheat", ["food.staple", "food.staple.whole_grain"], [["food:taxon:triticum_aestivum:flour:whole", 62], ["food:water:drinking", 28], ["food:taxon:gallus_gallus:egg:whole", 5], ["food:taxon:bos_taurus:milk:whole", 5]], recipe(["混合全麦粉和液体揉成面团并醒发。", "整形后烘烤至熟。"], "baked"));
asRecipeFood("food:taxon:panicum_miliaceum:porridge", ["food.staple", "food.staple.grain"], [["food:taxon:panicum_miliaceum:raw", 13], ["food:water:drinking", 87]], recipe(["小米淘洗后加水。", "煮至米粒软烂成粥。"], "stewed"));
asRecipeFood("food:taxon:oryza_sativa:porridge", ["food.staple", "food.staple.grain"], [["food:taxon:oryza_sativa:polished:raw", 13], ["food:water:drinking", 87]], recipe(["大米淘洗后加水。", "煮至米粒软烂成粥。"], "stewed"));
asRecipeFood("food:taxon:oryza_sativa:zongzi", ["food.staple", "food.staple.processed"], [["food:taxon:oryza_sativa:glutinous:raw", 48], ["food:taxon:sus_scrofa:lean_raw", 12], ["food:water:drinking", 40]], recipe(["糯米浸泡，准备馅料。", "包制后加水煮熟。"], "boiled"));
asRecipeFood("food:taxon:oryza_sativa:rice_noodles", ["food.staple", "food.staple.processed"], [["food:taxon:oryza_sativa:polished:raw", 31], ["food:water:drinking", 69]], recipe(["米浆制成米粉。", "入沸水煮熟。"], "boiled"));
asRecipeFood("food:taxon:oryza_sativa:liangpi", ["food.staple", "food.staple.processed"], [["food:taxon:oryza_sativa:polished:raw", 27], ["food:water:drinking", 65], ["food:taxon:glycine_max:tofu:soft", 8]], recipe(["米浆蒸制定型后切条。", "与配料拌匀食用。"], "cold_mixed"));
asRecipeFood("food:taxon:oryza_sativa:rice_cake", ["food.staple", "food.staple.processed"], [["food:taxon:oryza_sativa:glutinous:raw", 54], ["food:water:drinking", 46]], recipe(["糯米浸泡并磨制。", "蒸熟并压制成型。"], "steamed"));
asRecipeFood("food:taxon:nelumbo_nucifera:starch_drink", ["food.staple", "food.staple.processed"], [["food:taxon:nelumbo_nucifera:starch", 18], ["food:water:drinking", 82]], recipe(["莲藕淀粉用少量凉水调匀。", "冲入热水搅拌至透明。"], "stewed"));
asRecipeFood("food:taxon:glycine_max:soy_milk", ["food.soy", "food.beverage"], [["food:taxon:glycine_max:soybean:dried", 8], ["food:water:drinking", 92]], recipe(["黄豆浸泡后加水研磨。", "过滤并煮沸至熟。"], "stewed"));
asRecipeFood("food:taxon:triticum_aestivum:youtiao", ["food.staple", "food.staple.processed"], [["food:taxon:triticum_aestivum:flour:refined", 54], ["food:water:drinking", 26], ["food:taxon:gallus_gallus:egg:whole", 13], ["food:plant_oil:generic", 7]], recipe(["调制面团并醒发。", "整形后油炸至金黄。"], "deep_fried"));
asRecipeFood("food:taxon:triticum_aestivum:meat_bun", ["food.staple", "food.staple.processed"], [["food:taxon:triticum_aestivum:flour:refined", 45], ["food:taxon:sus_scrofa:lean_raw", 28], ["food:water:drinking", 27]], recipe(["面粉加水和面并醒发。", "调制肉馅，包入面皮。", "上锅蒸熟。"], "steamed"));

addRecipeBeverage("food:beverage:tea", "茶水", "Tea", [["food:taxon:camellia_sinensis:leaves:dried", 1], ["food:water:drinking", 99]], 99.0, recipe(["茶叶加热水冲泡。", "按个人浓淡浸泡后饮用。"], "stewed"));
addRecipeBeverage("food:beverage:coffee:black", "黑咖啡", "Black coffee", [["food:taxon:coffea_arabica:grounds", 2], ["food:water:drinking", 98]], 98.0, recipe(["咖啡粉与热水萃取。", "过滤后饮用。"], "stewed"));
addRecipeBeverage("food:beverage:lemon_water", "柠檬水", "Lemon water", [["food:taxon:citrus_limon:raw", 5], ["food:water:drinking", 95]], 95.0, recipe(["柠檬切片后加入饮用水。"], "cold_mixed"));
addRecipeBeverage("food:beverage:brown_sugar_water", "红糖水", "Brown sugar water", [["food:seasoning:brown_sugar", 8], ["food:water:drinking", 92]], 92.0, recipe(["红糖加入热水搅拌至溶解。"], "stewed"));

const soyMilk = byId.get("food:taxon:glycine_max:soy_milk");
soyMilk.hydrationMlPer100g = 92.0;
soyMilk.densityGramsPerMl = 1.01;
soyMilk.servings = [
  { id: "per_100ml", nutritionTableKey: "whole_food", ratioToTable: 1.0, labels: { zh: "100 毫升", en: "100 ml" } },
  { id: "cup_250ml", nutritionTableKey: "whole_food", ratioToTable: 2.5, labels: { zh: "1 杯（250 毫升）", en: "1 cup (250 ml)" } },
];
addRecipeBeverage("food:beverage:soy_milk:commercial", "豆奶", "Soy milk beverage", [["food:taxon:glycine_max:soybean:dried", 5], ["food:seasoning:sugar", 4], ["food:water:drinking", 91]], 91.0, recipe(["黄豆提取液与水、糖调配。", "均质并加热灭菌。"], "stewed"));

for (const food of foods) {
  if (!food.kind) food.kind = "ingredient";
  if (food.kind === "ingredient" && !food.nutritionTables && food.nutrients) {
    food.nutritionTables = { "standard.100g_edible": { basis: quantity(100), nutrients: food.nutrients } };
    delete food.nutrients;
  }
}

fs.writeFileSync(assetPath, `${JSON.stringify(asset, null, 2)}\n`, "utf8");
