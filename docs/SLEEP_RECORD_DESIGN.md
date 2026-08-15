# 记睡眠功能设计文档（初步）

> 本文档定义「记睡眠」（Sleep Recording）功能的初步设计，参考既有记录模块（饮水、用药、记饮食）的公共规范：单 Activity + Compose + MVVM、Repository 统一入口、用户级存档与三段式 `archiveSchemaVersion` 迁移链。文档为**初步**版本，字段与交互在实现过程中可与产品迭代调整。

---

## 1. 功能概述

「记睡眠」允许用户记录一段睡眠（夜间睡眠或小憩），包含入睡时间、醒来时间与记录时间，可附加备注。记录时可选启动计时器（入睡倒计时/小憩提醒）。

入口：`记记录` 页 → `记睡眠` 卡片（`RecordActionId.Sleep`，当前为空操作，实现后启用）。

首次范围：

- 记录 `夜间睡眠`（NIGHT_SLEEP）与 `小憩`（NAP）两类睡眠记录。
- 每条记录含：入睡时间、醒来时间（睡眠进行中可空）、记录时间、备注。
- 记录创建时可选择**启动计时器**（独立计时器模块），计时结束通过通知提醒；计时器可选择联动系统闹钟/计时器。
- 睡眠列表：按时间倒序，展示类型、时长、入睡/醒来时间与备注。

暂不做（后续迭代）：

- 自动睡眠检测（传感器/穿戴设备同步）。
- 睡眠质量评分、深睡/浅睡阶段分析、晨间综合报告。
- 数据导出到第三方睡眠健康平台。

---

## 2. 模块依赖

```
界面模块 (ui/sleep/)
  ├── 功能模块 (model/sleep/)          ← 睡眠领域模型（纯 Kotlin，可 JVM 测试）
  ├── 存档模块 (model/archive 存档基元 + 用户级存档键)
  ├── 基础设施 (common/timer/)         ← 计时器独立模块
  ├── 基础设施 (common/ui/)            ← BaseScreen、SettingRow、EditorScaffold、时间选择器
  └── 存档模块 (model/prefs ProfilePrefs.makeChartStateKey 构造每用户命名空间)
```

依赖方向遵守 AGENTS.md 模块规则：功能模块不得反向依赖界面模块；计时器模块属基础设施，供睡眠与菜肴等跨功能复用，不得反向依赖任一功能模块。

---

## 3. 数据模型

### 3.1 睡眠类型

```kotlin
// model/sleep/SleepKind.kt
@Serializable
internal enum class SleepKind {
    NIGHT_SLEEP,   // 夜间睡眠
    NAP,           // 小憩
}
```

### 3.2 睡眠记录

```kotlin
// model/sleep/SleepRecord.kt
@Serializable
internal data class SleepRecord(
    val id: String,                  // UUID
    val kind: SleepKind,             // 夜间睡眠 / 小憩
    val sleepStartAt: Long,          // 入睡时间 (epoch millis)
    val wakeUpAt: Long?,             // 醒来时间；睡眠进行中为 null
    val recordedAt: Long,            // 记录时间 (epoch millis)
    val note: String = "",           // 备注（可选）
    val timerId: String? = null,     // 关联的计时器实例 id（可选，见 §4）
)
```

约束与派生：

- `sleepStartAt` 必填；`wakeUpAt` 存在时必须 `>= sleepStartAt`。
- `recordedAt` 必填，独立于入睡/醒来时间（补记场景下可与入睡时间不同）。
- 时长 `durationMinutes` 为派生值：`wakeUpAt?.let { (it - sleepStartAt) / 60000 }`；睡眠进行中显示「进行中」。
- 数据类不可变（`data class` + `val`），可变状态只在 ViewModel `StateFlow<UiState>` 内。

### 3.3 用户级存档

```kotlin
// model/sleep/SleepArchive.kt
@Serializable
internal data class SleepArchive(
    val schemaVersion: ArchiveSchemaVersion = ArchiveSchemaVersion.Current,
    val records: List<SleepRecord> = emptyList(),
)
```

- 存档键：`sleep_records_v1`，经 `ProfilePrefs.makeChartStateKey(baseKey)` 构造 `_<userId>` 后缀，实现每用户隔离；删除用户时随 `user_prefs_<uid>` 一并清理。
- 三段式 `archiveSchemaVersion`（major/minor/patch）与当前安装包版本经 `model/archive/ArchiveVersion.kt` 读取；读写前执行幂等迁移链，迁移成功立即持久化（对齐 AGENTS.md 存档公约）。
- 纯校验 `validateSleepArchive`（JVM 可测）：id 非空且唯一、`sleepStartAt > 0`、`wakeUpAt == null || wakeUpAt >= sleepStartAt`、`recordedAt > 0`、时长上限（如单条不超过 48h，防脏数据）。
- 实现遵循既有 Repository 模式：`SleepRepository.fromContext(context)` 懒加载 + 内存缓存 + 协程 IO 读写，超时返回 `Result.Err`。

---

## 4. 计时器独立模块（基础设施）

计时器作为**独立基础设施模块**（`common/timer/`）提供，不绑定睡眠功能，供睡眠、菜肴制作步骤（`recipeSteps[].minutes`）等跨功能复用。

### 4.1 模块职责

