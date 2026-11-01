# HealthDietPro

健康饮食管理 App · 目标平台 Android 15+

## 项目定位

帮助用户管理日常饮食摄入，结合个人健康档案、地域性疾病流行度与营养建议，提供食材/食物/菜肴营养查询与对比、饮食与睡眠记录、物价记录、血糖/血压/体重等健康指标图表等核心功能。支持多用户切换、本地优先的数据持久化与无网络环境下的完整体验。

**全部数据留在本地**：无账号、无网络请求、无埋点。

## 目标平台

| 项目 | 值 |
|------|-----|
| minSdk | 35 (Android 15) |
| targetSdk | 36 (Android 16) |
| compileSdk | 36 |
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| 构建工具 | Gradle Kotlin DSL（Gradle 9.6） |
| 语言 | 简体中文 / English（三套 `values`，随系统切换） |

## 技术栈

- **语言**：Kotlin 2.2.10
- **UI**：Jetpack Compose + Material Design 3（已全面 Compose 化，无 XML 布局）
- **架构**：MVVM（ViewModel + StateFlow + 单向数据流）
- **导航**：单 Activity + 多 Composable 屏（`AnimatedPageContent` 横向切换）
- **持久化**：SharedPreferences（JSON 索引 + 多用户存档）
- **序列化**：Gson 与 kotlinx.serialization 并存（历史模块用 Gson，新模块用 kotlinx）
- **图表**：全自研 Compose Canvas，非第三方图表库

## 功能模块

App 主界面为三个底部页签：**食材** / **记录** / **我的**。

### 食材（营养）

| 功能 | 说明 |
|------|------|
| 食材 / 食物 / 菜肴查询 | 263 条食材记录，含分类树、搜索索引、营养表浏览 |
| 详情与对比 | 营养档案、系统标签（常见/收藏/最近）；两三条并排对比（VS 徽章） |
| 自定义条目 | 自建食材/菜肴，编辑成分与营养素 |
| 营养建议 | 按疾病营养规则（五级）标注营养素 |

### 记录

| 功能 | 说明 |
|------|------|
| 记血糖 | 记录与图表；**6 小时血糖预测**（叠加饮食/用药/睡眠修正线，运动线默认关闭），预测曲线与实际值隔离显示 |
| 记血压 | 收缩压 / 舒张压记录与图表 |
| 记用药 | 用药目录、用药记录，含位置水印元信息 |
| 记疾病 | 疾病与健康状况记录（ICD-11 映射） |
| 记饮水 | 每日饮水量记录 |
| 记饮食 | 饮食记录、餐次列表、摄入汇总与营养目标进度 |
| 记睡眠 | **夜间睡眠** 与 **小憩** 两类，入睡/醒来互推；统计页含时长分布与趋势 |
| 记物价 | 按商品 + 地区记录价格，多选地区筛选，历史价格曲线 |
| 记体重 / 身高 / 围度 | 体征记录（体重可标记"稳定体重"） |
| 容器量具 | 自定义容器与横截面形状，用于估算食物份量 |

### 我的

- 个人资料查看与编辑：头像、姓名、性别、年龄、生日、三级省/市/区（含 GPS 反查）、疾病史
- BMI：由体重 + 身高时间序列派生，图表 Y 轴按 BMI 类别着色，附中国标准对照卡与计算器
- 多用户切换 / 创建 / 删除（删除时级联清理头像、偏好与相关存档键）

### 设置

软件设置、用户设置、文字显示设置（字号缩放）、单位偏好、默认睡眠设置等。

## 项目结构

```
app/src/main/java/com/woshiwangnima/healthdietpro/
├── MainActivity.kt              # app shell：三页签 + 路由 + 返回栈
├── base/                        # Activity 基类（含未保存修改拦截 DirtyFormActivity）
├── ui/                          # 界面（Composable）
│   ├── nutrition/               # 食材 / 食物 / 菜肴
│   ├── record/                  # 血糖 / 血压 / 用药 / 疾病 / 饮水
│   ├── profile/                 # 身高 / 体重 / BMI / 围度 / 资料
│   ├── sleep/                   # 记睡眠（列表 / 统计 / 编辑 / 设置）
│   ├── price/                   # 记物价（列表 / 编辑 / 历史图表）
│   ├── diet/                    # 记饮食（记录 / 统计 / 设置）
│   ├── container/               # 容器量具与横截面
│   ├── settings/                # 设置
│   ├── widget/  event/  test/   # 公共挂件、事件、测试屏
├── model/                       # 领域模型与仓储
│   ├── food/  bloodglucose/  bloodpressure/  medication/  disease/
│   ├── diet/  container/  sleep/  price/  water/
│   ├── profile/  archive/  prefs/  region/  unit/  chart/  nutrient/  i18n/
├── common/                      # 基础设施
│   ├── ui/                      # 设计系统（AppTokens / AppSpacing / 公共组件）
│   ├── timer/  time/  cache/  range/  notification/  format/
├── util/                        # UnitConverter、定位、图片、系统导航模式
```

