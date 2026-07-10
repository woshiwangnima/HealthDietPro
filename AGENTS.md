# AGENTS.md

HealthDietPro agent 规范。目标架构：Kotlin + Jetpack Compose + MVVM + 9 模块。完整设计见 `README.md` 与 `docs/DESIGN.md`。本文件只列硬约束与验证命令。

## 技术栈

- Kotlin 2.x · Jetpack Compose · Material 3 · Navigation Compose · kotlinx.serialization · Coroutines + Flow
- 依赖注入：手动构造（后续可接入 Hilt）

**禁用清单（新代码与迁移代码一律遵守）：**
- 不引入 Gson（迁移既有 Repository 时改 `kotlinx.serialization`）
- 不新增 Fragment / XML 布局用于新屏（新屏必须 Compose）
- 不引入第三方图表库（图表走自研 Canvas）
- 不在静态数据模块写入

## 模块依赖规则

9 模块定义见 DESIGN §3。允许的依赖方向：

| 模块 | 可依赖 |
|------|--------|
| 静态数据只读 | 无（纯只读，不持写入口） |
| 存档（app / user） | 静态数据 schema |
| 基础设施（UI 公用 / 多层 Tag / 图表渲染） | 静态数据（图表轴格式）、存档（`TabPersistence` / `AppPrefs` 持久化） |
| 功能模块（单位切换 / GPS / 疾病 / 个人信息 / 营养表） | 静态数据 + 存档 + 基础设施 |
| 界面模块 | 功能模块 + 基础设施 + 存档 |

**禁止：** 循环依赖；UI 直接读 `assets/*.json`（须经静态数据 Repository）；功能模块反向依赖界面模块；静态数据模块依赖任何其他模块。

## 架构公约（MVVM + 单向数据流）

- 单 Activity + Composable 屏（Navigation Compose）。
- `ViewModel` 暴露 `StateFlow<UiState>`；事件自顶向下，状态自底向上。UI 不直接写存档。
- Repository 统一入口 `fromContext(context)` / `fromAsset(path)`，懒加载 + 内存缓存 + 预建索引。
- 数据类不可变（`data class` + `val`）；可变状态用 `MutableStateFlow` / `mutableStateOf`，不公开可变字段。
- 协程：asset / prefs 读写走 `Dispatchers.IO`，UI 走 `Dispatchers.Main`；Repository 协程化后用 `suspendCancellableCoroutine`，超时返回 `Result.Err`。
- 构造函数注入；除既有 `UnitConverter` 单例外，新代码不引入 `object` 单例。

## 关键不变量（不可违反）

- `BodyRecord.value` 始终基准单位（cm / kg）；UI 边界经 `UnitConverter.toBase / fromBase` 换算。`unit` 字段仅为 UI 提示，不可信为值的单位。
- `ProfilePrefs.save()` 是唯写入入口；新用户铸造 id 后自动 `setCurrentUserId`。
- 删除用户必须级联清理：头像、`user_prefs_<uid>`、所有 `_<uid>` 后缀键。
- 静态数据模块只读；所有写操作走存档模块。
- 每用户隔离状态经 `ProfilePrefs.makeChartStateKey(baseKey)` 构造 `_<userId>` 后缀。
- 空 `chartStateKey` 不持久化（一次性图表）。

## 编码规范

- 包结构按 9 模块分组（见 README 项目结构树）。
- 命名后缀：`*Screen`（Composable 屏）、`*ViewModel`、`*Repository`、`*UiState`。
- `@Composable` 函数 PascalCase，状态提升到调用方。
- 公开 API 优先 `internal`，仅跨模块必需时 `public`。
- 不加注释除非必要。

## Compose 设置行 / 可点击条目规范

迁移到 Compose 的设置类屏（`AppSettingsScreen` / `PreferencesScreen` / 后续所有列表式可点击条目）必须遵守：

- **左侧 leading icon**：每个可点击条目必须有 leading icon（`painterResource`），保留原 XML 屏的图标资源；新增条目也必须配图标。**图标尺寸动态跟随字号**：用 `MaterialTheme.typography.titleLarge.fontSize`（sp）经 `LocalDensity.toDp()` 转换声明 `Modifier.size`，使图标随 `pref_font_scale` 偏好与行高同比例缩放，禁止固定 `dp`。
- **可点击视觉区分**：可点击条目（跳转下一界 / 弹对话框 / 触发动作）右侧必须显示 `KeyboardArrowRight` trailing 箭头，颜色 `onSurfaceVariant`，与纯展示行（`clickable = false`，无箭头）区分。
- **不可点击纯展示行**：`clickable = false`，无 trailing 箭头，无 ripple；用于只读信息呈现。
- **trailing value**：有动态值（如缓存大小、当前单位）时，值显示在箭头左侧（`bodyMedium` + `onSurfaceVariant`），与箭头间距 4dp。
- **字号**：主标题 `titleLarge`（28sp 标题字号），功能说明副标题用脚注 12sp（`FontTokens.caption`）+ `AppPrefs.getFontStyleTokenAlphaMid` 中透明度。
- **本地化**：所有文本必须走 `stringResource` + `values/` `values-en/` `values-zh/` 三套，禁止 Composable 内硬编码中文字面量。
- **防呆确认**：破坏性 / 不可逆动作（清除缓存、删除用户等）点击后必须先弹 `AlertDialog` 二次确认，禁止直接执行。
- **复用**：通过 `common/ui/SettingRow.kt` 的 `SettingRow` Composable 统一渲染，新增条目优先复用而非另造。

## 测试要求

- 纯算法（`BmiUtil` / `ChartMath` / `PointInPolygon`）与 Repository（`fromAsset` 入口）必须有 JVM 单测，无 Android 依赖。
- 新增纯逻辑（算法 / Repository 查询）必须配单测。
- 验证不变量：`BodyRecord` 基准单位、`deleteUser` 级联完整性、`UnitConverter` 双向换算闭环。

## 验证命令

```bash
./gradlew assembleDebug      # 构建
./gradlew test               # JVM 单测
./gradlew lint               # Android lint
./gradlew installDebug       # 安装
```

Windows / OpenCode 环境若 `JAVA_HOME` 未设置，使用 Android Studio 自带 JBR，仅在当前命令进程内设置环境变量，不修改系统环境：

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat assembleDebug
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat test
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat lint
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat installDebug
```

完成任何改动后必须跑 `test` + `lint`；无法确定命令时问用户并写回本文件。

## Compose 迁移规则

- 不动 model 层与 Repository（`model/**`）。
- 逐屏迁移，保留旧 Fragment 一段时间做对照，再删。
- 新屏必须 Compose；新逻辑优先纯 Kotlin（无 Android 依赖）便于 JVM 测试。
- 迁移路径见 DESIGN §10。

## 工作流

- 不主动 commit / push（除非明确要求）。
- 改动前查 `README.md` 与 `docs/DESIGN.md` 对应模块节。
- 不改 `assets/*.json` 与 `res/raw/*` 除非明确要求。
- 现状：XML + DataBinding 单 Activity 多 Fragment，Compose 未启用；本规范描述目标态，迁移按 DESIGN §10 推进。
