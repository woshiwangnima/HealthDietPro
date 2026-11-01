# HealthDietPro 设计文档

> 配套文档：[`README.md`](../README.md)。本文档描述目标架构（Kotlin + Jetpack Compose + MVVM 模块化），并对照原 XML + DataBinding 实现给出迁移路径。

## 1. 设计目标

1. **本地优先**：无网络层，所有静态数据以 JSON 资源形式打包，离线可用。
2. **多用户隔离**：单设备支持多用户切换，每个用户的档案、体征记录、图表状态、用药记录独立命名空间。
3. **可插拔 UI 组件**：Tab 控件、图表、对话框等以策略接口形式提供接入点，便于主题/动画/指示器自定义。
4. **单向数据流**：MVVM 通过 `ViewModel` + `StateFlow` 暴露 UI 状态，事件自顶向下、状态自底向上。
5. **平稳迁移**：现 XML 实现已验证业务逻辑，Compose 化以逐屏替换为导向，模型层与 Repository 不动。

## 2. 总体架构

```
┌────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                    │
│   Screen Composable ── observes ──> StateFlow<UiState>     │
│        │ events                                   ▲        │
│        ▼                                          │        │
│   ViewModel (MVVM) ── dispatches ──> UseCase ──────┘        │
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────┐
│              Domain Layer (纯 Kotlin，无 Android 依赖)        │
│   Model + 业务规则（BmiUtil、PointInPolygon、ChartMath 等）  │
└────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────┐
│             Data Layer (Repository + Prefs/Asset)          │
│   ProfilePrefs / AppPrefs / UserPrefs / MedicationPrefs    │
│   AssetRepo (units/diseases/provinces/regions/chart/bmi)   │
└────────────────────────────────────────────────────────────┘
```

> Data Layer 按职责再分两层：**存档模块**（ProfilePrefs/AppPrefs/UserPrefs/MedicationPrefs，读写）与 **静态数据只读模块**（AssetRepo，只读）。Domain 层的 `BmiUtil` 属个人信息模块、`PointInPolygon` 属静态数据模块、`ChartMath` 属基础设施模块。

数据流（沿用 txt 文档的概念图）：

```
assets/*.json ──parse+cache──▶ Repositories ──▶ ViewModel ──▶ Compose UI
                                  │                  ▲
                                  └─UnitConverter────┘ (singleton)

DataStore/SharedPreferences ──▶ ProfilePrefs/AppPrefs/TabPersistence ──▶ ViewModel

LocationManager ──▶ CurrentLocationProvider ──▶ RegionRepository.resolve(...)
                                                  └─▶ RegionSnapshot ──▶ UserProfile
```

## 3. 模块划分

9 个模块：5 功能/领域模块 + 界面模块 + 存档模块 + 静态数据只读模块 + 基础设施模块。原"app 存档 / user 切换 / user 存档 / UI 公用"四分法按职责重新分布：持久化收敛进存档模块，数据读取收敛进静态数据只读模块，可复用 UI 收敛进基础设施模块，领域 UI 归属各功能模块，跨域屏归属界面模块。

### 3.1 单位切换模块

**职责**：7 类 76 单位的基准↔用户单位换算与本地化格式，以及单位选择 UI。数据访问经静态数据模块的 `UnitRepository`。

| 组件 | 职责 | 迁移目标 |
|------|------|----------|
| `UnitConverter` | 进程级单例，封装 `UnitRepository`，提供 `toBase/fromBase/formatWithUnit/formatHeightFtIn` | 保留单例，注入 `UnitRepository` |
| 单位选择 UI | 详情屏单位 Spinner/选择器，写入 app 存档偏好 | Compose `DropdownMenu` |

**契约**：`value_in_this_unit × toBase = value_in_base_unit`；`hidden=true` 不出现在选择器。

### 3.2 GPS 定位模块

**职责**：无 GMS 单次定位，输出 lng/lat，交静态数据模块的 `RegionRepository.resolve` 反查 `RegionSnapshot`。

| 组件 | 职责 | 迁移目标 |
|------|------|----------|
| `CurrentLocationProvider` | `GPS_PROVIDER`→`NETWORK_PROVIDER`→`PASSIVE_PROVIDER.getLastKnownLocation`；4s 超时 + consumed guard；权限预检 + `SecurityException` 兜底；主线程回调 | `suspendCancellableCoroutine` 协程化，超时返回 `Result.Err` |

### 3.3 疾病系统模块

**职责**：以「记疾病」替代个人资料中的简单特殊病史，记录用户自身/家族/既往/风险疾病史，浏览完整离线 WHO ICD-11 MMS 分类树，并向营养、血糖、血压和用药提供只读疾病关联。完整方案见 [`DISEASE_RECORD_DESIGN.md`](DISEASE_RECORD_DESIGN.md)。模型与静态目录在静态数据模块，用户疾病史在用户级存档模块。

