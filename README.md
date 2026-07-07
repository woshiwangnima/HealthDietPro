# HealthDietPro

健康饮食管理 App · 目标平台 Android 15+

## 项目定位

帮助用户管理日常饮食摄入，结合个人健康档案、地域性疾病流行度与营养建议，提供食物营养数据查询、饮食记录、身高/体重/BMI 历史图表等核心功能。支持多用户切换、本地优先的数据持久化与无网络环境下的完整体验。

## 目标平台

| 项目 | 值 |
|------|-----|
| minSdk | 35 (Android 15) |
| targetSdk | 36 (Android 16) |
| compileSdk | 36 |
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| 构建工具 | Gradle Kotlin DSL |

## 技术栈（已确定）

- **语言**: Kotlin 2.2.10
- **UI**: Jetpack Compose（Material Design 3）
- **架构**: MVVM（ViewModel + StateFlow + 单向数据流）
- **导航**: 单 Activity + 多 Composable 屏（Navigation Compose）
- **持久化**: SharedPreferences 过渡到 DataStore（JSON 索引与多用户存档）
- **序列化**: kotlinx.serialization（替换 Gson）
- **协程**: Kotlin Coroutines + Flow
- **依赖注入**: 手动构造 / 后续可接入 Hilt

> 项目演进路线：从原 XML + DataBinding 单 Activity 多 Fragment 迁移至 Jetpack Compose + MVVM。本文档与配套设计文档 `docs/DESIGN.md` 描述目标架构。

## 模块划分

项目按职责拆分为 **9 个模块**：5 个功能/领域模块 + 界面模块 + 存档模块 + 静态数据只读模块 + 基础设施模块。

### 功能/领域模块

| 模块 | 职责 | 关键组件 | 状态 |
|------|------|----------|------|
| **单位切换** | 7 类 76 单位基准↔用户单位换算与本地化格式（含 ft/in）+ 单位选择 | `UnitConverter` | 已实现 |
| **GPS 定位** | 无 GMS 单次定位（4s 超时、权限预检、最后已知兜底） | `CurrentLocationProvider` | 已实现（协程化为迁移项） |
| **疾病系统** | 疾病 UI（选择 + 地区患病率 + 营养建议渲染）+ 性别过滤；模型/数据在静态数据模块 | 疾病 UI（原 `NutritionFragment` stub） | 模型已实现+单测；UI 待实现 |
| **个人信息** | 体征记录与详情，3 小模块：**身高** / **体重** / **BMI** | `Height/Weight/BmiDetailActivity`、`BmiUtil`、`BmiReferenceView`、`BmiCalculatorView` | 已实现（XML，待 Compose） |
| **营养表系统** | 食物分类树 + 营养表浏览 + 每日饮食记录与摄入汇总 + 自定义食物 + 营养目标 + OCR/扫码 | 待建 | 待实现（绿地） |

### 界面模块（仅屏，不含可复用 UI 元素）

- 个人资料查看/编辑（`ProfileFragment`/`ProfileEditActivity`，整合 GPS + 疾病 + 地区）
- 用药记录（`MedicationRecord*`/`RecordInputDialog`）
- 设置（`AppSettings`/`Preferences`/`Reminder`/`UserSettings`）
- `MainActivity`（app shell + 底栏导航）

### 存档模块（app 存档 + user 存档）

- **app 存档**：`AppPrefs`、`TabPersistence`（图表状态键命名空间 + Tab 选择）
- **user 存档**：`ProfilePrefs`（load/save/切换/删除级联 + 种子 + 迁移）、`UserPrefs`、`MedicationPrefs`、`UserProfile`/`BodyRecord` 实体、头像

### 静态数据只读模块（只读，无写）