设计系统统一收敛在 `common/ui/designsystem/`（`AppTokens`、`AppSpacing`、
`AppSplitCard` 等），新增界面应复用其中组件而非各自写样式。

## 静态数据资产

无网络层，所有静态数据位于 `app/src/main/assets/`：

| 资源 | 内容 |
|------|------|
| `units.json` | **9 类 78 个单位**（长度/质量/体积/密度/时间/能量/血糖/血压/存储），含 ft/in 等本地化符号 |
| `diseases.json` | **24 条**疾病与健康状况；10 个分类、23 个就诊科室；条目含中英文正名/别名/医学简介、ICD-11 映射、病程、适用性与五级营养规则 |
| `food_catalog/` | **263 条**食材记录 + 4 个索引 + 缩略图/详情图；维护源在 `tools/food_catalog/source/records/`，构建期由 `catalog_tool.py` 生成 |
| `location/` | 34 省简化边界矩形 + 质心、市/区质心（`provinces.json` / `regions.json`） |
| `DRIs/` | 营养素参考摄入 |
| `serving_containers.json`、`cooking_methods.json` | 份量容器与烹饪方式 |

Repository 模式：按需懒加载、内存缓存，查找走预建索引。统一构造入口
`fromContext(context)`（运行时）与 `fromAsset(path)`（测试/离线）。**只读**，所有写操作走存档模块。

区域解析采用混合策略：省 = 简化矩形射线投射 + 质心抢七；市/区 = 分层最近质心。
以 <1 MB 数据换 ~1–3 km 边界误差。

## 存档与多用户持久化

存档分 **app 存档** 与 **user 存档**：

- **多用户**：所有 `UserProfile` JSON 序列化在 `health_diet_prefs/all_users`；当前用户 ID 在 `current_user_id`
- **体征记录**：`BodyRecord` 始终以基准单位存储（cm / kg）；UI 边界转换为用户偏好单位（保存 `toBase`，展示 `fromBase`）
- **种子数据**：无记录的用户合并 `createSeedProfile`，约 30 天体重 + 6 个月身高示例点，保证新账号也有图表数据
- **每用户键**：图表 UI 状态、`UserPrefs`、`MedicationPrefs` 均以 `<baseKey>_<userId>` 命名空间隔离
- **删除级联**：清理头像文件、`user_prefs_<uid>`，以及 `health_diet_prefs` / `app_prefs` 中以 `_<uid>` 结尾的所有键
- **迁移**：`ProfilePrefs.ensureMigrated` 每次加载时桥接旧形态到 `RegionSnapshot`

## 构建指南

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 单元测试（基线：356 项，350 通过；6 项 FoodNutrientRepositoryTest 为历史失败）
./gradlew testDebugUnitTest
```

> 本机需指定 JDK：`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"`
> 食材目录与图片是构建期生成物（依赖 `uv`），首次构建会自动调用 `tools/food_catalog/catalog_tool.py`。

### 打 release 包

完整流程（版本号位置、签名密钥位置与备份要求、签名校验命令、已知坑）见
[`docs/RELEASE_BUILD.md`](docs/RELEASE_BUILD.md)。

产物：

- `app/build/outputs/apk/release/app-release.apk` —— 可直接安装分发
- `app/build/outputs/bundle/release/app-release.aab` —— Google Play 上架

## 数据来源

疾病/营养建议、单位、地区质心与 BMI 中国标准均为本地 JSON 资源，离线可用。
营养数据参考《中国食物成分表》（标准版）。ICD-11 映射以 WHO MMS `2025-01` 为固定版本，
仅作分类与检索参考，不用于诊断。

## 设计文档

| 文档 | 内容 |
|------|------|
| [`docs/DESIGN.md`](docs/DESIGN.md) | 目标架构、模块职责、数据流与迁移路径 |
| [`docs/UI_SPEC_PROJECT.md`](docs/UI_SPEC_PROJECT.md) | UI 规范：设计令牌、组件用法、图标画法 |
| [`docs/RELEASE_BUILD.md`](docs/RELEASE_BUILD.md) | 构建与发布流程 |
| [`docs/PROJECT_CLEANUP.md`](docs/PROJECT_CLEANUP.md) | 文件梳理：未使用资源清单与待更新项 |
| [`docs/FOOD_CATALOG_ASSET_DESIGN.md`](docs/FOOD_CATALOG_ASSET_DESIGN.md) | 食材资产生成与维护 |