| 组件 | 职责 |
|------|------|
| 用户疾病记录 | `DiseaseReference` + `UserDiseaseRecord`；历史类型、状态、短期/长期、日期、诊疗信息均经 Repository 写入用户级存档 |
| ICD-11 浏览 | `Icd11Repository` 读取预生成的 WHO MMS 只读快照/索引，支持分类树、面包屑和代码/中英文/别名检索 |
| 筛选与适用性 | 分类、就诊科室、记录状态由 Repository 查询；医学适用性使用 `requiredAnatomicalTraits`，不以性别身份字符串判断 |
| 跨功能关联 | 血糖/血压只展示来源可核验的参考提示，不自动改提醒；用药适应症选择稳定疾病引用，不持久化自由文本标签 |
| 营养建议渲染 | 疾病→`NutrientRecommendation`→`DietaryRecommendation` 展示（INCREASE/FREE/LIMIT/STRICT_LIMIT/FORBID） |

**状态**：当前 curated `DiseaseRepository` 已具备稳定 ID、ICD 映射、分类/科室查询和多语言检索；完整 ICD-11 快照模块、用户疾病记录存档、Compose UI 和迁移待按设计实施。

### 3.4 个人信息模块

**职责**：体征记录与详情，3 小模块。`BodyRecord` 实体与持久化在存档模块，本模块负责记录编辑 UI、BMI 派生算法、详情屏。

| 小模块 | 组件 | 契约 |
|--------|------|------|
| 身高 | `HeightDetailActivity`、`HeightChartActivity` | 双 Tab（图表/数据），`ToggleBar` 共享，`onRecordsChanged` 回流 |
| 体重 | `WeightDetailActivity`、`WeightChartActivity` | 同上 |
| BMI | `BmiDetailActivity`、`BmiUtil`、`BmiReferenceView`、`BmiCalculatorView` | `BmiUtil.buildBmiDataPoints` 双指针派生；Y 轴按 BMI 类别着色；只读 |

**关键不变量**：
- `BodyRecord.value` 始终以基准单位存储；UI 边界换算（保存 `UnitConverter.toBase`，显示 `fromBase`）；`unit` 字段仅记录用户用过的显示单位 id，不可信为值的单位。
- 日期解析：`LocalDate.parse(record.date).atStartOfDay(ZoneId.systemDefault())` → epoch ms。
- 图表状态键由存档模块 `ProfilePrefs.makeChartStateKey` 构造 `_<userId>` 命名空间。

### 3.5 营养表系统模块（需求已敲定，待实现）

**职责**：食物分类树 + 营养表浏览 + 每日饮食记录与摄入汇总 + 自定义食物 + 营养目标 + OCR/扫码。

| 子功能 | 依赖 |
|--------|------|
| 食物分类树 | 多层 Tag（基础设施，待实现） |
| 营养表浏览 | 静态数据 `FoodNutrientRepository`、`Nutrient` |
| 每日饮食记录与摄入汇总 | 存档模块（每日记录持久化）、疾病模块（营养建议对照） |
| 自定义食物 | 存档模块 |
| 营养目标 | 存档模块、静态数据 `nutrient_reference` |
| OCR/扫码 | 待定 |

**设计引用**：[营养表设计方案](NUTRITION_TABLE_DESIGN.md)。该文件定义已确认的信息架构、逐条 food catalog schema、DRIs 人群自动匹配、固定餐次、完整营养素自定义食物与不足/适宜/超标判定规则。

**实现进度**：DRIs 与营养表相关模型、Repository、用户自定义食品档案及浏览/记录界面已逐步实现。内置食物维护源位于 `tools/food_catalog/source/records/`，通过 `tools/food_catalog/catalog_tool.py` 生成 `food_catalog/` 分片、索引和图片 manifest。OCR/扫码不在第一版范围内。

### 3.6 界面模块

**职责**：跨域屏与 app shell——个人资料查看/编辑、用药记录、设置、`MainActivity`。**仅屏，不含可复用 UI 元素**（可复用件归基础设施）。

| 屏 | 组件 | 整合 |
|----|------|------|
| 个人资料查看/编辑 | `ProfileFragment`/`ProfileEditActivity` | GPS + 疾病（多选）+ 地区（级联）+ 存档（自动保存） |
| 用药记录 | 药品目录 + 用药事件记录（Compose） | 存档 `MedicationPrefs`、`WatermarkUtil`（基础设施）；药品定义与实际用药事件分离 |
| 设置 | `AppSettingsActivity`/`PreferencesActivity`/`ReminderSettingsActivity`/`UserSettingsActivity` | 存档 app/user 偏好 |
| app shell | `MainActivity` | 基础设施 Tab（底栏 `main_nav`） |