- 仓储：`UnitRepository`/`DiseaseRepository`/`ProvinceRepository`/`RegionRepository`/`ChartTimeConfigRepository`（+未来 `FoodNutrientRepository`、营养素参考 Repository）
- 查询算法：`PointInPolygon`、resolve 管线、索引、`getSorted`/`findByPoint`
- 数据 schema：`Disease`/`Nutrient`/`UnitDef`/`Region`/`RegionSnapshot`/`Quantity`/`ChartTimeConfig`
- 统一入口 `fromContext` / `fromAsset`

### 基础设施模块（若干小模块）

- **UI 公用**：`BaseBack`/`BaseActivity`、`DialogExtensions`、`TextOverflowUtil`、`ViewExtensions`、`DateTimePicker`、`WatermarkUtil`、**Tab 控件**（`TabBar`/`ToggleBar`/`FilterBar` + 三策略 `TabBinder`/`TabAnimator`/`TabIndicator` + `MultiLevelTabCoordinator`）
- **多层 Tag**：GameplayTag 式层级标签系统（点分字符串、层级查询、继承/包含语义）——**待实现**；食物分类树（营养表系统）与 Tab 命名空间（`tab_${screenId}_level_${level}`）后续接入此能力替代字符串拼装
- **图表渲染**：`ChartView`/`ChartCanvas`/`ChartMath`/`ChartSeries`/`LineStyle`/`ChartFragment`/`ChartFullscreen` + `DataPoint`/`RecordHistory`

### 跨模块依赖

```
静态数据只读 ◀── 单位切换 / 疾病系统 / 个人信息 / 营养表系统 / 图表渲染
GPS 定位 ──▶ 静态数据（地区 resolve） ──▶ 疾病系统（省份码→患病率）
疾病系统（营养素+建议） ──▶ 营养表系统（摄入对照）
图表渲染 ◀── 个人信息 / 营养表系统
存档模块 ◀── 个人信息 / 界面模块 / Tab 控件
界面模块（个人资料编辑） ──▶ GPS + 疾病 + 静态数据 + 存档
```

## 静态数据资产（静态数据只读模块）

无网络层，所有静态数据位于 `app/src/main/assets/*.json` 与一处 `res/raw`：

| 资源 | 内容 |
|------|------|
| `units.json` | 7 类共 76 个单位（长度/质量/体积/密度/时间/能量/存储），基准单位 cm、kg、l、g_ml、s、kcal、b |
| `diseases.json` | 12 种疾病（高血压、2 型糖尿病、血脂异常、PCOS 仅女性、脂肪肝、冠心病、慢性肾病、OSA、骨关节炎、高尿酸血症、甲状腺功能减退、偏头痛），含地区患病率与营养建议 |
| `provinces.json` | 34 省简化边界矩形 + 质心 |
| `regions.json` | ~136 行市/区质心（无省级） |
| `chart_time_config.json` | 时间范围 / 间隔 / 自动间隔阈值规则 |
| `res/raw/bmi_classification.json` | 4 档 BMI 分带（中国标准） |
| `food_nutrition.json`（待建） | 食物营养数据（《中国食物成分表》标准版） |
| `nutrient_reference.json`（待建） | 营养素参考摄入（DRIs） |

Repository 模式：按需求懒加载、内存缓存，查找通过预建索引（`byCode`、`citiesByProvince`、`districtsByCity`）。统一构造入口 `fromContext(context)`（运行时）与 `fromAsset(path)`（测试/离线）。**只读**：所有写操作走存档模块。

跨仓库依赖：`ChartTimeConfigRepository` 依赖 `UnitRepository`（Quantity 转 ms）；`RegionRepository.resolve` 依赖 `ProvinceRepository`（省射线投射）后做质心下降。

区域解析混合策略：省 = 简化矩形射线投射 + 质心抢七；市/区 = 分层最近质心。以 <1 MB 数据换 ~1–3 km 边界误差。

## 存档模块与多用户持久化

存档模块分 **app 存档** 与 **user 存档** 两个小模块，负责所有需要持久化与序列化/反序列化的内容（静态数据模块只读，不写）。

