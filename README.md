# HealthDietPro

健康饮食管理 App · 目标平台 Android 15+

## 项目定位

帮助用户管理日常饮食摄入，提供食物营养数据查询、饮食记录、个人健康档案管理等核心功能。

## 目标平台

| 项目 | 值 |
|------|-----|
| minSdk | 35 (Android 15) |
| targetSdk | 36 (Android 16) |
| compileSdk | 36 (API level 1) |
| AGP | 9.2.1 |
| Kotlin | 2.0.0 |
| 构建工具 | Gradle Kotlin DSL |

## 技术栈

- **语言**: Kotlin
- **UI**: XML + DataBinding
- **UI组件**: Material Design 3 (com.google.android.material)
- **架构**: 单 Activity + 多 Fragment
- **布局**: ConstraintLayout

## 项目结构

```
app/src/main/java/com/woshiwangnima/healthdietpro/
├── MainActivity.kt              # 宿主 Activity，底栏 Tab 切换
├── ui/
│   ├── nutrition/               # 营养表模块
│   │   └── NutritionFragment.kt
│   ├── record/                  # 记录模块（居中放大 Tab）
│   │   └── RecordFragment.kt
│   └── profile/                 # 个人中心模块
│       └── ProfileFragment.kt
└── model/
    ├── category/                # 食物分类体系
    │   └── FoodTag.kt
    ├── food/                    # 食物数据模型
    │   ├── FoodItem.kt
    │   └── NutritionInfo.kt
    └── unit/                    # 单位换算
        ├── FoodUnit.kt
        └── UnitConverter.kt
```

## 核心数据模型

### 食物分类体系 — FoodTag

参考 Unreal Engine GameplayTag 的点分字符串设计：

```
"主食.谷类及制品.水稻类"
"蔬菜.叶菜类.深色叶菜"
"水果.浆果类"
```

天然支持无限层级，父标签推断靠前缀匹配，一个食物可打多个标签。

一级标签（系统预置，不可修改）：

| 一级分类 | 示例二级 |
|----------|----------|
| 主食 | 谷类及制品、薯类及制品等 |
| 蔬菜 | 叶菜类、根茎类、瓜果类等 |
| 水果 | 仁果类、核果类、浆果类等 |
| 畜禽肉蛋 | 猪肉、牛肉、禽肉、蛋类等 |
| 水产 | 鱼类、虾蟹类、贝类等 |
| 大豆及制品 | 大豆、非发酵豆制品等 |
| 奶类及制品 | 液态奶、酸奶、奶酪等 |
| 坚果 | 树坚果、种子类 |
| 油脂 | 动物油脂、植物油脂 |
| 饮料 | 无糖饮料、含糖饮料等 |
| 调味品 | 盐、酱油、醋、酱类等 |

多级标签定义存储在本地 JSON (`food_categories.json`)，用户不可修改系统标签，但可添加自定义标签。

### 单位换算 — FoodTag / UnitConverter

- **质量类**: g、kg、mg、斤、盎司 → 基准 g
- **体积类**: mL、L、杯、汤匙、茶匙 → 基准 mL
- **个数类**: 个、份
- 跨类换算（质量 ↔ 体积）依赖 `approximateDensity(g/mL)`

### 食物数据

```kotlin
data class FoodItem(
    val id: String,
    val name: String,
    val tags: List<FoodTag>,
    val userCustomTags: List<String>,
    val defaultUnit: FoodUnit,
    val approximateDensityGPerMl: Double?,
    val nutrition: NutritionInfo,
    val otherInfo: FoodOtherInfo  // GI, GL, 炎症指数
)
```

## 功能路线

### MVP 阶段

- [x] 项目框架搭建：单 Activity + 多 Fragment 底栏导航
- [ ] 食物分类树展示（从 JSON 加载）
- [ ] 食物营养表浏览、搜索
- [ ] 饮食记录（每日摄入）
- [ ] 个人资料页

### 后续

- 自定义食物创建
- 营养目标设定
- 数据统计图表
- 食物识别（OCR / 扫码）

## 构建指南

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 数据来源

营养数据参考《中国食物成分表》（标准版），以本地 JSON 格式存储。