**个人资料编辑流程要点**：
- `OnBackPressedCallback` 镜像 `saveProfile()`：返回手势始终落盘。
- `checkSaveEnabled()` 与 `originalProfile` diff，仅变化时启用保存按钮。
- `currentDialog` 引用：打开新对话框/Sheet 前先关闭旧，避免堆叠。
- 级联地区选择：`showManualProvinceSheet → showManualCitySheet → showManualDistrictSheet`，每层 dismiss 后展开下一层。
- GPS 反查：`CurrentLocationProvider.getCurrentLocation` → `RegionRepository.resolve(lng, lat, provinceRepo)` → `RegionSnapshot`。
- 头像：`GetContent("image/*")` → 解码 → center-crop 200×200 JPEG → `filesDir/avatars/<timestamp>.jpg`。

### 3.7 存档模块

**职责**：app/user 需要持久化与序列化/反序列化的全部内容。静态数据模块只读，不写。分 app 存档与 user 存档两小模块（原"user 切换"并入 user 存档）。

#### 3.7.1 app 存档

| 组件 | 职责 | 迁移目标 |
|------|------|----------|
| `AppPrefs` | 全局偏好：单位选择、文本溢出模式、图表 UI 状态键（按 `<baseKey>_<userId>` 隔离） | DataStore Preferences |
| `TabPersistence` | Tab 选中索引/集合持久化于 `app_prefs`，int-set 序列化为 StringSet | DataStore |

**契约**：`AppPrefs.getChart*(ctx, chartStateKey)` 与 `setChart*(...)` 涵盖 `chartStyle / chartTimeRange / chartLabelInterval / chartYMin / chartYMax` 五组键值。`chartStateKey` 由 `ProfilePrefs.makeChartStateKey` 构造。删除用户时 `ProfilePrefs.deleteUser` 调用 `AppPrefs` 清理所有以 `_<uid>` 结尾的键。

#### 3.7.2 user 存档

| 实体/组件 | 字段约束 | 持久化位置 |
|-----------|----------|------------|
| `UserProfile` | id、name、gender、birthday、avatarFileName、region(RegionSnapshot)、diseases、heightRecords、weightRecords | `health_diet_prefs/all_users` |
| `BodyRecord` | `date, value: Float(基准单位), unit: String?`，`Serializable` | 内嵌于 `UserProfile`，Intent 间以 `ArrayList<BodyRecord>` 传递 |
| `DataPoint` | `timestamp: Long, value: Float, dateLabel: String` | 不持久化，派生自 `BodyRecord`（基础设施图表层） |
| `MedicationRecord` | 用药记录含位置水印元信息 | `MedicationPrefs` 命名空间 `_<uid>` |
| `UserPrefs` | 用户级偏好（字号、提醒等） | 文件 `user_prefs_<uid>` |
| `ProfilePrefs` | load/save/`setCurrentUserId`/`deleteUser` + 种子 + 迁移 | `health_diet_prefs` |

**多用户存储模型**：

```
health_diet_prefs
├── all_users        : String（List<UserProfile> JSON 序列化）
└── current_user_id  : String
```

**切换流程**：用户点击行 → `ProfilePrefs.setCurrentUserId(id)` → `refreshProfile()`，无需重启。

**新增用户流程**："+ 新增用户" → `ProfileEditScreen(create_new=true)`；`loadProfile()` 合成空 `UserProfile(id="")`（含种子数据）；`saveProfile()` 时 id 为空则用 `System.currentTimeMillis() xor_RANDOM` 铸造新 id 并 `setCurrentUserId` 自动切换；返回手势也保存。

**删除流程**：
1. `ProfilePrefs.deleteUser(ctx, id)` 从 `all_users` 过滤。
2. 删除当前则提升首个剩余用户（或空串）为 `current_user_id`。
3. 级联清理：头像 `filesDir/avatars/<avatarFileName>`；`UserPrefs.deleteUserFile(ctx, id)` 删 `user_prefs_<uid>`；`health_diet_prefs` / `app_prefs` 中以 `_<id>` 结尾的所有键（覆盖图表状态与 MedicationPrefs）。

**迁移**：`ProfilePrefs.ensureMigrated` 在每次 `load / getCurrentUserId / loadUserMap` 时运行，桥接旧单一 profile 与省份字符串至 `RegionSnapshot`。区域补救：`ProfileFragment.refreshProfile` 与 `ProfileEditActivity.updateProvinceDisplay` 在仅有 `provinceCode` 时补全 `provinceName`。

**种子数据**：`ProfilePrefs.load` 对无记录用户合并 `createSeedProfile`（约 30 天 demo 体重 + 6 个月身高），UI 透明。

### 3.8 静态数据只读模块

**职责**：全部 asset JSON 仓储，**只读**（query/sort/filter/resolve，无写）。统一懒加载 + 内存缓存 + 预建索引。