- **多用户**：所有 `UserProfile` JSON 序列化在 `health_diet_prefs/all_users`；当前用户 ID 在 `current_user_id`。
- **体征记录**：`BodyRecord` 始终以基准单位存储（cm / kg）；UI 边界转换为用户偏好单位（保存时 `UnitConverter.toBase`，展示时 `fromBase`）。
- **种子数据**：无记录的用户合并 `createSeedProfile`，约 30 天体重 + 6 个月身高 demo 点，保证新账号也有图表数据。
- **每用户键**：图表 UI 状态、`UserPrefs`、`MedicationPrefs` 均以 `<baseKey>_<userId>` 命名空间隔离，`ProfilePrefs.makeChartStateKey` 为标准构造器。
- **删除级联**：清理头像文件、`user_prefs_<uid>` 文件、以及 `health_diet_prefs` / `app_prefs` 中以 `_<uid>` 结尾的所有键。
- **迁移**：`ProfilePrefs.ensureMigrated` 每次加载时桥接旧的单一 profile 形态和省份字符串到 `RegionSnapshot`。

## 核心功能盘点

### 单位切换模块
7 类 76 单位的基准↔用户单位换算与本地化格式（含 ft/in 解析），由 `UnitConverter` 单例外壳封装 `UnitRepository`。单位选择写入 app 存档偏好。

### GPS 定位模块
无 GMS 单次 GPS 定位：优先 `GPS_PROVIDER` → 次选 `NETWORK_PROVIDER` → 兜底 `PASSIVE_PROVIDER.getLastKnownLocation`，4 秒超时 + consumed guard 防双触发，权限预检 + `SecurityException` 兜底。

### 疾病系统模块
12 疾病按地区患病率降序排序（`DiseaseRepository.getSorted`，省份缺失按全国平均回退），性别过滤适用性（如 PCOS 仅女性）。**疾病→营养建议 UI 待实现**：疾病选择 + 患病率展示 + 营养建议渲染（原 `NutritionFragment` stub）。

### 个人信息模块
- **身高 / 体重**：详情双 Tab（图表/数据），`ToggleBar` 共享控件，记录增删改通过 `onRecordsChanged` 回流，保存前 `UnitConverter.toBase` 保证存档恒为基础单位。
- **BMI**：由体重+身高时间序列派生（`BmiUtil.buildBmiDataPoints` 双指针遍历），图表 Y 轴按 BMI 类别着色，附带 BMI 中国标准对照卡与 BMI 计算器卡。

### 营养表系统模块（待实现）
食物分类树展示与食物营养表浏览（按食物名/营养素筛选排序）+ 每日饮食记录与摄入汇总（与疾病营养建议对照）+ 自定义食物创建 + 营养目标 + OCR/扫码食物识别。模型层（`Nutrient`、`Disease.nutrientRecommendations`、`DietaryRecommendation`）已就绪，UI 与食物数据待建。

### 界面模块
- **个人资料**：只读档案卡（头像、姓名、性别、年龄、生日、地区、疾病史空时自动隐藏）；完整编辑（身份、三级省/市/区级联选择含 GPS 反查、按性别过滤的疾病多选）；返回自动保存。
- **用药记录**：用药记录列表与录入，含位置水印元信息。
- **设置**：软件设置、用户设置、提醒、偏好。