- 倒计时/正计时实例的创建、启动、暂停、清空与完成回调。
- 可选联动：启动系统闹钟（`AlarmClock.ACTION_SET_ALARM`）或系统计时器（`AlarmClock.ACTION_SET_TIMER`）——借助系统时钟应用能力，不自行常驻前台服务；当系统不提供对应组件时回退为应用内通知提醒。
- 计时状态用户级持久化（进程被杀后恢复），支持多实例并发（睡眠计时与小憩计时互不干扰）。
- 触发系统闹钟/计时器属于**系统副作用**，必须显式声明且二次确认，禁止静默启动。

### 4.2 领域模型（纯 Kotlin）

```kotlin
// common/timer/TimerInstance.kt
@Serializable
internal data class TimerInstance(
    val id: String,                  // UUID
    val label: String,               // 用途标签（如「小憩提醒」「菜肴步骤 2」）
    val totalMinutes: Int,           // 目标时长
    val startedAtMillis: Long?,      // 最近一次启动时间
    val remainingSeconds: Long,      // 暂停/恢复用剩余秒数
    val state: TimerState,           // IDLE / RUNNING / PAUSED / FINISHED
    val notifyViaSystem: Boolean,    // 是否联动系统闹钟/计时器
)

@Serializable
internal enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }
```

- 控制器抽象 `TimerController`（接口，界面模块经 ViewModel 事件调用，不直接持有 Android 依赖的脏逻辑）；实现方持有 `AlarmManager`/通知权限引用。
- 回调：`onTick(remainingSeconds)`、`onFinished(timerId)`；UI 只订阅 `StateFlow<TimerUiState>`。

### 4.3 与睡眠记录的关系

- 记录创建时若选择「启动计时器」，睡眠 ViewModel 经 `TimerController.create(...)` 创建实例并把 `timerId` 写入 `SleepRecord`。
- 计时结束（如 20 分钟小憩提醒）：计时器模块发通知；睡眠列表对关联记录显示「计时已结束」标记；用户可一键补录醒来时间（`wakeUpAt = now`）。
- 删除睡眠记录不删除关联计时器实例（计时器独立生命周期）；但记录删除后可手动清理孤儿计时器。

---

## 5. 界面与交互

单 Activity + Compose 屏（Navigation Compose 或路由枚举，对齐 `WaterRecordActivity`/`ContainerRecordActivity` 模式）：

### 5.1 睡眠列表页 `SleepListScreen`

- 入口：`RecordActionId.Sleep` → `SleepRecordActivity`。
- 列表倒序展示：类型标签（夜间睡眠/小憩）、时长（或「进行中」）、入睡→醒来时间、备注。
- 「进行中」记录卡片置顶高亮，提供「醒来」快捷操作；行点击进入详情/编辑。
- 筛选：类型（夜间/小憩）、时间范围（复用既有时间范围选择组件）。

### 5.2 记录/编辑页 `SleepEditorScreen`

- 类型选择：`夜间睡眠` / `小憩`（SegmentedButton）。
- 入睡时间：默认当前时间，可经时间选择器修改。
- 醒来时间：可空；为空表示「睡眠进行中」。
- 记录时间：默认当前时间，可修改（支持补记）。
- 备注：可选多行输入。
- **启动计时器**：可选开关；开启后选时长（或直接联动系统计时器），二次确认系统闹钟/计时器联动。
- 保存/取消走 `EditorScaffold` 固定底部栏 + `BackHandler` 未保存二次确认；系统返回逐级回退。

### 5.3 本地化

所有文案走 `values/`、`values-en/`、`values-zh/` 三套资源，禁止硬编码。

---

## 6. 存档与迁移

- 新增或修改字段必须增加下一版本迁移节点、兼容旧字段默认值，并补充导入前校验与 JVM 单测（对齐 AGENTS.md）。
- 首版字段未来可能的迁移示例：在记录上增加 `quality`/`environment` 可选字段 → v0.0.2 迁移节点默认补空并持久化。
- 敏感存档导入导出复用 `model/archive/SensitiveArchiveCodec`（GZIP + PBKDF2 + AES-GCM），导入前完成解密、解压、版本校验与迁移。

---

## 7. 复用与扩展

- **菜肴制作步骤**（`Dish.recipeSteps[].minutes`）：后续实现时直接复用 `TimerController`，同一实例管理「启动/暂停/清空」步骤计时，无需为菜肴另建计时逻辑。
- 计时器模块为唯一计时实现来源，禁止睡眠与菜肴各自重复造轮子。

---

## 8. 实现路径

1. `model/sleep/`：`SleepKind`、`SleepRecord`、`SleepArchive`、纯校验与迁移（JVM 单测）。
2. `common/timer/`：`TimerInstance`、`TimerState`、`TimerController` 抽象与实现（含系统闹钟/计时器联动、持久化）。
3. `model/sleep/SleepRepository.kt`：`fromContext` + 懒加载 + 用户级存档读写。
4. `ui/sleep/SleepViewModel.kt`：`StateFlow<SleepUiState>`（列表、编辑表单、计时器事件）。
5. `ui/sleep/SleepListScreen.kt` / `SleepEditorScreen.kt` / `SleepRecordActivity.kt`，接入 `RecordActionId.Sleep`。
6. 三套字符串资源；删除用户级联清理 `sleep_records_v1`。

## 9. 测试边界

- JVM：`SleepArchive` 校验/迁移、时长派生、计时器状态机（纯 Kotlin）。
- 不依赖 `android.*` 的计时器逻辑全部走 JVM 测试；系统闹钟/计时器联动只在设备测试中验证。