| 仓储 | 数据 | 查询 API |
|------|------|----------|
| `UnitRepository` | `units.json` 7 类 76 单位 | byCode、byCategory |
| `DiseaseRepository` | `diseases.json` 12 疾病 | `getSorted(provinceCode)`（患病率降序，缺失按 `avgPrevalence` 回退）、gender 过滤 |
| `ProvinceRepository` | `provinces.json` 34 省 | `findByPoint(lng, lat)`（射线投射 + 质心抢七） |
| `RegionRepository` | `regions.json` ~136 行 | `resolve(lng, lat, provinceRepo)`（省→最近 city→最近 district）、`byCode`/`citiesByProvince`/`districtsByCity` 索引 |
| `ChartTimeConfigRepository` | `chart_time_config.json` | `Quantity`→ms、按可见窗口选自动间隔、`getFormatForIntervalMs` |
| `BmiClassification`（待建 Repository） | `res/raw/bmi_classification.json` 4 带 | byValue 查带 |
| `FoodNutrientRepository` | `food_catalog/`（由逐条维护源编译） | 按 ID、食物名、分类和营养素筛选排序 |
| `NutrientReferenceRepository`（待建） | `nutrient_reference.json` | DRIs 查询 |

**查询算法**（纯 Kotlin，无 Android 依赖）：
- `PointInPolygon.contains`：经典奇偶射线投射，1e-12 epsilon 避免水平边除零。
- 省解析：遍历所有省多边形（简化矩形互有重叠）找所有含点省，取质心平方欧氏距离最小者。
- 市/区解析：选定省内最近质心选 city，再在该 city 内最近质心选 district。文档化权衡：<1 MB 质心数据 vs 50+ MB 完整多边形，约 1–3 km 边界误差。

**数据 schema**：`Disease`/`Nutrient`/`UnitDef`/`UnitCategory`/`UnitCategoryType`/`Region`/`RegionSnapshot`/`Province`/`Quantity`/`ChartTimeConfig`。

**入口**：`fromContext(context)`（运行时）与 `fromAsset(path)`（测试/离线）。

**序列化注**：`UnitRepository` 唯一使用 `kotlinx.serialization`；其他仓库继续使用 Gson（迁移统一为 kotlinx.serialization 是后续工作）。

### 3.9 基础设施模块

按大类分若干小模块：UI 公用、多层 Tag、图表渲染。

#### 3.9.1 UI 公用（含 Tab 控件）

| 组件 | 职责 | 迁移目标 |
|------|------|----------|
| `BaseBackActivity` → `BaseBackScreen` | 工具栏 + 返回 + 中文标题（`getTitleText()`） | Compose `TopAppBar` |
| `DialogExtensions.showConfirmDialog` | 一行式确认框 | Compose `AlertDialog` |
| `TextOverflowUtil` | 用户偏好驱动的文本溢出（收缩/跑马灯/省略号），安全降级 EditText 与 Spinner | `TextOverflow` 配合 `UserPref` |
| `ViewExtensions` | `applySystemBarInsets()` 状态/导航栏 inset padding | `Modifier.windowInsetsPadding` |
| `DateTimePicker` | 日期→时间链式选择器，返回 epoch 毫秒 | Compose `DatePickerDialog` + `TimePickerDialog` |
| `WatermarkUtil` | 非破坏性照片水印（时间+位置），渐变条带与可选降采样 | `Bitmap`/`ImageBitmap` |
| `MainActivity` 底栏 | `ToggleBar` key `main_nav`，`applyEnlargedTabHeight` | 见 Tab 控件 Compose 化 |

**Tab 控件系统**：抽象 `TabBar`（原 FrameLayout → Compose `Row`/`Column`）支持水平/垂直、等权重 vs 可滚动（阈值 `maxVisible`）、`CENTER_HIGHLIGHT`。三种可插入策略：

| 接口 | 默认实现 | 作用 |
|------|----------|------|
| `TabBinder` | `DefaultTabBinder` | 项数据 → 视图（字号、颜色、图标色、背景） |
| `TabAnimator` | `DefaultTabAnimator` | 项选中动画（scale 14/12、alpha、elevation、center raise + OvershootInterpolator） |
| `TabIndicator` | `SlidingIndicator` | 覆盖指示器：滑动胶囊仅在水平 + 不可滚动时启用 |

具体子类：`ToggleBar`（单选，`restore()` 不触发 listener，持久化 `TabPersistence.saveIndex`）；`FilterBar`（多选，默认 `maxVisible=4` 滚动，持久化 int-set）。协调器 `MultiLevelTabCoordinator`：多条 Bar 注册到 `tab_${screenId}_level_${level}`，统一 `onLevelChanged(level, selection)` 回调。当前仅 `DemoTabsActivity` 演示，生产未采用（保留接入点）。**命名空间后续接入多层 Tag。**

Compose 化：`TabRow`/`ScrollableTabRow` 不能覆盖凸起/自定义动画/滑动指示器，改自定义 `Layout` + `Modifier`，保留三策略接口（`@Composable`）；动画切 `animate*AsState` + `Animatable`，OvershootInterpolator 改 `Easing`。

#### 3.9.2 多层 Tag（待实现）

GameplayTag 式层级标签系统（参考 Unreal GameplayTag）：点分字符串、层级查询、继承/包含语义。将替代当前 Tab 命名空间字符串拼装（`tab_${screenId}_level_${level}`），并支撑营养表系统的食物分类树（`FoodTag` 点分体系）。当前未实现，为绿地区域。