### 基础设施亮点
- **图表渲染（全自研 Canvas）**：`ChartView` + `ChartCanvas`，`ChartMath` 7 种线型插值（线性/阶梯/贝塞尔/Catmull-Rom/单调三次 Hermite/自然三次 Spline）+ `niceScale` 轴刻度；6 线型 5 点状虚/点/实线；触摸十字准线、时间轴拖动条、4 秒自动隐藏、Y 范围垂直滑动；横屏沉浸式全屏。
- **Tab 控件（三策略）**：抽象 `TabBar`（水平/垂直、等权重 vs 可滚动、`CENTER_HIGHLIGHT`）+ `ToggleBar`（单选）/`FilterBar`（多选）+ `MultiLevelTabCoordinator` 多级命名空间；可插入 `TabBinder`/`TabAnimator`/`TabIndicator`。
- **多层 Tag（待实现）**：GameplayTag 式层级标签系统，将替代当前 Tab 命名空间字符串拼装，并支撑食物分类树。
- **Compose 设置行（`SettingRow`）**：可点击条目统一渲染——24dp 起 leading icon（`painterResource`，尺寸随 `titleLarge.fontSize` sp 动态缩放，跟随 `pref_font_scale` 偏好）、主标题 28sp 标题字号、功能说明副标题 12sp 脚注 + 中透明度（50%）、trailing `KeyboardArrowRight` 箭头区分可点击、动态值显示在箭头左侧、破坏性动作 `AlertDialog` 二次确认、文本走 `stringResource` 三套本地化。详见 `AGENTS.md`「Compose 设置行规范」。

## 项目结构（目标 Compose 化后，按模块分组）

```
app/src/main/java/com/woshiwangnima/healthdietpro/
├── MainActivity.kt                  # app shell（界面模块）
├── unit/                            # 单位切换
│   └── UnitConverter.kt
├── location/                        # GPS 定位
│   └── CurrentLocationProvider.kt
├── disease/                         # 疾病系统（UI）
├── profile/                         # 个人信息
│   ├── height/   weight/   bmi/
├── nutrition/                       # 营养表系统（待实现）
├── screens/                         # 界面模块（其余屏）
│   ├── person/   medication/   settings/
├── archive/                         # 存档模块
│   ├── app/   user/
├── data/                            # 静态数据只读
│   ├── unit/   disease/   region/   chart/   food/(未来)
├── common/                          # 基础设施
│   ├── ui/   tab/   tag/   chart/
```

> 上图为逻辑分组示意；现有 XML 实现的物理目录尚未按此调整，Compose 化迁移时逐步对齐。

## 功能路线

### 已完成（XML 形态，待 Compose 迁移）

- [x] **单位切换**：单位换算与本地化格式
- [x] **GPS 定位**：无 GMS 单次定位
- [x] **静态数据**：营养素/疾病/单位/地区/图表配置/BMI 分带 Repository
- [x] **个人信息**：身高/体重/BMI 历史图表与自研 Canvas 绘制
- [x] **界面模块**：个人资料编辑、地区级联选择、GPS 反查、多用户切换/创建/删除与级联清理、用药记录
- [x] **基础设施**：Tab 控件系统、图表 Canvas、公用组件

### 进行中（Compose + MVVM 化）

- [ ] 主题与导航迁至 Navigation Compose
- [ ] `ProfileFragment` → `ProfileScreen` + `ProfileViewModel`
- [ ] Tab 控件 Compose 化（保留三策略接入点）
- [ ] `ChartView` → Compose Canvas，保留 7 种线型与十字准线
- [ ] DataStore 取代 SharedPreferences（存档模块）
- [ ] `CurrentLocationProvider` 协程化

### 待实现

- [ ] **疾病系统** UI（疾病选择 + 患病率 + 营养建议渲染）
- [ ] **营养表系统**：食物分类树 + 营养表浏览 + 每日饮食记录 + 自定义食物 + 营养目标 + OCR/扫码
- [ ] **多层 Tag**：GameplayTag 式层级标签系统
- [ ] `MultiLevelTabCoordinator` 与 `RecordHistory` 在生产屏接入
- [ ] `ChartFullscreenData` Parcelable 化

## 构建指南

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 数据来源

疾病/营养建议、单位、地区质心与 BMI 中国标准均为本地 JSON 资源，离线可用。营养数据参考《中国食物成分表》（标准版）。

## 设计文档

详见 [`docs/DESIGN.md`](docs/DESIGN.md)，覆盖目标架构、9 模块职责、数据流与迁移路径。