#### 3.9.3 图表渲染

`ChartView` + `ChartCanvas` 迁移为 Compose `Canvas` + `Layout`：

| 原组件 | Compose 等价 |
|--------|-------------|
| `ChartCanvas.onDraw` | `Canvas(modifier) { drawIntoCanvas { ... } }` |
| `ChartMath` 各插值 | 纯 Kotlin，无 Android 依赖，直接复用 |
| `ChartView` 控件区（spinner / EditText） | Compose `Slider` / `TextField` |
| 时间轴拖动 | `Modifier.pointerInput` + `detectDragGestures` |
| 触摸十字准线 | `Modifier.pointerInput` + `detectTapGestures` |
| 全屏 Activity | `ChartFullscreenScreen`（横屏、`WindowInsetsController.hide`） |
| `ChartFullscreenActivity.pendingData` 静态槽 | `RememberSaveable` + `Parcelable` `ChartFullscreenData` |

绘制分层（保留）：① Y 轴背景带（`YAxisBand` 半透明色块）② 5 条 `niceScale` 网格线 + 轴框（`gridLineStyles` 循环 dash）③ X 轴刻度（高度随间隔桶增长）④ 序列路径（LINEAR/STEPPED_FRONT/STEPPED_BACK 直线；BEZIER/SPLINE/CATMULL_ROM/MONOTONE 200 点采样）⑤ X 轴标签（`ChartTimeConfigRepository.getFormatForIntervalMs`，回退 mm:ss / HH:mm / MM-dd HH:mm / MM-dd）⑥ 十字准线覆盖（虚线 + 主序列圆点 + 信息气泡，边缘夹取）。

线型：`LineStyle` 6 种（LINEAR / BEZIER / SPLINE / CATMULL_ROM / MONOTONE / STEPPED_FRONT / STEPPED_BACK），`LineStyle.fromSpinnerPosition` 映射。`LineType`（SOLID/DASHED/DOTTED）、`PointShape`（CIRCLE/TRIANGLE/SQUARE/DIAMOND/CROSS）、`PointFill`（FILLED/HOLLOW）。

状态持久化：所有控件状态以 `chartStateKey` 为前缀写入存档模块 `AppPrefs`，键 `chartStyle / chartTimeRange / chartLabelInterval / chartYMin / chartYMax`，`Long.MAX_VALUE` 表示"全部"时间范围。`DataPoint`/`RecordHistory` 为图表层数据结构（`RecordHistory` 待生产采用）。

## 4. 数据模型契约

### 4.1 单位（Unit）

```kotlin
@Serializable
data class UnitDef(
    val id: String,
    val i18n: Map<String, UnitI18n>,
    val toBase: Float,
    val hidden: Boolean = false
)

@Serializable
data class UnitCategory(
    val id: String,
    val i18n: Map<String, UnitI18n>,
    val baseUnit: String,
    val units: List<UnitDef>
)

@Serializable
data class UnitI18n(
    val label: String = "",
    val symbol: String = ""
)

enum class UnitCategoryType(val id: String, val defaultUnitId: String) {
    Unknown("", ""), Weight("weight", "kg"), Length("length", "cm"),
    Volume("volume", "l"), Density("density", "g_ml"), Time("time", "s"),
    Energy("energy", "kcal"), Storage("storage", "b");
    companion object { fun fromId(id: String): UnitCategoryType = ... }
}
```

不变量：`value_in_this_unit × toBase = value_in_base_unit`；`hidden=true` 不出现在选择器（如存储字节）。
注：`UnitRepository` 唯一使用 `kotlinx.serialization`；其他仓库继续使用 Gson（迁移统一为 kotlinx.serialization 是后续工作）。

### 4.2 营养素

```kotlin
enum class Nutrient(val id: String, val displayName: String, val unit: String) {
    SODIUM("sodium", "钠", "mg"), POTASSIUM("potassium", "钾", "mg"),
    ENERGY("energy", "能量", "kcal"), PROTEIN("protein", "蛋白质", "g"),
    FAT("fat", "脂肪", "g"), CARBOHYDRATE("carbohydrate", "碳水化合物", "g"),
    DIETARY_FIBER("dietary_fiber", "膳食纤维", "g"), CHOLESTEROL("cholesterol", "胆固醇", "mg"),
    SATURATED_FAT("saturated_fat", "饱和脂肪", "g"), SUGAR("sugar", "糖", "g"),
    CALCIUM("calcium", "钙", "mg"), IRON("iron", "铁", "mg");
    companion object { fun fromId(id: String): Nutrient? = ... }
}
```

12 项营养素的 `id` 与 `diseases.json` 的 `nutrientId` 键一一对应。

### 4.3 地区（Region）

```kotlin
data class Region(val level: String, val code: String, val parentCode: String, val lng: Double, val lat: Double)

data class RegionSnapshot(
    val lng: Double, val lat: Double,
    val provinceCode: String, val provinceName: String,
    val cityCode: String, val cityName: String,
    val districtCode: String, val districtName: String
) {
    fun display(): String  // 非空层级以 " - " 连接，全空则 "未设置"
    fun isEmpty(): Boolean
}

data class Province(val code: String, val name: String, val polygons: List<List<DoubleArray>>, val lng: Double, val lat: Double)
```

resolution pipeline：省 = `ProvinceRepository.findByPoint`（射线投射 + 质心抢七）→ 该省最近 city（`nearest` 平方欧氏距离）→ 该 city 最近 district。

### 4.4 疾病（Disease）

```kotlin
enum class DietaryRecommendation { INCREASE, FREE, LIMIT, STRICT_LIMIT, FORBID }

data class NutrientRecommendation(val nutrientId: String, val recommendation: DietaryRecommendation)

data class Disease(
    val id: String,
    val name: String,
    val gender: List<String> = listOf("MALE", "FEMALE"),
    val prevalence: Map<String, Float>,           // 2 位省份代码 -> 0..1 患病率
    val nutrientRecommendations: List<NutrientRecommendation>
)
```

`DiseaseRepository.getSorted(provinceCode)`：按区域患病率降序排序，省份缺失时按全国平均 `avgPrevalence` 回退。`gender` 用于过滤适用性（如 PCOS 仅女性）。

### 4.5 配置文件（Profile）

```kotlin
data class BodyRecord(val date: String, val value: Float, val unit: String?) : Serializable {
    fun getUnit(isWeight: Boolean): String  // 缺省回退 UnitCategoryType.Weight/Length.defaultUnitId
}

data class DataPoint(val timestamp: Long, val value: Float, val dateLabel: String)

enum class Gender { MALE, FEMALE }   // 值与 Disease.gender 字符串一致

data class AppDate(val date: String, val time: String? = null) {
    val displayText: String
    val isDateTime: Boolean
}

data class UserProfile(
    val id: String,
    val name: String,
    val gender: Gender,
    val birthday: String,
    val avatarFileName: String?,
    val region: RegionSnapshot,
    val diseases: List<String>,
    val heightRecords: List<BodyRecord>,
    val weightRecords: List<BodyRecord>
)
```

### 4.6 图表配置

```kotlin
data class Quantity(val value: Double, val unit: String) {
    fun toMillis(unitRepo: UnitRepository): Long   // (value * unitDef.toBase * 1000).toLong()
    fun getDisplayName(unitRepo: UnitRepository): String
}

data class ChartTimeConfig(
    val timeRangeOptions: List<TimeRangeOptionDef>,
    val intervalOptions: List<IntervalOptionDef>,
    val autoIntervalRules: List<AutoIntervalRuleDef>
)
```

### 4.7 图表序列

```kotlin
enum class LineType { SOLID, DASHED, DOTTED }
enum class PointShape { CIRCLE, TRIANGLE, SQUARE, DIAMOND, CROSS }
enum class PointFill { FILLED, HOLLOW }

data class ChartSeries(
    val points: List<DataPoint>,
    val label: String,
    val color: Int,
    val lineStyle: LineStyle,
    val lineType: LineType,
    val pointShape: PointShape,
    val pointFill: PointFill
)

enum class LineStyle { LINEAR, BEZIER, SPLINE, CATMULL_ROM, MONOTONE, STEPPED_FRONT, STEPPED_BACK } {
    fun fromSpinnerPosition(pos: Int): LineStyle
    fun toSpinnerPosition(): Int
}
```

### 4.8 BMI

```kotlin
data class BmiBand(val min: Float, val max: Float, val label: String, val color: Int)

// 中国标准：
//   <18.5   体重过低  #264CAF50（alpha 0x26 ~15%）
//   18.5–24 体重正常
//   24–28  超重
//   ≥28    肥胖
```

`BmiUtil.buildBmiDataPoints(weightRecords, heightRecords)`：两组时间排序记录上做双指针前进，每个唯一日期取当天或之前最新的 weight 与 height，计算 BMI，`dateLabel = date.takeLast(5)`（MM-dd）。

## 5. 资源资产约束

| 文件 | 规模 | 关键边界 |
|------|------|---------|
| `units.json` | 7 类 76 单位 | 存储单位 `hidden=true`；基准 cm/kg/l/g_ml/s/kcal/b |
| `diseases.json` | 12 种疾病 | gender 仅 PCOS 为 `["FEMALE"]`；prevalence 键为 2 位省码 |
| `provinces.json` | 34 省 | 多边形为简化边界矩形（4 角），非真实行政轮廓 |
| `regions.json` | ~136 行 | 仅 city/district 级，按省组织（无省级） |
| `chart_time_config.json` | 17 固定间隔 + auto | `autoIntervalRules` 7 阈值 1h→1y |
| `res/raw/bmi_classification.json` | 4 带 | 颜色 ARGB 含 alpha 字段 |

## 6. 关键流程

### 6.1 用户档案自动保存

`ProfileEditActivity`（迁移后 `ProfileEditScreen`，界面模块）：
- `OnBackPressedCallback` 镜像 `saveProfile()`：返回手势始终落盘。
- `checkSaveEnabled()` 与 `originalProfile` 做 diff，仅在变化时启用保存按钮。
- `currentDialog` 引用：打开新对话框/Sheet 前先关闭旧对话框，避免堆叠。
- 级联地区选择：`showManualProvinceSheet → showManualCitySheet → showManualDistrictSheet`，每层 dismiss 当前 Sheet 后展开下一层。
- GPS 反查：`CurrentLocationProvider.getCurrentLocation` → `RegionRepository.resolve(lng, lat, provinceRepo)` → `RegionSnapshot` 写入 `UserProfile.region`。
- 疾病多选：按 `Disease.gender` 过滤，不适用项以删除线 + alpha 0.4 禁用。
- 头像：`GetContent("image/*")` → 解码 → center-crop 200×200 JPEG → `filesDir/avatars/<timestamp>.jpg`。

### 6.2 身高/体重详情

`HeightDetailActivity` / `WeightDetailActivity`（对应 Compose Screen，个人信息模块）：
- 双 Tab（图表/数据），用 `ToggleBar` 共享控件，持久化键 `tab_height_chart` / `tab_weight_chart`。
- Tab 图表：`ChartView` 直接挂入内容区，`chartStateKey = makeChartStateKey(ctx, "height_history" / "weight_history")`。
- Tab 数据：`DataListFragment.newInstance(records, unit, category, isHeight)`，记录增删改通过 `onRecordsChanged` 回流。
- 返回：图表 Tab 支持共享元素转场（`supportFinishAfterTransition`）；数据 Tab 保存并 `setResult(RESULT_OK)` 返回 `ArrayList<BodyRecord>`。
- 数据 Tab 的 BottomSheet 增删改：保存前 `UnitConverter.toBase(category, value, unitId)`，保证存档恒为基础单位。

### 6.3 BMI 详情

`BmiDetailActivity`（只读，个人信息模块）：
- 直接读 `ProfilePrefs.load(this)`（与其他详情不同，因为 BMI 为派生量而非编辑量）。
- `BmiUtil.buildBmiDataPoints(weightRecords, heightRecords)` 双指针派生 BMI 时间序列。
- 图表 Tab：Y 轴带按 BMI 类别着色（`YAxisBand`），`chartStateKey = bmi_history_<uid>`，单位 `kg/m²`。
  - 第一卡：ChartView 45% 屏高
  - 第二卡：`BmiReferenceView` BMI 中国标准对照表
  - 第三卡：`BmiCalculatorView` 互动 BMI 计算器（身高/体重 EditText 实时计算）
- 数据 Tab：按时间戳降序列出所有 BMI 点，每行显示日期 + 值 + `BmiUtil.getBmiLabel` 着色。
- Tab 持久化键 `tab_bmi_chart`。
- 切 Tab 重建内容视图（`content.removeAllViews()`），只读历史下滚动位置不保留，可接受。

### 6.4 多用户切换

```
ProfileScreen(profile VM)
  ├── settingsBtn     → AppSettingsScreen       (result refresh)
  ├── profileDetailBtn→ ProfileEditScreen       (create_new=false; result refresh)
  │     ├── heightRow → HeightDetailScreen
  │     ├── weightRow → WeightDetailScreen
  │     └── bmiRow    → BmiDetailScreen         (read-only)
  ├── userSettingsBtn → UserSettingsScreen      (no result)
  └── switchUserBtn   → ModalBottomSheet
        ├── "+ 新增用户" → ProfileEditScreen(create_new=true)
        ├── row tap      → setCurrentUserId → refreshProfile
        └── delete        → AlertDialog → deleteUser → refreshProfile

ChartView.btnFullscreen → ChartFullscreenScreen (landscape + immersive)
                          退出 "缩放" 按钮 → finish()
```

### 6.5 GPS 定位（无 GMS）

`CurrentLocationProvider`（GPS 定位模块）：
- 优先 `GPS_PROVIDER` → 次选 `NETWORK_PROVIDER` → 兜底 `PASSIVE_PROVIDER.getLastKnownLocation`。
- 4 秒超时 + consumed guard，避免 timeout 与 onLocationChanged 双触发。
- 权限预检 `ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION`，`@SuppressLint("MissingPermission")` + 运行时 `SecurityException` 兜底。
- 主线程回调；协程化后改为 `suspendCancellableCoroutine`，超时返回 `Result.Err`。

### 6.6 区域解析混合策略

省（`ProvinceRepository.findByPoint`，静态数据模块）：
- 遍历所有省多边形（简化矩形互有重叠），找出**所有**含该点的省。
- 在候选中取质心平方欧氏距离最小者，避免 JSON 顺序"first hit wins"不稳定。
- 多边形环通过 `PointInPolygon.contains` 经典奇偶射线投射，含 1e-12 epsilon 避免水平边除零。

市/区（`RegionRepository`，静态数据模块）：
- 预建 `byCode / citiesByProvince / districtsByCity` 索引。
- 在选定省内做最近质心（`(r.lng-lng)² + (r.lat-lat)²` 最小）选 city，再在该 city 内最近质心选 district。
- 文档化权衡：<1 MB 质心数据 vs 50+ MB 完整多边形，约 1–3 km 边界误差。

## 7. 公约与边界

- **BodyRecord.value 始终基准单位**；`unit` 字段不可信为值的单位，仅为 UI 提示。
- **.saveProfile() 是唯写入入口**：所有写操作最终经 `ProfilePrefs.save`，新用户铸造 id 后自动 `setCurrentUserId`。
- **删除级联**：必须清理头像、`user_prefs_<uid>` 文件、`_<uid>` 后缀键，否则切回旧 id 会读到残留。
- **种子数据透明合并**:每次 `load` 都补齐缺失日期，UI 透明。
- **每用户隔离**：所有按用户状态必须经 `ProfilePrefs.makeChartStateKey(baseKey)` 构造后缀。
- **空 chartStateKey 不持久化**：`ChartView` 在 key 为空时直接 early-return，使其可作为一次性图表。
- **`ChartFullscreenActivity.pendingData` 是 `@Volatile` 静态**：进程被系统杀死时 onCreate 会读到 null 自行 finish()。后续应改为 `Parcelable` + `SavedStateHandle`。
- **静态数据模块只读**：所有写操作走存档模块，仓储不持有写入口。

## 8. 待实现 / 绿地区域

1. **疾病系统 UI**（当前 stub）：疾病选择 + 地区患病率展示 + 营养建议渲染。模型层已就绪，UI 待建。
2. **营养表系统**（全功能）：食物分类树 + 营养表浏览 + 每日饮食记录与摄入汇总（与疾病营养建议对照）+ 自定义食物创建 + 营养目标 + OCR/扫码识别。含逐条 food catalog 资产、`nutrient_reference.json` 资产与 `FoodNutrientRepository`、`NutrientReferenceRepository` 仓储建设。
3. **多层 Tag**（基础设施）：GameplayTag 式层级标签系统，替代 Tab 命名空间字符串拼装，支撑食物分类树。
4. **`MultiLevelTabCoordinator` 生产接入**：当前仅 `DemoTabsActivity` 演示，未在真实多级导航屏使用。
5. **`RecordHistory` 接入**：图表屏现直接通过自身 `parseRecords / buildBmiDataPoints` 构造 `DataPoint`；`RecordHistory` 通用 helper 已就绪待采用。
6. **持久化升级**：SharedPreferences → DataStore（存档模块，异步、协程化、类型安全）。
7. **`ChartFullscreenData` Parcelable 化**：替换进程内静态槽位，避免进程死亡丢数据。
8. **界面模块 Compose 迁移**：个人资料/用药记录/设置各屏 Compose 化。

## 9. 测试边界

- Repository 层：`fromAsset(path)` 提供纯文件入口，便于 Robolectric/JVM 单测（`RegionRepository`、`ProvinceRepository` 已有单测）。
- 纯算法：`PointInPolygon`、`ChartMath`、`BmiUtil` 无 Android 依赖，可直接 JVM 测试。
- 协程化 Repository 与 `CurrentLocationProvider` 可通过 `runTest` + `TestDispatcher` 控制。
- 验证不变量：`BodyRecord.value` 始终基础单位、`ProfilePrefs.deleteUser` 级联完整性、`UnitConverter` 双向换算闭环。

## 10. 迁移路径（XML → Compose）

建议按屏分批迁移，模型层与 Repository 不动：

1. **基础设施先行**：主题、Navigation Compose、`BaseScreen`、`AppSettings`、`Preferences`。
2. **公用组件 Compose 化**：Tab 控件（保留三策略）、对话框、Toast、`DateTimePicker`。
3. **界面模块 - 个人资料**：`ProfileFragment` → `ProfileScreen` + `UserSwitchViewModel`，从原 Fragment 读 ProfilePrefs 验证一致性。
4. **个人信息模块 - 详情屏**：身高/体重/BMI 详情双 Tab，ChartView 用 Compose Canvas 重写并保留 `chartStateKey` 持久化方案。
5. **界面模块 - 记录与设置**：`MedicationRecordScreen`、`UserSettingsScreen`。
6. **疾病系统 UI**：新功能，纯 Compose 实现。
7. **营养表系统**：新功能，纯 Compose 实现（依赖多层 Tag）。
8. **DataStore 替换**：迁移完后切到 DataStore，提供一次性 SharedPreferences 读取兼容。

每屏迁移完成后保留原 Fragment 一段时间做对照，逐步删除。

---

参考盘点文档：`新建 文本文档 (2).txt`（Per-File Analysis / Layer-Level Summary / Profile & Chart Module Breakdown）。
