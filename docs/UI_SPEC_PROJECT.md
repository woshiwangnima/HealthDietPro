# HealthDietPro 项目 UI 规范

> 本文档是《通用UI设计规范》（`v0.4`）的项目落地册，按通用规范 §1「项目落地时另立项目规范，通过**取值映射 + 场景映射**引用本文档」编写。
>
> 规则来源分三类，冲突时优先级从高到低：
>
> 1. `AGENTS.md` 的硬约束与关键不变量
> 2. 本文档的取值映射与偏离记录
> 3. 《通用UI设计规范》原文
>
> 代码实现位于 `app/src/main/java/com/woshiwangnima/healthdietpro/common/ui/designsystem/`。

---

## 1. 适用范围

| 项目 | 内容 |
| --- | --- |
| 目标平台 | Android 手机（竖屏优先），`minSdk 35` / `targetSdk 36` |
| 不覆盖 | PC 键鼠、手柄、TV 10-foot、跨端一致性（通用规范 §10 仅保留断点与等价原则） |
| 设计基准 | 移动端竖屏 `1080×2340`，非 `1920×1080`（通用规范 §2.1 的移动竖屏行） |
| 视觉基调 | 现代深色 + 精细令牌；**不采用像素风** |
| 主题 | 深色 / 浅色双套，跟随 `AppPrefs.darkMode`，两套均需满足 §11.1 对比度 |

---

## 2. 取值映射

### 2.1 颜色

来源：通用规范 §2.3。代码：`AppTokens.kt` 的 `DarkAppColors` / `LightAppColors`。

**表 2.1-1 深色主题（默认，整体黑色）**

| 令牌 | 取值 | 实测对比度 | 要求 | 结论 |
| --- | --- | --- | --- | --- |
| `color.bg` | `#000000` | — | 不用纯黑（本项目经批准改为纯黑，见 D-9） | ⚠️ D-9 |
| `color.panel` | `#0D0D0D` | text/panel 16.60:1 | ≥ 4.5:1 | ✅ |
| `color.panelRaised` | `#1A1A1A` | text/panelRaised 14.87:1 | ≥ 4.5:1 | ✅ |
| `color.border` | `#2B2B2B` | border/panel 1.37:1 | 装饰用，不承载语义 | ⚠️ D-2 |
| `color.borderStrong` | `#616161` | borderStrong/bg 3.39:1 | ≥ 3:1 | ✅ |
| `color.text` | `#EDEDED` | text/bg 17.94:1 | ≥ 7:1 | ✅ |
| `color.textMuted` | `#A0A0A0` | textMuted/bg 8.03:1 | ≥ 4.5:1 | ✅ |
| `color.disabled` | `#5A5A5A` | — | 不作正文色 | ✅ |
| `color.primary` | `#7FA6F0` | primary/bg 8.62:1 | ≥ 4.5:1 | ✅ D-10 |
| `color.onPrimary` | `#000000` | onPrimary/primary 8.62:1 | ≥ 4.5:1 | ✅ |
| `color.success` | `#3FBF6F` | success/bg 8.89:1 | ≥ 4.5:1 | ✅ |
| `color.warning` | `#F2B23E` | warning/bg 11.21:1 | ≥ 4.5:1 | ✅ |
| `color.danger` | `#E5484D` | danger/bg 5.37:1 | ≥ 4.5:1 | ✅ |
| `color.dangerFill` | `#C62828` | onDangerFill/… 5.62:1 | ≥ 4.5:1 | ✅ |
| `color.info` | `#C084FC` | info/bg 7.95:1 | ≥ 4.5:1 | ✅ D-10 |
| `color.scrim` | `#000000` 60% | — | 通用规范 §2.8 | ✅ |

层级只靠中性灰阶三级明度差表达：`#000000`（底）→ `#0D0D0D`（面板）→ `#1A1A1A`（浮起 / 表头），不再使用蓝色调底色。

**表 2.1-2 浅色主题**

| 令牌 | 取值 | 实测对比度 | 结论 |
| --- | --- | --- | --- |
| `color.bg` | `#F4F6FB` | — | ✅ |
| `color.panel` / `panelRaised` | `#FFFFFF` | text/panel 17.87:1 | ✅ |
| `color.border` | `#D3DAE8` | border/bg 1.30:1 | 仅装饰 |
| `color.borderStrong` | `#7A8398` | borderStrong/bg 3.51:1 | ✅ |
| `color.text` | `#0F1729` | text/bg 16.53:1 | ✅ |
| `color.textMuted` | `#56607A` | textMuted/bg 5.80:1 | ✅ |
| `color.primary` | `#1F5ED0` | primary/bg 5.44:1 | ✅ |
| `color.success` | `#1B7A45` | success/bg 4.96:1 | ✅ |
| `color.warning` | `#8A5A00` | warning/bg 5.48:1 | ✅ |
| `color.danger` | `#B3261E` | danger/bg 6.05:1 | ✅ |
| `color.info` | `#7B2FBE` | info/bg 6.49:1 | ✅ D-10 |

**表 2.1-3 食物分类色（`FoodKind`）**

`AGENTS.md` 规定三色固定，本文档不擅自更改；但**现有值配白字不满足 §11.1 的 4.5:1**，见偏离 D-3。

| 分类 | 现有值 | 白字对比度 | 建议值 | 白字对比度 |
| --- | --- | --- | --- | --- |
| 食材 `INGREDIENT` | `#43A047` | 3.30:1 ❌ | `#2E7D32` | 5.13:1 ✅ |
| 食物 `FOOD` | `#F57C00` | 2.70:1 ❌ | `#BF360C` | 5.60:1 ✅ |
| 菜肴 `DISH` | `#E53935` | 4.23:1 ❌ | `#C62828` | 5.62:1 ✅ |

代码中两套并存：`AppFoodKindCurrent`（当前生效）与 `AppFoodKindAccessible`（预览对照），待 D-3 决策后切换。

### 2.2 间距与尺寸

来源：通用规范 §2.2、§10.4（移动端列）。

| 令牌 | 值 | 用途 |
| --- | --- | --- |
| `space.1` | 4 dp | 图标与文字、描边内边距 |
| `space.2` | 8 dp | 同组控件间距、标签内边距 |
| `space.3` | 12 dp | 列表行内间距 |
| `space.4` | 16 dp | 卡片内边距、栅格列间距 |
| `space.5` | 24 dp | 浮层内容边距、区块间距 |
| `space.6` | 32 dp | 大区块间距、面板标题区 |
| `space.7` | 48 dp | 页面级分区 |
| `space.8` | 64 dp | 大屏分区（手机侧边距用 `page`） |
| `page` | **16 dp** | 页面左右边距（通用规范 §10.4 移动端列，**非** §2.2 的 64） |
| `rowMinHeight` | **56 dp** | 列表行高（§10.4 移动端列） |
| `hitMin` | **48 dp** | 最小点击热区（§10.4 移动端列） |

规则：
- 所有间距取上表，禁止 5 / 7 / 13 dp 等随手值。
- 同一组内间距必须小于组间间距。

### 2.3 字号

来源：通用规范 §2.4。代码：`AppTypeDefault`，经 `toMaterialTypography()` 映射到 Material 3。

| 令牌 | 字号 / 行高 | Material 3 映射 |
| --- | --- | --- |
| `font.caption` | 12 / 16 sp | `labelSmall`、`bodySmall` 之外的 `labelSmall` |
| `font.bodyS` | 14 / 20 sp | `bodySmall`、`labelLarge`、`labelMedium` |
| `font.body` | 16 / 24 sp | `bodyLarge`、`bodyMedium` |
| `numeric`（等宽） | 16 / 24 sp | 数值专用，等宽数字 |
| `font.titleS` | 20 / 28 sp | `titleMedium`、`titleSmall` |
| `font.title` | 24 / 32 sp | `titleLarge`、`headlineSmall` |
| `font.titleL` | 32 / 40 sp | `headlineLarge`、`headlineMedium`、`displaySmall` |
| `font.display` | 40 / 48 sp | `displayLarge`、`displayMedium` |

与旧 `FontTokens` 的对照：

| 旧令牌 | 旧值 | 新令牌 | 新值 |
| --- | --- | --- | --- |
| `micro` | 11 sp | `font.caption` | 12 sp |
| `caption` | 12 sp | `font.caption` | 12 sp |
| `label` | 14 sp | `font.bodyS` | 14 sp |
| `body` | 16 sp | `font.body` | 16 sp |
| `subtitle` | 22 sp | `font.titleS` | 20 sp |
| `title` | 28 sp | `font.title` | 24 sp |
| `headline` | 36 sp | `font.titleL` | 32 sp |

规则：
- 单屏字号层级不超过 4 级（`AGENTS.md`），需要强调时先用颜色与字重。
- 数值、血糖、营养素一律走 `numeric` 等宽样式，避免位数变化导致抖动（§2.4、§4.3）。
- 字号缩放走 `AppFontScaleState`（100% / 125% / 150%），布局随字号重排，不截断。

### 2.4 动效

来源：通用规范 §2.9。代码：`AppMotionDefault` + `AppEaseLinear/Out/In/InOut`。

| 令牌 | 时长 | 缓动 | 本项目用途 |
| --- | --- | --- | --- |
| `motion.instant` | 80 ms | linear | 按压、颜色切换、开关 |
| `motion.fast` | 120 ms | ease-out | 轻提示进入、图标反馈 |
| `motion.exit` | 150 ms | ease-in | 收起、退出、下滑关闭 |
| `motion.base` | 180 ms | ease-out | 页面进入、展开、抽屉、气泡 |
| `motion.sheet` | 220 ms | ease-out | 底部动作条升起 |
| `motion.slow` | 280 ms | ease-in-out | 图表首次绘制、模态进入 |
| `motion.emphasis` | 400 ms | ease-in-out | 强调转场 |
| `motion.cinematic` | 800 ms | ease-in-out | 仅演出类，必须可跳过 |

与 `AGENTS.md` 的差异：

| 项 | `AGENTS.md` 现值 | 本文档取值 | 说明 |
| --- | --- | --- | --- |
| 主页横向进入 | 180 ms | 180 ms（`motion.base`） | 一致 |
| 主页横向退出 | **140 ms** | **150 ms**（`motion.exit`） | ⚠️ D-5，需同步 `PageMotion.kt` |
| 缓动 | `FastOutSlowInEasing` | 进入 `ease-out`、退出 `ease-in` | ⚠️ D-5 |
| 上限 | 无 | 界面动画 ≤ 400 ms | 新增约束 |

规则：
- 减少动态效果开启时：位移/缩放降为淡入淡出，时长减半，最低 80 ms；功能不得丢失。
- 同一屏同时播放的动画不超过 3 组。
- 循环动画只用于「等待」与「可交互提示」。

### 2.5 圆角、描边与阴影

来源：通用规范 §2.6、§2.7；抽屉顶角取移动规范 05 的浮层语义。代码：`AppRadiiDefault`。

| 令牌 | 值 | 适用 |
| --- | --- | --- |
| `radii.container` | 4 dp 硬边 | 面板、卡片、按钮、输入框 |
| `radii.label` | 8 dp | **仅**语义标签（`FoodKind` 色块、状态标签）——见 D-1 |
| `radii.sheet` | 12 dp（仅上两角） | 底部抽屉，比卡片大一档以区分浮层层级 |
| `radii.pill` | 999 dp | 计数角标、筛选 chip |
| `radii.stroke` | 2 dp | 面板与控件描边 |
| `radii.highlight` | 1 dp | 上/左内高光，透明度 20%～30% |

同心圆角：`R_outer = R_inner + Padding`（移动规范 07 检查项 11），工程上用 `concentricRadius(outer, inset)`。
4 dp 卡片配 12 dp 内边距时内层取 0，避免内圆比外圆还弯。

阴影：
- 常态 1～2 dp 硬阴影；浮层柔影 ≤ 8 dp。
- 光源方向统一左上 135°，禁止各控件自行打光。
- 发光只用于焦点与选中，同屏不超过 1 处主发光。

### 2.6 层级

来源：通用规范 §2.8。代码：`AppTokens.kt` 中的 `APP_Z_*` 常量。

| 常量 | z | 内容 |
| --- | --- | --- |
| `APP_Z_SCREEN` | 100 | 主内容区 |
| `APP_Z_HUD` | 200 | 常驻信息条 |
| `APP_Z_PANEL` | 300 | 列表、详情、设置 |
| `APP_Z_OVERLAY` | 400 | 模态、抽屉、气泡、底部动作条 |
| `APP_Z_TOAST` | 500 | 轻提示、横幅（不阻断，默认在遮罩之上） |

规则：
- 同一时刻只允许一个模态层，禁止模态套模态；二级确认先关当前浮层再开确认框。
- 轻提示在模态打开时改为浮层内提示，避免落在遮罩后。
- 层级值集中在此，禁止在 Composable 内写魔法数字。

---

## 3. 场景映射

### 3.1 断点

| 断点 | 宽度 | 典型设备 | 布局 |
| --- | --- | --- | --- |
| `compact` | < 600 dp | 手机竖屏 | 单栏，底部操作（**默认**） |
| `medium` | 600～1023 dp | 手机横屏、小平板 | 双栏，侧栏可收起 |
| `expanded` | ≥ 1024 dp | 平板、折叠屏展开 | 三栏，常驻侧栏 |

### 3.2 浮层

| 场景 | 采用类型 | 依据 |
| --- | --- | --- |
| 破坏性/不可逆确认 | 确认对话框（Alert Dialog），默认焦点在安全选项 | §5.5.2 |
| 长图文说明（只读） | **底部抽屉 80%（`AppBottomSheet`）**，禁止居中弹窗承载 | 移动规范 05 §一、§二 |
| 表单录入 | 底部抽屉 60%，`dismissOnScrimClick = false`，只允许显式取消 | 移动规范 05 §二、07 踩坑九 |
| 记录编辑 | 全屏页（`EditorScaffold` + `BackHandler` 未保存确认） | §10.3 |
| 次要操作集合 | 底部动作条，≤ 5 个动作 | §5.5.4、§10.3 |
| 字段说明 | 行内 `InlineTooltip` 单击展开 | §5.5.5、`AGENTS.md` |
| 结果反馈 | 轻提示（Toast），1.5～2.5 s，不带按钮 | §5.5.6；移动规范 05 §一 |
| 筛选 | 侧栏（`compact` 下为抽屉） | §7.2 |

### 3.3 导航

- 一级目的地：底部页签（`AppBottomNavigationBar`），`compact` 固定。
- 二级页面：全屏页 + 顶部返回 + `BackHandler`，逐层回退，禁止直接退到根页或退出 Activity。
- 详情：手机全屏页（不是右侧抽屉）。
- 菜单深度 ≤ 3 层。

### 3.4 图表

来源：通用规范 §9.4。本项目在 `common/ui/chart/` 自研 Canvas 图表，需满足：
- 系列不能只靠颜色区分，必须配合线型 / 点形状 / 直接标注。
- 关键数值直接标注；悬停（触屏为点按）显示浮层。
- 空数据、部分缺失、异常值分别有样式，**缺失不得画成 0**。
- 首次绘制 280 ms（`motion.slow`）内完成，不循环。
- 提供数据表视图（`AppDataTable`）。

---

## 4. 偏离记录

对通用规范说「不」的地方必须在此登记，不得在实现中悄悄偏离。

**D-1 圆角双轨**
- 通用规范 §2.6 要求「圆角最大 4 px，直角为主」。
- `AGENTS.md` 要求 `FoodKind` 色块「用圆角背景包裹正名」。
- 处理：容器/面板/按钮走 4 dp 硬边；语义标签（`FoodKind` 色块、状态标签、筛选 chip）走 8 dp / pill。理由：标签需与不规则食物图并置，硬边会与缩略图产生冲突。

**D-2 描边对比度**
- 通用规范 §2.3-2 规定 `color.border = #2E3A57` 且「与背景 ≥ 3:1」，但该取值实测仅 **1.68:1**，规范自身不一致。
- 处理：拆为两个令牌——
  - `color.border`（`#2E3A57`）仅用于**装饰性**分隔线与面板描边，不承载「必须可见」的语义；
  - `color.borderStrong`（`#55638A`）用于**可交互控件描边、焦点环、输入框边界**，实测 3.19:1，满足 §11.1 的非文本 3:1。

**D-3 食物分类色对比度**
- `AGENTS.md` 固定 `#43A047 / #F57C00 / #E53935` 配白字。
- 实测白字对比度 **3.30 / 2.70 / 4.23**，均低于 §11.1 的 4.5:1；橙色最严重（2.70:1）。
- 处理：代码保留 `AppFoodKindCurrent` 不擅自改；同时提供 `AppFoodKindAccessible`（`#2E7D32 / #BF360C / #C62828`，白字 5.13 / 5.60 / 5.62）供对照。**待决策**：切换建议值需同步修改 `AGENTS.md` 的不变量。

**D-4 主色与分类色撞色**
- 现有主题主色 `#4CAF50` 与 `FoodKind.INGREDIENT` 的 `#43A047` 几近同色，用户会误判「食材」标签为可点击主操作。
- 处理：主色改用通用规范 §2.3 的 `#4C8DFF`（深色）/ `#1F5ED0`（浅色）；绿色让位给食材分类色。结果：**蓝 = 操作，绿/橙/红 = 分类**，同时满足 §2.3「同一层级只允许一个强调色」。

**D-5 页面切换动效**
- `AGENTS.md` 规定 180 ms 进入 / **140 ms** 退出。
- 通用规范 §2.9 的 `motion.exit` 为 **150 ms** `ease-in`。
- 处理：以本文档 150 ms 为准，`PageMotion.kt` 退出时长需从 140 改为 150，缓动改为 `ease-in`；`AGENTS.md` 对应条目待同步。

**D-6 不做像素风**
- 通用规范 §2.4/§2.5/§2.6 的像素字体、像素图标族、九宫格面板不采用。
- 理由：① 现有 85 个矢量图标重制为像素族需 ≈ 85 × 5 尺寸 × 5 状态 ≈ 2125 张资产，属美术工作量；② 中文小字号像素字体授权与 1604 条文案的字形覆盖风险；③ 血糖值、40+ 营养素表需要精确阅读，像素字体损害可读性。
- 处理：只落地令牌体系、状态矩阵与验收清单；图标沿用矢量、正文沿用系统字体、数值用等宽。

**D-7 触屏单端**
- 通用规范 §3 的焦点环、焦点陷阱、手柄映射、全局快捷键不适用。
- 处理：删除焦点环与快捷键；保留 §8.4 触感三档（确认 / 警告 / 错误）与 §11.1 的无障碍朗读、色觉辅助要求。

**D-8 信息型循环滚动文字**
- 通用规范 §9.9 规定循环动画只用于等待态或可交互提示，不用于信息展示。
- 缩略图角标宽度被 96 dp 图片限制，3 个标签既要 12 sp（§4.2 字号下限）又要完整可读，二者不可兼得。
- 处理：新增 `common/ui/MarqueeRow.kt`。仅当内容实际溢出 4 dp 以上才缓慢往返滚动：停顿 1.2 s → 3.6 s 匀速滚到末端 → 停顿 1.2 s → 回滚；放得下时完全静止。
  不使用省略号截断，因为角标只有 2–3 个字，截断后无法辨识。`modifier.horizontalScroll(state, enabled = false)` 保证用户不能误拖动。

**D-9 深色底采用纯黑**
- 通用规范 §2.3-2 规定 `color.bg`「不使用纯黑 `#000000`」，理由是纯黑与面板的明度差不足以表达层级，且大面积纯黑在 OLED 上易产生拖影。
- 产品要求深色模式「整体为黑色」，故 `color.bg = #000000`，并去掉原有 `#0B1020` 的蓝色调。
- 补偿措施：面板 / 浮起层改用中性灰阶拉开三级明度差（`#000000` → `#0D0D0D` → `#1A1A1A`，约 5% / 7% 明度递增），
  保留 §2.7-1 的底材分层；正文对比度从 15.70:1 提升到 17.94:1，正文与描边对比度均达标。
- 范围：仅 `DarkAppColors` 令牌，浅色主题不变；未迁移到 `AppTheme` 的屏仍走旧 `HealthDietProTheme`，观感不会同步变黑，属既定迁移节奏。

**D-10 主色按纯黑底重算，提示色改用紫色相**
- 通用规范表 2.3-1 的 `color.primary = #4C8DFF`、`color.info = #38BDF8` 是按深蓝底 `#0B1020` 校准的，底换成纯黑后不再适配。
- 三个实测问题：
  1. **语义色亮度倒挂**：OKLCH 亮度 `warning 0.804` > `info 0.754` > `success 0.716` > `primary 0.658`，「提示 / 成功 / 警告」全部比「主操作」抢眼，与 §2.3「强调色只用于主操作与选中」的层级意图相反。
  2. **两个蓝不可辨**：深色 ΔH 28°、ΔE 13；浅色 `#1F5ED0` 与 `#0A6EA8` 亮度几乎相同（0.513 / 0.517，ΔL 0.004），ΔE 仅 8.1，用户无法从颜色区分「可点」与「说明」。
  3. **饱和中蓝在纯黑上发毛边**：`C=0.182 / L=0.658` 处于蓝光焦点误差最明显的区间，1–2 px 描边与细笔画在暗光下有光晕。
- 处理（保留品牌蓝，只重算取值 + 把 info 移出色相）：
  - 深色 `primary` → `#7FA6F0`（L 0.727 / C 0.117，黑底 8.62:1，面板 7.97:1），落在 Material 深色 primary 的 tone 80 区间。
  - 深色 `info` → `#C084FC`（H 306°，与主色 H 263° 相差 43°，ΔE 12.1，黑底 7.95:1）——亮度低于 primary，不再倒挂。
  - 浅色 `info` → `#7B2FBE`（白底 7.02:1、页面底 6.49:1，与主色 ΔE 14.7）；浅色 `primary` 保持不变。
  - `onPrimary` 仍为 `#000000`，8.62:1；`primaryContainer` / `onPrimaryContainer` 未动（10.09:1）。
- 与规范的差异：`primary` / `info` 色值偏离表 2.3-1，语义分工不变（蓝 = 操作，紫 = 提示）；§2.3「同一层级只允许一个强调色」现在成立——紫只承担状态提示，不作强调色使用。
- 影响面：令牌值改动即全局生效，`colorScheme.primary` 66 处、`tokens.colors.primary` 20 处调用点无需改动；`info` 仅 2 处直接引用与 `tertiary` 映射，`DesignTokenContrastTest` 8 个用例覆盖新值。
- 遗留：`color.info` 与图表色 `organicAcid #6A1B9A`（浅色）同属紫色相，两者不会同屏出现；若后续提示条与营养构成图同屏，再评估。D-4 中「主色取 `#4C8DFF`」的表述以本条为准。

---

## 5. 组件状态矩阵

采用通用规范 §6.6-1，仅保留触屏列：

| 组件 | 默认 | 悬停 | 焦点 | 按下 | 选中 | 禁用 | 加载 | 空 | 错误 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 按钮 | ✓ | — | — | ✓ | ✓ | ✓ | ✓ | — | — |
| 图标按钮 | ✓ | — | — | ✓ | ✓ | ✓ | ✓ | — | — |
| 输入框 | ✓ | — | — | — | — | ✓ | — | — | ✓ |
| 复选框 | ✓ | — | — | ✓ | ✓ | ✓ | — | — | — |
| 标签页 | ✓ | — | — | ✓ | ✓ | ✓ | — | — | — |
| 列表项 | ✓ | — | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 卡片 | ✓ | — | — | ✓ | ✓ | ✓ | ✓ | — | — |
| 进度条 | ✓ | — | — | — | — | ✓ | ✓ | — | ✓ |
| 下拉菜单 | ✓ | — | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

补充约束（来自 `AGENTS.md`）：
- 禁用态必须在同区域说明原因，不能「消失得无影无踪」。
- 加载态按钮保留原宽度，防止布局跳动；加载期间不可重复触发。
- 破坏性操作必须先弹二次确认，默认焦点在「取消」。
- 危险操作文案用具体动词，禁止泛化的「确定」。

---

## 6. 验收清单

对照通用规范 §13.1，保留适用项并补充项目项。

| 分类 | 检查项 | 通过标准 |
| --- | --- | --- |
| 视觉 | 字号、颜色、间距取自令牌 | 无随手值；新增硬编码色 / 裸 dp 为缺陷 |
| 视觉 | 对比度达标 | 正文 ≥ 4.5:1，大字 ≥ 3:1，控件描边 ≥ 3:1；由 `DesignTokenContrastTest` 守护 |
| 视觉 | 材质与光影一致 | 同屏 ≤ 2 种材质，光源统一左上 135° |
| 布局 | 安全区内无关键内容 | 刘海、圆角、手势条不裁切；嵌套 `BaseScreen` 传 `includeStatusBarPadding = false` |
| 布局 | 超长与窄屏重排正确 | 不拉伸、不重叠、不截断 |
| 状态 | 默认/按下/选中/禁用/加载齐全 | 与本文件 §5 一致 |
| 状态 | 加载、空、错误齐全 | 不出现空白页与死路 |
| 输入 | 触屏可完成全部操作 | 无「只有悬停/双击可用」的功能 |
| 浮层 | 类型选择正确 | 与本文件 §3.2 一致 |
| 浮层 | 关闭方式与焦点归还正确 | 返回键、遮罩行为符合契约；无模态套模态 |
| 动效 | 时长与缓动使用令牌 | ≤ 400 ms；演出可跳过 |
| 动效 | 减少动态效果可用 | 关闭动画后功能完整 |
| 文案 | 按钮使用「动词 + 对象」 | 不出现泛化的「确定」 |
| 数据 | 文案、颜色、数值来自资产 | 三套 `strings.xml` 均含同 key；无硬编码中文 |
| 无障碍 | 色觉、字号、朗读达标 | 字号 100/125/150% 不截断；灰度可辨 |
| 性能 | 界面打开 ≤ 200 ms | 超过时显示骨架或加载 |
| 性能 | 长列表虚拟化 | 不一次性实例化全部条目 |
| 一致性 | 同类组件样式与行为一致 | 不出现两套按钮或两套提示 |

---

## 7. 实施状态

| 项 | 状态 | 位置 |
| --- | --- | --- |
| 令牌层（颜色/间距/字号/动效/圆角/层级） | ✅ 已完成 | `common/ui/designsystem/AppTokens.kt` |
| 主题包装（`AppTheme`） | ✅ 已完成 | `common/ui/designsystem/AppTheme.kt` |
| 对比度计算（纯 Kotlin，可 JVM 测试） | ✅ 已完成 | `common/ui/designsystem/ContrastRatio.kt` |
| 对比度回归测试（6 个用例） | ✅ 已完成 | `test/.../designsystem/DesignTokenContrastTest.kt` |
| 图表分类配色令牌（深/浅双套） | ✅ 已完成 | `AppTokens.kt` → `AppChartColors`，16 个对比度断言 |
| 整数千分位公共方法 | ✅ 已完成 | `common/format/NumberFormat.kt` |
| 令牌预览屏 | ✅ 已完成 | `common/ui/designsystem/DesignSystemPreview.kt`，入口：测试页 → Features |
| 试点屏：营养详情（食材/食物/菜肴） | ✅ 已完成 | `ui/nutrition/NutritionScreen.kt` → `FoodDetailScreen` 局部套 `AppTheme` |
| 全局主题切换到 `AppTheme` | ⬜ 待做 | 现仍用 `HealthDietProTheme`（`Theme.kt`） |
| `FontTokens` / `MaterialTheme` 调用点替换 | ⬜ 待做 | 影响约 500 处调用点 |
| 35 个公共组件按状态矩阵重建 | ⬜ 待做 | `common/ui/*.kt` |
| 70 屏逐屏重刷 | ⬜ 待做 | 见第 8 节批次 |
| `PageMotion.kt` 退出 140 → 150、缓动改 `ease-in` | ⬜ 待做 | D-5 |
| 清债：129 处硬编码色、1163 处裸 dp、134 处圆角 | ⬜ 待做 | 并入组件与逐屏批次 |

---

## 8. 逐屏重刷批次

按依赖顺序执行，前批结论回灌后批。

| 批次 | 范围 | 行数 | 备注 |
| --- | --- | --- | --- |
| 1 | `ui/settings` + `BaseScreen` + 底部导航 | 826 | 最小闭环，先验证令牌 |
| 2 | `ui/record` | 7,532 | 最大单块，含图表/表格/编辑器 |
| 3 | `ui/diet` | 3,407 | |
| 4 | `ui/nutrition` | 3,180 | 食物卡片与三分类色块 |
| 5 | `ui/profile` | 3,718 | |
| 6 | `ui/container` + `ui/sleep` | 3,250 | |
| 7 | 剩余屏 + 删除 `ui/test`（9 文件） | — | |

### 8.1 试点结论：营养详情屏

`ui/nutrition/NutritionScreen.kt` 的 `FoodDetailScreen` 作为第一个真实屏试点，采用**局部主题**推进，
不改变全局 `HealthDietProTheme`，因此可与未迁移的屏并存：

- `FoodDetailScreen` → `AppTheme(darkTheme = rememberAppDarkTheme())` → `FoodDetailContent`。
  `rememberAppDarkTheme()` 复用 `AppPrefs.darkMode`（YES / NO / 跟随系统），与全局主题判定一致。
- 颜色分层取值：
  - 仅详情可达的代码（`HealthMetricsTable`、`DetailPanel`、`NutritionEnergyAndExercise`）用 `LocalAppTokens.current`；
  - 跨屏共享的组件（`DetailSectionTitle`、`AppDataTable`、`EqualWidthSegmentedTabs` 等）只读 `MaterialTheme`，
    由外层主题决定取值，避免在旧主题下取到深色令牌。
- 间距 / 圆角 / 字号使用与主题无关的 `AppSpacingDefault` / `AppRadiiDefault` / `AppTypeDefault`，
  可安全用于共享组件。
- 数字统一 `type.numeric`（等宽）承载数值列与能量值，提升表格纵向对齐。
- 容器统一 `DetailPanel`：`panel` 底 + `border` 2 dp 描边 + `radii.container` 4 dp 圆角。
- 三分类正名色仍取 `AppFoodKindCurrent`（= `AGENTS.md` 固定色），本次不改观感，只把硬编码收敛到令牌。

### 8.2 本轮裁定与修复（营养详情）

产品 / 设计对审计结论逐条裁定后的落地情况：

| 审计项 | 裁定 | 落地 |
| --- | --- | --- |
| P0-2 供能图表、能量值缺数据时整段消失 | 按规范改 | 图表与能量行改为空状态说明（`nutrition_energy_empty`），不再 `return` 造成空区 |
| P0-4 详情传入从未使用的 `showGlycemicIndicator` | 详情恒显示血糖 | 删除该无效参数，行为不变 |
| P0-5 删除确认框标题与按钮同名、未用危险色 | 改 | 标题改「删除%1$s？」（按 `FoodKind` 取分类名），确认按钮用 `colorScheme.error` |
| P1-8 图表分类色在面板上难辨 | 给出可辨方案 | 新增 `AppChartColors`（深 / 浅两套，面板上非文本对比度实测 ≥ 3:1），详情图表改用 `LocalAppTokens.current.chart` |
| P1-9 可点击热区普遍 < 48 dp | 按规范改 | 分页箭头移入独立 48 dp 行并补位置指示点；可跳转行、运动步数行 `heightIn(hitMin)`；分段控件 44→48 dp；星级 14→16 dp |
| P1-10 缩略图系统标签压到 9 sp | 改跑马灯 + 12 sp | 见 D-8；标签回到 `AppTypeDefault.caption`（12 sp），内容放得下时静止 |
| P2-12 营养表数值未右对齐 / 未等宽 | 按文档改 | `AppDataTableColumn(alignment = CenterEnd)` + `AppDataTableText(numeric = true)`（等宽数字） |
| P2-14 整数缺千分位 | 抽公共方法 | 新增 `common/format/NumberFormat.kt` 的 `formatGroupedInteger`，用于千卡、千焦、步数、环图中心值 |
| P2-17 菜谱分区间距不一致 | 改 | 分区标题统一 `s5`（24 dp）、分区内标签 `s1`/`s2` |
| P2-11 / 13 / 15 / 18 | 维持现状 | 字号分级、间距微差、图标列宽、详情内二级返回均按原设计保留 |
| P0-1 「如何估重」空页签 | 暂不处理 | 缺数据资源，等资源补齐后填内容 |

图表配色同时登记于令牌层：`AppChartColors.forNutrient(nutrientId)` 按营养素 ID 取色，
深色面板 `#0D0D0D` 与浅色面板 `#FFFFFF` 各一套，`DesignTokenContrastTest` 覆盖 16 个断言。

### 8.3 本轮裁定与修复（营养详情第二轮 + 深色令牌转黑）

**一、深色令牌整体转黑（D-9）**

`DarkAppColors` 由蓝色调 `#0B1020` 体系改为中性黑灰阶：底 `#000000`、面板 `#0D0D0D`、浮起 `#1A1A1A`、
描边 `#2B2B2B`、强描边 `#616161`、正文 `#EDEDED`、次要文字 `#A0A0A0`、禁用 `#5A5A5A`。
`onPrimary` / `onSuccess` / `onWarning` 同步改为 `#000000`（原为 `#0B1020`），语义色与浅色主题保持不变。
`DarkAppChartColors` 的 8 个分类色全部复核，在 `#0D0D0D` 上最小 4.50:1（fiber），仍满足非文本 3:1。

**二、营养详情（食材 / 食物 / 菜肴）逐条修复**

| 审计项 | 依据 | 落地 |
| --- | --- | --- |
| 供能图例按数值分 15 / 12 / 9 sp | §2.4 最小字号 12 sp；同屏字号层级 ≤ 4 级 | 统一 `font.caption`，删除 `macronutrientLegendTextSizes` / `...DotSize` 两个按数值取字号的函数；数值差异仍由环形扇区与标签内置百分比表达 |
| 健康指标表固定 184 dp / 40 dp / 48 dp | §11.1 字号 150% 不截断 | 表头与数据行改 `heightIn(min = space.7)`，整表用 `IntrinsicSize.Min` 撑开；血糖杯列改 `weight(2f)/weight(1f)`，不再硬编码 96 / 48 dp |
| 血糖分级用 primary / secondary / error | §2.3 语义色只表达状态 | 低 = `success`、中 = `warning`、高 = `danger`；`secondary` 在新令牌下等于次要文字色，原实现使「中」看起来像禁用 |
| 收藏图标用 `danger`、难度星用 `secondary` | §2.3 危险色只用于删除 / 不可逆操作 | 收藏改 `warning`；难度星填充用 `text`、空星用 `border`（评分是数据，不是状态） |
| 删除图标与编辑图标同色 | §4.1 危险变体 | 删除按钮描红（`danger`），编辑仍为次要色 |
| 食物 / 菜肴「辅料」标签取 `tertiary` | §2.3 | 改次要文字色，与「主料」标签同级 |
| 信息行值文本无 `weight`，长值被裁 | §2.4 多行最多 3 行、超长滚动 | `InfoLine` 值列 `weight(1f)` 换行；`StarRatingLine` / `DishComponentLine` / `IngredientJumpLine` 统一 `space.1` / `space.2`，用量列改等宽 |
| 计时器 10 dp 圆角、32 dp 按钮、6 dp 间距 | §2.6 圆角 4 dp；§4.1-3 最小热区 48 dp | 圆角 `radii.container`、按钮 `hitMin`、间距取 `space.1` / `space.2`；暂停按钮补 `contentDescription` |
| 来源链接可点击区仅一行文字高 | §4.1-3 | 包一层 `heightIn(min = hitMin)` |
| 营养素表解析失败时只剩表头 | §6.2 空状态 | 新增 `DetailEmptyState`（图标 + 标题 + 说明 + 下一步），空表替换为「暂无营养素数据」并说明如何恢复 |
| 未上线页签只有一行「内容正在规划中」 | §6.2 | 同一空态组件，补说明「可先看营养档案页签」 |
| 灯箱只有点按背景与返回键可关 | §5.5.7、§7.3 必须有可见关闭入口 | 顶部加 48 dp 关闭按钮与提示文案，容器改用面板色 + 描边 |
| 详情名条固定 28 dp、22 sp 旧字号 | §2.4 / §11.1 | 改 `heightIn(min = space.6)` + `titleS`(20 sp)，别名 14 sp；无别名时正名占满整条 |

**三、未变更项（有意保留）**

- 三分类正名色仍为 `AppFoodKindCurrent`（`AGENTS.md` 固定色），Q-1 未裁定前不动。
- 详情的图表、表格、分段控件仍读 `MaterialTheme`：它们跨屏共享，由外层主题决定取值，符合 §8.1 的分层规则。
- 「如何估重」页签仍为空态占位（缺数据资源）。

### 8.4 详情页改为区块卡（第三轮）

**一、诊断**

改造前 8 个展示块里只有 3 块有底材（头部、健康指标表、营养素表），其余 5 块直接贴在页面背景上；区块标题一律用 `DetailSectionTitle` 画在容器**外面**。结果：页签切换时材质跳变（营养档案页有底材、制作步骤页全裸），标题与内容没有视觉绑定，分组只能靠 24 dp 间距——在纯黑底上滚起来几乎看不见边界。

**二、结论与落地**

- 采用**区块卡**而非「每块一张卡」：合并同组内容后每页 4 张，避免卡海与内边距累加（§2.2 同组内间距 < 组间间距）。
- 卡底取 `panelRaised`（`#1A1A1A`）：`panel` `#0D0D0D` 对纯黑只有 1.08:1，属 §2.7-1 的 1 级明度差，卡片边界会靠不住；`panelRaised` 是 2 级（10%），达标。
- 标题移入卡内（§2.4 把 `font.titleS` 的用途写作「卡片标题、面板小标题」），标题与内容靠 `s2` 间距分隔，不加分隔线，避免结构层第三层描边（§2.7）。
- 新增 `DetailSectionCard(iconRes, title, action, contentPadding)`；`DetailPanel` 保留给无标题的头部身份卡，底材同步改 `panelRaised`，避免同屏两种卡片明度。
- 营养档案页：身份卡 → 供能卡 → 健康指标卡 → 营养信息卡（份量下拉 + 能量/运动 + 表格三合一）→ 相关菜肴卡（仅在有数据时出现）。
- 制作步骤页：制作基础信息 / 食材清单 / 制作教程三张卡，与营养档案页同款；卡与卡之间统一 `s4`(16 dp)。

**三、连带修复（卡片提亮引起的回归）**

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 表格表头与卡底同色而消失 | `AppDataTable` 表头用 `surfaceVariant`（= `panelRaised`）0.72 叠加 | 表格改为**容器无关**：行底透明、表头 `onSurface` 8%、斑马纹 3%、分隔线 14%；放在 panel / panelRaised / 白色卡片内层级一致 |
| 健康指标表与卡片描边叠成双线 | 表格自带 `clip + panel + 2 dp border` | 去掉自绘外框，只保留表头底色（`text` 6% 叠加）与网格线；左右内缩 `s3`，避免最外圈单元格线贴住卡片描边 |
| 计时器控件边界消失 | `StepTimer` 底用 `secondaryContainer`（= `panelRaised`），与卡底同色 | 改 `text` 6% 叠加 + 16% 描边；结束态仍用 `dangerFill` |
| 难度空星几乎看不见 | 空星用 `border` `#2B2B2B`，在 `#1A1A1A` 卡上仅 1.16:1 | 改 `textMuted` 45% 透明度，刻度可辨且不抢填充星 |

**四、已知限制**

- 营养素表保留 `maxBodyHeight` 的自身滚动，页面滚动需先把表格滚到底才继续（嵌套滚动）。彻底解法是给 `AppDataTable` 增加「随父容器滚动」模式（`wrapContent` 高度、不内滚），见 Q-6。

### 8.5 按《移动端设计系统规范》复检（第四轮）

**一、依据**

外部规范：`E:\Download\mobile-design-system-skill-main`（**只读引用，未改动该目录任何文件**），主要引用 01 视口与触控、05 浮层与底部抽屉、06 页面原型、07 踩坑与 16 项门禁。

**二、诊断（营养详情屏）**

| 问题 | 违反条款 | 证据 |
| --- | --- | --- |
| 健康指标说明用居中弹窗承载长图文 | 05 §一：居中弹窗「严禁用于常规图文长详情展示」 | `AppModal` + 三张卡 + 长正文 |
| 抽屉/弹窗缺拖拽手势与三档高度 | 05 §二 | 只有居中弹窗一种浮层 |
| 头部 4 个图标按钮两两热区 0 间距 | 01 §三、07 检查项 04：相邻可点项 ≥ 8 dp | 2×2 的 48 dp `IconButton` 紧贴 |
| NRV 表头可点区域仅约 42×20 dp | 01 §三、07 检查项 03：≥ 48 dp | `clickable` 挂在文字 Row 上 |
| 营养素表列最小宽合计 317 dp，360 dp 屏只剩 11 dp 余量 | 07 踩坑六、检查项 13 | 320 dp 屏与大字号下必出横向滚动 |
| 缩略图 `ContentScale.Fit`，非方图留黑边 | 06 §三：方形槽位 1:1 居中裁切 | 96 dp 槽位 |
| 蒙层内嵌表圆角 4 dp = 卡片 4 dp | 07 检查项 11：`R_outer = R_inner + Padding` | 内外两圈弧线错位 |

**三、落地**

- 新增 `common/ui/designsystem/AppBottomSheet.kt`：`AppSheetHeight`（Peek 30% / Half 60% / Full 80%）、36×4 dp 拖拽条、下滑关闭。
  - 手势不死锁：`onPreScroll` 先把手势还给列表；`onPostScroll` 仅在列表回到顶部时把剩余位移记到抽屉；`onPostFling` 按位移 20% 或速度 1200 px/s 判定关闭，否则回弹。
  - 底部安全区：面板背景铺到屏幕底边，内容区单独吃 `navigationBars` 内边距，既不被手势条压住，也不出现硬切色带。
- 健康指标说明改 `AppBottomSheet(snapPoints = listOf(Full))`，标题固定、内容区滚动 + 上下 24 dp 渐隐，内部仍为三张区块卡（GI / GL / 炎症指数）。
- `AppModal` 增加 `dismissOnScrimClick`（默认 true）：含输入项的表单必须传 false，落实检查项 09。
- 营养素表：列最小宽 105/130/82 → **96/116/72**（合计 284 dp，表格本身贴满卡宽 328 dp）；
  NRV 表头整格可点、`heightIn(min = 48 dp)`，并把箭头从 14 dp 提到 `s4`。
- 详情头部：行高 96 → 104 dp（两排 48 dp + 8 dp 间距），图标按钮组横向间距 `s2`。
- 缩略图统一 `ContentScale.Crop`（灯箱仍 `Fit`，不裁切观察原图）。
- `AppRadii` 新增 `sheet = 12 dp`；新增 `concentricRadius(outer, inset)`，蒙层内嵌分级表在 4 dp 卡片 + 12 dp 内边距下取 0。

**四、与《通用UI设计规范》的冲突取舍**

| 冲突点 | 本次取舍 | 理由 |
| --- | --- | --- |
| 2 dp 描边 vs 0.5 px 发丝线 | 结构卡保留 2 dp；表内分隔线维持 1 dp 低透明 | 2 dp 描边是本项目已确立的结构语言，改成发丝线会让纯黑底上的卡片边界消失；发丝线只约束「同一容器内部」的分隔 |
| 卡片 4 dp 圆角 vs 8–12 dp 圆角 | 卡片仍 4 dp，仅抽屉顶角 12 dp | 4 dp 是数据型界面的既定气质；抽屉是更高一层浮层，圆角大一档有助于读出层级 |
| 复杂表转纵向信息卡 | 营养素表保留 3 列 | 该表是「同一营养素的名称/含量/NRV」对照，卡片化后 20+ 行会膨胀约 3 倍高度；已通过收敛列宽消除横滚，满足「单屏不横向晃动」的实质要求 |
| 详情页配底部固定 CTA | 不加 | 本页没有转化型主操作，收藏/对比已放在头部 |

**五、未做（明确不做）**

- 弱网骨架屏与断网重试页：详情数据全部来自本地库，无网络请求。
- 底部常驻操作栏：无转化 CTA，加了反而是「一屏多个主按钮」。
- 全局 Toast 时长：`MainActivity` 仍有 `Toast.LENGTH_LONG`（约 3.5 s）超出 1.5～2.5 s 区间，属别处调用，留待统一清理，不混进本轮详情页改动。

### 8.6 三处说明浮层与相关菜肴卡片（第五轮）

**一、需求拆解**

详情页原先只有「健康指标」有问号。这一轮把说明体系补齐，三个入口共用同一套浮层：

| 入口 | 标题 | 板块 |
| --- | --- | --- |
| 健康指标 | 健康指标说明 | GI / GL / 炎症指数（沿用，含分级表） |
| 营养素供能 | 营养素供能说明 | 供能比说明、能量单位说明 |
| 营养信息 | 营养信息说明 | 能量单位说明、可食部说明、NRV 及 NRV% 说明 |

**二、实现**

- `NutritionHelpTopic` 枚举收敛「入口 → 标题 / 副标题」，避免三处各写一遍、日后改漏。
- 全屏浮层沿用 `AppBottomSheet`，内容多时只滚内容区，符合移动规范 05「长图文用抽屉」。**（高度参数与手势在 §8.7 重做）**
- 新增三个说明构件：`HelpSectionCard`（定义 → 要点 → 公式 → 举例 → 备注）、`HelpFormulaBlock`（等宽字体 + 6% 叠色底）、`HelpExampleBlock`（主色 8% 底 + 35% 描边）。公式与举例视觉分档，扫读时不用逐字看。
- **能量单位说明**抽成 `EnergyUnitHelpCard()`，两个抽屉引用同一份文案与版式，避免出现「同一个概念两套说法」。
- 文案依据：千卡/千焦换算 1 kcal = 4.184 kJ；Atwater 系数 4/9/4/2/7/3 kcal/g；NRV 取 GB 28050 成人参考值（能量 8400 kJ、蛋白质 60 g、脂肪 ≤60 g、碳水 300 g、膳食纤维 25 g、钠 2000 mg）。

**三、相关菜肴改独立卡片**

- 原来的「相关菜肴」是一张卡里堆若干文字链接，链接视觉权重低、也没有任何识别信息。
- 改为：**分组标题在卡外**（图标 + 标题 + 总数），下面每道菜一张独立卡（`RelatedDishCard`）：56 dp 1:1 缩略图 + 菜名（最多 2 行）+「热量 / 组分」元信息 + 右箭头，整卡可点，卡间距 8 dp。
- 数量控制：同一食材可能出现在几十道菜里（鸡蛋尤甚），按 `commonness` 取前 6 道，标题右侧报总数，超出时补一行「另有 N 道菜用到它」——不做假入口，只说明还有多少。
- ~~这是对「标题一律入卡」的有意例外：一组独立卡片再加一层外卡会变成卡中卡。~~ **该例外已撤销，见 §8.7 第三节。**

**四、未做**

- 说明板块不做折叠面板：每个板块正文约 2～4 行，折叠后反而要多一次点击，且折叠状态没有持久化价值。
- 相关菜肴不做「查看全部」页：当前没有按食材反查菜谱的独立目的页，加按钮会变成死链。

### 8.7 底部抽屉重做 + 相关菜肴入卡（第六轮）

**一、诊断：说明浮层为什么滚不动、为什么被导航栏切**

| 现象 | 根因 | 修复 |
| --- | --- | --- |
| 长图文显示不全且无法上下滑动 | 内容容器是 `Modifier.weight(1f, fill = false)`。Compose 的 `fill = false` 会用**无界主轴约束**测量该子项（不是"取分配值"），里层的 `verticalScroll` 因此认为「内容刚好填满、无需滚动」，把全部内容展开后被面板 `clip` 裁掉 | 改成 `weight(1f)`（固定分配）——滚动区拿到确定高度，`verticalScroll` 才有 `maxValue` |
| 面板高度依赖 `heightIn(max = 屏高 × 0.8)`，短内容会缩、长内容靠裁剪 | 面板高度不固定时，内部约束随内容浮动，滚动行为也跟着不稳定 | 面板**恒为满档高度**，用垂直偏移表现"露出多少"，内部布局一次算准，滚动位置在档位切换时不被重置 |
| 底部被三键导航遮住最后一行 | ① 原实现把 `windowInsetsPadding(navigationBars)` 加在内容容器**外层**，只是把容器压缩，滚动内容仍会漏到导航栏下方；② 只给**滚动内容**加 `padding(bottom)` 仍然"看不出变化"——面板背景照样铺到屏幕底，被 `NavigationBar0` 那个窗口整条压住，底边和圆角都看不见；③ Dialog 是独立窗口，Compose 的 `WindowInsets.navigationBars` 在这里实测恒为 `0.dp` | 留白改加在**面板外层**（`padding(bottom = inset)`），整个抽屉抬到导航条之上，底边与圆角可见、系统键不再压字；取值改走 `overlayNavigationBarPadding()`（见下） |

**底部留白：浮层语义，与页面不同**

页面（`BaseScreen`）底部通常只是留白，手势导航下内容延伸到手势条下没有问题，所以 `adaptiveNavigationBarWindowInsets()` 在手势模式返回 0。**浮层底部是可交互内容和面板边界**，三键条与手势条都会压住它，因此抽屉改用 `overlayNavigationBarPadding()`——两种导航方式都让位。

| 步骤 | 来源 | 说明 |
| --- | --- | --- |
| 1 | `ViewCompat.getRootWindowInsets(view)` 的 `navigationBars.bottom` | **Dialog 窗口里唯一可靠的通道**。实测测试机（1440×3200 @600 dpi，`navigation_mode=0`）返回 `177 px = 47.2 dp` |
| 2 | Compose `WindowInsets.navigationBars` | 次要通道；Dialog 内实测恒为 `0.dp` |
| 3 | 系统资源 `navigation_bar_height` | 前两者都取不到时兜底 |
| 4 | 按 `systemNavigationMode()` 取常量 | 三键/双键 48 dp、手势 24 dp，保证**永远不为 0** |

最终结果不做 `remember` 缓存：组合可能早于窗口下发 insets，缓存会把 0 永久固化。

**二、`AppBottomSheet` 重做（`common/ui/designsystem/AppBottomSheet.kt`）**

- **档位**：`AppSheetHeight` 改为 `Peek(30%)` / `Half(60%)` / `Full(80%)`，`snapPoints` 数据驱动，默认三档全开；传单档时自动退化为「全高 + 下滑关闭」。本页三个说明浮层只传 `Full`。
- **吸附**：松手时取 `当前位置 − 速度 × 0.12 s` 做投影，吸附到最近的锚点；投影低于最低档 55% 或（单档时）下甩速度 > 1200 px/s 则关闭。吸附用 `spring(dampingRatio 0.85, StiffnessMediumLow)`。
- **阻尼**：向上拖过满档、向下拖过屏幕底边，位移只按 32% 计入，形成"拉不动"的手感，不会硬停在边界。
- **手势分工**：拖拽条与标题区用 `draggable`（带速度采样）；内容区通过 `NestedScrollConnection` 参与——`onPreScroll` 只在未到满档时先撑开抽屉，`onPostScroll` 只在列表已到顶（`!canScrollBackward`）时才带动抽屉，因此不会出现"滚列表顺手把抽屉拖走"或两者死锁。
- **入场**：`tween(motion.sheetMs)` 淡入 + 48 dp 上移，`animateFloatAsState` 驱动遮罩透明度。

**三、相关菜肴回到卡内**

- 第五轮把标题放在卡外、每道菜一张独立卡，理由是"避免卡中卡"；但同屏其他板块（营养档案、供能、健康指标、营养信息）都是"标题在卡内"的区块卡，相关菜肴变成唯一例外，视觉上像是漏了一层容器。
- 本轮改回 `AppSectionCard`：标题、总数、列表全部收进同一张卡。
- 卡内**不再套二级卡片**（那才是真正的卡中卡）：改成"无底色行 + 发丝分隔线"的列表式，行高 ≥ 56 dp，48 dp 1:1 缩略图 + 菜名（≤2 行）+ 元信息 + 右箭头，整行可点。分隔线内缩 `s3` 与文字左缘对齐。
- 数量控制不变：按 `commonness` 取前 6 道，标题右侧报总数，超出补一行「另有 N 道菜用到它」。

**四、验证方式**

- 只跑 `:app:compileDebugKotlin`；不连设备、不装包、不截图、不改分辨率（用户裁定）。

---

### 8.8 记物价（新功能）

**一、需求边界**

一条物价 = 物品 + 地区 + 日期 + 数量单价。用户明确的原则：同一物品在不同地区、不同时间价格不同；**完全相同的物品/地区/日期，也可能同时存在不同售卖单位的记录**（土豆 3.2 元/斤、1.5 元/个）。

**二、数据模型（`model/price/PriceRecord.kt`）**

| 字段 | 说明 |
| --- | --- |
| `itemSource` / `itemId` / `itemName` | 物品来源（食材/食物/菜肴/自定义）+ 库内 id + **冗余保存当时的显示名**。食材库更新或自定义食材被删掉后，历史物价不应消失或改名 |
| `price` + `priceMax` + `priceUnit` | 价格**下限**、**上限（可空）**、物价单位（币种，默认「元」）。`priceMax == null` 即单值；不做基准单位换算，币种就是文本 |
| `sellUnit` | 售卖单位，如 斤 / 个 / 500 克；可自由输入，预设只是快捷项 |
| 省市区 code + name + lng/lat | 地区冗余存名称，行政区划调整后仍可读 |
| `recordedAt` | **精确到日**（当天 00:00），物价没有分钟级意义 |

**身份键** = `itemSource + itemId(或名称) + 省 + 市 + 区 + 日期 + 售卖单位 + 物价单位`。注意**没有把它做成数据库唯一约束**：同一天同一地区同一物品同单位完全可能早上一个价、傍晚一个价，两条都值得记。

键相同 = 同一张卡片：`groupByPrice()` 把同键记录合并成一条**价格区间**（下限取最小、上限取最大），列表只显示这一条，不再弹「已存在相同条件的记录」——既然能合并，拦截就是多余的一次点击。编辑页顶部会把合并结果预算出来（「该条件下已有 N 条记录，保存后会合并显示为 X」）。

**三、存储**

`PriceArchiveStore` 沿用 `user_archives/<userId>/prices.json` + `encodeDomain/decodeDomain` 信封，域 id `prices`。和容器、饮水、饮食一致：用户备份导出自动带上，`writeUserArchiveManifest` / `noteUserActivity` 一并调用。

**四、界面**

**入口**：记录中心 →【杂项】第二位（`Container` 之后、两个禁用项之前）。物价是「顺带记一笔」的工具，放在日常三段会稀释 Diet / Water / Sleep 的高频入口；杂项组本身也只承载非日常流水（容器容量、习惯、身体感受）。

**两个视图，底部切换栏**：`DetailTabBar`（与睡眠 / 饮食 / 血压同一套）+ `AnimatedPageContent` 切页。顺序按用户要求：**【统计】在左、【记录】在右**；**默认停在【记录】**——「添加物价记录」这个主动作在那一页，进来就能记一笔，不用先切 tab。`BaseScreen(includeNavigationBarPadding = false)`，底部让位由 `DetailTabBar` 自己的 `adaptiveNavigationBarWindowInsets()` 处理，页面不再重复加一圈。

版式对齐【食材 / 食物 / 菜肴详情】：页面只留 `bg`，内容全部装进 `AppSectionCard`（卡底 `panelRaised`、`radii.container` 圆角、`radii.stroke` 描边），卡片间距 `s4`，页边距 `s3`/`page`。卡内不再出现「灰底块 + 灰底行」的第二层容器——可点项一律用「行 + 发丝分隔线」，只有真正的输入控件才带描边（§2.7 底材只分底 → 面板 → 浮起三级）。

| 页面 | 要点 |
| --- | --- |
| 【记录】页（`PriceRecordsTab`） | ① 全宽「添加物价记录」＝全页唯一填充按钮（§1.3 单一主动作）；②【查找与筛选】卡：搜索框 + 地区 chip（chip 视觉 28 dp、热区 48 dp，选中态主色 2 dp 描边 + `primaryContainer` 底）；③【物价记录】卡：标题右侧 `共 N 条`（N = 合并后的卡片数），卡内每行 = 物品名（`body`）+ `地区 · 日期`（`caption`，合并多条时追加「合并 N 条」，可选备注）+ 右侧等宽**价格区间**（`3.2–4.5`，`numeric` 右对齐，§2.4-2）与 `元 / 斤`，行高 ≥ 56 dp，整行进编辑（打开组内最近更新的一条），末端 48 dp 删除（删除整组，确认文案写明会一并移除 N 条）。**无记录时不渲染筛选卡**，只留空态（§6.2） |
| 编辑（`PriceEditorScreen`） | 四张卡：物品 / 价格 / 地区与日期 / 备注，卡内用 `PriceNavRow`（标签 + 值 + 右箭头，≥ 56 dp）承载选择项；价格卡第一行「最低价」「最高价」两格（**两格都不标必填/可选**，规则是至少填一格，只填一格即单值，填在哪一格都算），第二行「售卖单位」输入 + 「物价单位」下拉，以下是快捷售卖单位 chip 与说明；同名物品已有记录时插入【该物品最近的价格】卡（≤3 个合并区间，只读对照）；底部 `FormSaveBar`，返回未保存弹 `DiscardChangesDialog`（**新建时按内容是否动过判断**，空表单直接退出不弹） |
| 物品选择（`PriceItemPickerDialog`） | 复用库内食材/菜肴搜索（`FoodItem.searchMatchRank`，未命中再退到 `searchIds`）；搜索词不是已有物品时，置顶一条「用『xxx』这个名称」，即自定义物品 |
| 地区选择（`PriceRegionPickerDialog`） | 省 → 市 → 区三级；顶部两个快捷入口——「使用资料中的地区」（资料里设过常驻地区时才出现，配合副标题显示具体省市区）、「使用当前定位」；支持只记到省 / 只到市（区级数据缺失时不卡住） |

| 【统计】页（`PriceStatisticsTab`） | 一张【物品价格】卡，行 = 物品名（`body`）+ `N 个地区 · N 条记录`（`caption`）+ 右列按单位分行的价格区间（`numeric` 数字 + `caption` 单位）。单位超过 2 个时第三行折成「另有 N 种单位」。卡下一条 `caption` 说明口径；有筛选时卡上方多一行「当前筛选：…」+ 清除。空态单独一套图标（`ic_chart`）与文案 |
| 统计聚合（`model/price/PriceStats.kt`） | `List<PriceRecord>.statsByItem()`：**只按物品名聚合**（`trim + lowercase`，手输名与库内名算同一种，不用 id——用户认得的是名字）。先复用 `groupByPrice()` 得到各单位的区间，再按物品收口；不同 `priceUnit`/`sellUnit` **分行**不合并（元/斤 与 元/个 不可比），**不做基准单位换算**。排序按最近记录日期倒序。`recordCount` 是原始条数，不是卡片数 |

**地区默认值**：新建记录直接取**个人资料里的地区**（`ProfilePrefs.load().region`），不主动定位。理由：记物价记的多是「我住的地方」的日常价，用户已经在资料里告诉过 App 自己住哪，每次进来再定位一遍既是多余，也会因为室内定位漂移把省市区带偏。只有临时在外地、或资料里没设地区时，才由用户在选择器里点「使用当前定位」（`CurrentLocationProvider` + `RegionRepository.resolve` 反查），权限也在那一刻才申请。

**日期默认值**：默认今天。日期卡右端有「今天」复位 chip，**只在日期被改到别的日子时出现**（已选今天时不给一个点不动的按钮），点击回填今天；修改历史价格时才需要手动挑日期。原 `RecordTimePickerField` 的灰底块与卡内行风格冲突，这里改用行内 chip（`ic_time` + `caption`），热区同样补到 48 dp。

**单位预设**：售卖单位与物价单位的快捷项按 locale 分两套（中文：斤 / 公斤 / 500 克 / 个 / 盒…、元 / 角；其他：jin / kg / 500 g / piece…、CNY / USD…），避免非中文环境默认选中「斤」。

**五、与既有约定的一致点 / 偏离**

- 选择物品与地区的弹层用 `AlertDialog(usePlatformDefaultWidth = false)` + 自定宽度，而不是 `AppBottomSheet`：抽屉内容区是 `verticalScroll` 容器，里面放 `LazyColumn` 会因无界高度崩溃。项目里 `DietFoodSheet` 同理，保持一致（记 Q-7）。
- 列表 / 编辑 / 保存条 / 丢弃确认 / 日期选择全部复用既有组件，没有新造控件。
- 列表与编辑不再使用 Material `Card`、也不再拿 `surfaceVariant × 0.45` 拼灰底行：卡片一律 `AppSectionCard`，颜色 / 间距 / 字号 / 圆角全部走 `LocalAppTokens`（`spacing.page`、`s1`–`s5`、`radii.container/label/stroke/highlight`、`type.caption/body/bodyS/numeric`），和详情页共用同一套取值，避免同一 App 出现两种卡片明度。
- 单价用 `type.numeric` 等宽右对齐，数值变化不抖（§2.4-2）；数值本身用正文色而不是主色——强调色只留给主操作与选中态（§2.3）。
- 唯一新增的视觉资产是 `ic_price`（价格标签），沿用 24 dp / viewport 24 的现有图标规格。

**六、未做**

- 物品维度价格趋势图：现在按「物品 + 地区 + 日期 + 单位」的合并卡片倒序排列，趋势图需要按物品聚合的时间序列视图，单独一期。
- 卡片内展开组内明细：合并后只显示区间与条数，没做「点开看早上 3.2、傍晚 4.0」的二级列表；编辑入口打开的是组内最近更新的一条。
- 统计页的价格趋势 / 环比：现在只有区间，没有「近三个月走势」；需要按物品聚合的时间序列，单独一期。
- 统计页不提供均价：不同单位不能混算，单一单位下均价又会掩盖早晚价差，区间已经回答了「大概多少钱」。
- 定位结果反查只到区级质心（`RegionRepository` 既有精度），城区边界附近 1–3 km 误差，可接受。
- 列表用「整页滚动 + 单卡」而不是 `LazyColumn`：一块一卡才能和详情页的板块语言对齐，而物价是手工逐条录入、量级在几十条内。若真到数百条以上，再按 §4.6 拆成虚拟列表（届时行容器要改成逐行卡片，视觉分组会弱一档）。

---

### 8.9 BMI 历史 + 个人信息（第九轮）

两屏都改成「局部 `AppTheme` + 区块卡 + 令牌」的写法，与【食物 / 食材 / 菜肴详情】【记物价】同一套语法。
`AppTheme` 套在 Activity 的 `setContent` 内层（`BmiDetailActivity` / `ProfileEditActivity`），
这样顶栏、弹窗、输入框、图表拿到的是同一份 colorScheme，不再出现「页面按令牌、控件按旧主题」的混色。

**BMI 历史（`BmiDetailScreen`）**

| 区域 | 做法 |
| --- | --- |
| 结构 | `BaseScreen(includeNavigationBarPadding = false)` + `AnimatedPageContent` 切页 + `DetailTabBar`（图表 / 数据），底部让位交给 `DetailTabBar` |
| 最新 BMI | 新卡：32 sp 等宽大数字 + 分级 chip + 测量日期 + 「身高 · 体重」来源 + 较上次的变化（±0.05 以内不显示） |
| BMI 趋势 | `AppSectionCard(containerColor = panel)` 包 `ComposeChart`：图表自身 Surface 也是 `surface`（= panel），同色才接得上；若用 `panelRaised` 会读成「卡里凹进去一块」 |
| BMI 分级 | 四行「范围 + 分级 chip」+ 底注（WS/T 428-2013）；范围用等宽数字右对齐 |
| BMI 计算器 | 身高 / 体重两格 `EditorTextField`（不标必填，规则是填了就算）+ 结果行；问号按钮补到 48 dp 热区 |
| 记录明细 | `AppDataTable(maxBodyHeight = 480.dp, fadeColor = panelRaised)`；时间列只到日、列宽 128 dp，避免 16 字符把表格撑到必须横向滚动 |
| 空态 | 图表页与数据页共用一套「图标 + 为什么空 + 下一步做什么」，不再留白 |

**分级配色**：资产里 `BmiUtil.loadBmiBands()` 的颜色是 `#26xxxxxx`（自带 15% 透明度），
作为**文字色**只有 1.2–2:1，且不随主题切换。本轮改为 `tokens.colors` 的四级语义色
（info / success / warning / danger），底衬统一 14% 透明度 + 45% 描边；曲线色带也用同一组色。

**个人信息（`ProfileEditActivity` → `ProfileEditScreen`）**

| 区域 | 做法 |
| --- | --- |
| 头像 | 卡内一行：64 dp 圆头像 + 「点击更换头像」+ 格式说明 + 右箭头，整行可点；首字母改用 `onPrimary`（深色主题主色是浅蓝，白字仅 2.4:1） |
| 基本信息 | 姓名走 `EditorTextField`（唯一带描边的控件）；**性别由下拉改为片选**（`AppDropdownField` 自带 20 sp 标题，会和卡标题同级打架）；生日、地区走行内导航 |
| 身体数据 | 身高 / 体重 / BMI 三行 + 说明「点进每一项可以查看趋势」；**去掉原来的灰底块**（卡底已是 `panelRaised`，再叠一层灰等于第二层浮起） |
| 输入行 | `ProfileNavRow`：`caption` 标签 + `body` 值 + 右箭头，最小 56 dp；未填写时值用 `textMuted` |
| 弹层 | 地区选择改为「图标 + 文案 + 箭头」的整行选项；省市区列表、病史列表行高提到 56 dp 并加发丝分隔线；定位弹层补 `CircularProgressIndicator`（原来是一行静态文字） |

**顺带修的三个实际缺陷**：
1. `ProfileEditUiState.bmiColor` 直接取 `BmiUtil.getBmiColor()`——同样带 15% 透明度，做文字色几乎不可见。该值文本已含「24.1 超重」，颜色是冗余信号，按 §2.3 回落到正文色；
2. BMI 分级标签原来走 `BmiUtil.getBmiLabel()`，取的是资产里写死的中文，英文环境会中英混排；改按 index 取 `bmi_*` 字符串资源；
3. 头像首字母白色在浅蓝主色上对比度不足，改 `onPrimary`。

### 8.10 「我的」细调 + 搜索框统一重构（第十轮）

**「我的」页（`ProfileScreen` / `ProfileUserInfoCard`）**

| 位置 | 改动 |
| --- | --- |
| 页面 | 套局部 `AppTheme`；底材统一 `colors.bg`；设置入口独立成行（48 dp 热区、20 dp 图标），固定在顶部不随内容滚动 |
| 个人卡 | `Material` 默认卡（16 dp 圆角 + 2 dp 阴影 + 2 dp tonal）改为 `radii.container` + 1 dp `border` + `panelRaised`，与已重刷的卡片同一语言 |
| 主信息 | 姓名 `titleS`（20 sp，SemiBold）+ 信息行 `bodyS`；信息行只放「性别 · 年龄」，不再拼生日（生日另有独立行，旧版会把生日显示两遍） |
| 副信息 | 生日 / 地区 / 疾病三行统一 `caption` + 16 dp 图标；性别符号改用语义色（男 `primary`、女 `info`），不再硬编码蓝/粉 |
| 设置项 | 三行自绘 `ProfileActionRow`（≥ 56 dp、图标 20 dp、标题 `body`、副标题 `caption`、右箭头 18 dp），分隔线 `border`，不再用旧 `SettingRow`（标题走 `titleLarge` = 24 sp，比卡标题还大） |
| 疾病去重 | 同名疾病只保留最近更新的那条（状态以最新为准），解决卡片出现「高血压、高血压、高血压」 |

顺带清掉 `ProfileUserInfoUiState.diseaseText` / `hasDiseaseText` / `resolveDiseaseText()` 三处死代码，
`formatProfileInfoLine` 更名为 `formatProfileHeadline`（不再接收 birthday），测试同步更新为 4 条。

**搜索框（新增 `AppSearchField`，位置 `common/ui/designsystem/`）**

旧结构的四个问题，逐条对应：

| 旧问题 | 现在 |
| --- | --- |
| 尾部并排「清空」「搜索」两个图标按钮，其中 5 个调用点有 4 个传空 lambda，按了没反应 | 放大镜在**前**（纯装饰）、清空在**后**（仅在非空时出现）；不再有提交按钮 |
| 【记录】页输入不触发过滤，必须点搜索图标，等于「输入了列表不动」 | 输入即过滤；`query` 是唯一真相，删掉 `submittedQuery` 这份拷贝 |
| 【记录】页回车无行为（没有 `imeAction`） | `ImeAction.Search` + `KeyboardActions`：回车 = 记历史并收键盘 |
| `OutlinedTextField` 56 dp 高、尾部两个 48 dp 按钮，输入区被挤到很窄 | 固定 48 dp 行：1 dp `borderStrong` 描边、聚焦 2 dp `primary`、`radii.container` 圆角，图标 18 dp |

`FoodSearchField` 保留旧名，改为 `AppSearchField` 的薄封装，四处调用点（饮食添加食物、营养编辑器、疾病搜索、物价筛选）
一次性换成同一套外观；原本传空 lambda 的两处直接去掉 `onSearch` 参数。

**搜索面板（`SearchActivityPanel` 重写）**

| 旧问题 | 现在 |
| --- | --- |
| 三层半透明灰底（面板灰 42% → 内嵌灰 72% → 每行再套 `surface`），层级读不出来、对比度随主题漂移 | 单层 `panel` 面 + 1 dp 描边，当作搜索框的下拉面板 |
| 历史是「5 行小列表 + 假滚动条 + 上下滑动提示」，还要自己算 thumb 位置 | 历史与最近都改为 chip 流式换行（胶囊 32 dp，删除键 28 dp），超过上限截断 |
| 清空历史一点即清，无二次确认（违反 §5 破坏性操作） | 先弹确认，默认「取消」 |
| 面板与列表挤在同一个 `verticalScroll` 里，营养表还要给列表一个「一屏高」的假高度才能滚 | 面板限高（记录 320 dp / 营养 240 dp）内部滚动，与列表分层；删掉 `height(screenHeight)` 这类补丁 |

**空态**：新增 `AppEmptyState`（图标 + 结论 + 下一步 + 可选出口）。
【记录】搜索无命中时给出「没有匹配的功能项 + 换个关键词 + 清空搜索」，不再让页面收成一片空白；
【营养表】按原因分两种文案（搜索没命中 / 筛选筛空），并提供「清除筛选」（`NutritionViewModel.clearFilters()`）。

**主题**：【记录】与【营养表】浏览页一并套上局部 `AppTheme`，搜索框、空态、卡片、图标色才与已重刷的详情页/物价页同源。
旧主题仍是全局默认（`HealthDietProTheme`），这两页属批次 2、4 的提前迁移。

### 8.11 搜索面板改为浮层 + 固定头栏（第十一轮）

第十轮把面板做成了 `Column` 里的一个兄弟节点，于是留下两个结构性问题，本轮修掉：

| 问题 | 表现 | 现在的做法 |
| --- | --- | --- |
| 收起键在滚动内容里 | 收起键是 `LazyColumn` 的最后一项，历史 + 最近超过限高后必须先滚到底才能点，唯一的出口被藏进内容 | 面板拆成「固定头栏 + 独立滚动的内容区」：头栏 48 dp（说明文案 + 右上角收起键），内容区 `verticalScroll`，头栏永远停在面板上边界（§5.2「超长时只滚动内容区，标题与操作固定」） |
| 展开即占位 | 面板作为兄弟节点插进 `Column`，展开时把下方功能网格 / 分类栏 / 结果列表整体下压；营养表为了让列表还能滚，还得给列表算一个「一屏高」的假高度 | 面板改浮层：页面根节点换成 `BoxWithConstraints`，面板 `align(TopStart)` + `padding(top = 搜索框下沿)` 叠在内容之上，不再参与排版。点面板外空白处收起（`SearchPanelDismissLayer`，只盖搜索框下方，点输入框仍可继续输入） |

**高度**：`minOf(320 dp, 可用高度 × 0.55, 可用高度 − 面板顶边)`，再 `coerceAtLeast(160 dp)`。
320 dp 是 §7.2 的下拉上限；0.55 那一项是给横屏和大号键盘兜底（Manifest 是 `adjustNothing`，窗口不随输入法缩放，只能自己收）。

**层级**：面板底色从 `panel`（`#0D0D0D`，与页面卡片同色，会糊成一片）提到 `panelRaised`（`#1A1A1A`），
加 8 dp 柔影（§2.7 浮层允许 ≤ 8 px），chip 则退回 `panel` 做内嵌对比。浅色主题下 `panel == panelRaised`，靠描边与投影区分。

**其它**：返回键先收起面板（§5.3「关闭最上层浮层」）；选中历史项后收起面板并清焦点，不再留着键盘。

**API**：`SearchActivityPanel(..., maxHeight: Dp = SearchPanelMaxHeight)`；新增 `SearchPanelDismissLayer(onDismiss, modifier)`；
`SEARCH_PANEL_MAX_HEIGHT` 两个私有常量删除（记录 320 / 营养 240 的分裂没有必要，统一 320 再按可用高度收）。
字符串新增 `search_activity_hint`（头栏那句说明）。

### 8.12 搜索面板收尾 + 记录设置-功能启用（第十二轮）

**搜索面板（`SearchActivityPanel`）**

| 项 | 改动 |
| --- | --- |
| 收起键 | 从面板右上角移到**下边界居中**，改成「`KeyboardArrowUp` + 收起」的图标文字样式；仍是 48 dp 行，自带 1 dp 上分隔线。位置在 `Column` 的固定段，`weight(1f, fill = false)` 只作用于中间内容区，条目再多也不会把它顶出可视区 |
| 说明文字 | 「点选下面的标签即可填入搜索框」不再常驻在头栏，改为面板出现（= 光标落进搜索框）时 180 ms 淡入；排版位置提前占好，避免面板高度跳动。输入框有内容 → 面板本身隐藏 → 这句随之消失 |
| 清除 | 「清除」由纯文字改为「`Delete` 图标 + 文字」，颜色用 `colors.danger`（不是 `textMuted`）——它确实会删数据，不该和普通次要操作同色 |

**「我的」页切换用户**

`TextButton`（裸文字，看着像可点段落）换成描边胶囊：`height = s6`、`radii.pill` 圆角、1 dp `colors.border`、点击热区整行。
图标 14 dp + 文字 `caption` 同一 `Row` 且显式 `CenterVertically`，字号行高不再把图标顶偏。

**用户设置 - 记录设置 - 功能启用**

层级：用户设置 → 记录设置 → 功能启用。功能启用页按记录中心同一套分节（基本信息 / 身体测量 / 健康监测 / 日常 / 杂项）列出
**已实现**（`enabled == true`）的功能，每行「图标 + 名称 + `Switch`」，行高与 `SettingRow` 一致；未实现的灰项不给开关。

| 项 | 决定 |
| --- | --- |
| 存储 | `RecordFeaturePrefs`（`model/prefs/`），键 `record_disabled_features_v1`，落 `UserPrefs.current()` → `files/user_archives/<uid>/user_preferences.json`，**用户级**，换用户各读各的 |
| 存「关」还是存「开」 | 存被关闭的 id 集合。默认全开，新增功能不必给存量用户补写开启记录 |
| 关闭后的表现 | 整条从记录中心移除（不是置灰）：关掉是为了精简界面；同时从「最近使用」里剔除，避免搜索面板还能点进已关掉的功能 |
| 过滤点 | `List<RecordSectionUiState>.withoutDisabledActions(disabled)`，纯函数，单测 4 条（空集合原样、跨节只删点名项、整节清空仍保留分节、未实现项不受影响） |

字符串新增：`user_settings_record` / `user_settings_record_desc` / `user_settings_record_features` /
`user_settings_record_features_desc` / `record_features_hint`（三套语言）。

### 8.13 记录功能启用重构 + 搜索提示与历史栈修正（第十三轮）

**入口：去掉中间页**

原层级「用户设置 → 记录设置 → 功能启用」两级跳，中间那页只有一个入口行，纯属多一次点击。
现在「用户设置」首屏直接一行【记录功能启用】（标题 + 副标题「按分类决定记录中心里出现哪些功能」），
点进去就是开关页。`UserSettingsPage.Record` 枚举与 `user_settings_record` / `user_settings_record_desc` 两条文案一并删除。

**功能启用页 UI 重构**

| 项 | 旧 | 新 |
| --- | --- | --- |
| 主题 | 整页仍是 `HealthDietProTheme`，与 `SettingRow` 的旧色板一致，但与已重刷的页面两套色 | 整个 `UserSettingsActivity` 换 `AppTheme(darkTheme = rememberAppDarkTheme())`：顶栏、卡片、开关吃同一份 token |
| 分组 | 裸 `Text` 标题（`titleMedium` + 主色）散在分隔线之间，读不出「这是一张卡」 | 每组一张 `AppSectionCard`：组图标（`ic_pk` / `ic_height` / …）+ 组名在卡标题行，与记录中心的分节同名同图标 |
| 行 | `titleLarge`（= 22–24 sp）标题 + `titleLarge+6dp` 的巨型图标，与开关抢注意力 | 行高 ≥ 56 dp、图标 20 dp、标题 `body`（16 sp）、尾部 `Switch`；整行可点 |
| 关闭态 | 无区分 | 图标转 `disabled`、标题转 `textMuted`；仍可读清是什么功能，便于就地打开 |
| 分隔线 | 全宽 `HorizontalDivider`，直接切到卡片边缘 | 左缩进 = 卡内边距 + 图标宽 + 间距，与文字左缘对齐 |
| 说明 | 卡外一段 `bodyMedium` 挤在列表首项 | 卡外 `caption` + `textMuted`，作为这一页的规则说明 |

**「我的」- 切换用户**

图标和文字没对齐的真因是**图标本身**：`ic_switch_user.xml` 里两枚箭头的 y 区间是 7–17 与 12–22，
整个图形重心在 14.5，而 24×24 画布中线是 12，等于图标自己是往下偏的；`CenterVertically` 只能对齐「盒子」，
对齐不了盒子里偏下的图形。现在改成 y 5–15 / 9–19，图形重心回到 12。
同时图标与文字都改用 `colors.primary`，描边取 `primary` 45% 透明度；高度由固定 32 dp 改 `heightIn(min = 32 dp)`，
大字号下不再裁字。

**搜索面板提示语（第三次修正）**

前两轮把「点选下面的标签即可填入搜索框」放在面板里，但面板的显示条件是「聚焦 + 为空 + **有历史或最近**」，
首次使用或清空过历史时根本不弹面板，那句提示自然永远看不到。现在：

| 项 | 改动 |
| --- | --- |
| 弹出条件 | 记录页 `searchFocused && !searching`、营养表 `searchFocused && keyword.isBlank()`——**聚焦即展开**，不再要求有内容 |
| 提示语 | 仍然在面板顶部，180 ms 淡入（`animateFloatAsState`），因此光标一落下就出现，输入任意字符（面板消失）时同步消失 |
| 空面板 | 历史与最近都为空时补一句 `search_activity_empty`（「还没有搜索记录，直接输入关键词即可搜索。」），不交白卡 |

**搜索历史栈容量**

`MAX_HISTORY_ENTRIES` 由 8 恢复为 **10**，并把它从「展示上限」升级为「栈容量」：
新增 `pushSearchHistory(history, term, limit = SEARCH_HISTORY_LIMIT)`（`model/prefs/SearchHistoryCodec.kt`），
记录中心与营养表共用同一条规则——新词进栈顶、同名忽略大小写去重、超 10 条最旧出栈。

> 溯源：8 是本轮对话第十轮重写 `SearchActivityPanel` 时随手写下的展示截断，此前代码里并没有 10 或任何上限
> （旧面板是 `LazyColumn` 全量渲染、只把可见行高限制在 5 行，历史本身无上限增长）。所以不是「改坏了 10」，
> 而是「引入了 8」；现在统一为 10 并落到写入侧。

单测：`SearchHistoryCodecTest` 增 5 条（入栈置顶、满 10 淘汰最旧、同名去重置顶、空白不变、自定义上限）。

### 8.14 「设置 + 收起」描边胶囊 + 软件设置重构（第十四轮）

**新增两个共用组件**

| 组件 | 位置 | 规格 |
| --- | --- | --- |
| `AppPillIconTextButton` | `common/ui/designsystem/AppPillIconTextButton.kt` | 图标 16 dp + 文字 `bodyS`，同为主色；胶囊高 32 dp、`radii.pill`、1 dp 主色描边（`AppPillBorderAlpha` = 45%）；**热区另算**：外层 `heightIn(min = 48 dp)` 且整块可点，视觉不撑高、触摸不吃亏（§2.2） |
| `AppListRow` / `AppRadioRow` / `AppListRowDivider` | `common/ui/designsystem/AppListRow.kt` | 图标 20 dp 主色、标题 `body`、副标题 `caption`、行高 ≥ 56 dp；`showChevron` 只给会切页面的行，`trailingValue` 是当前值；分隔线左缩进 = 卡内边距 + 图标 + 间距 |
| `AppSectionGroup` | `common/ui/designsystem/AppSectionCard.kt` | 无标题区块卡（只有底材 + 描边），给「两三行、不值得起标题」的分组用 |

**「我的」右上角设置**

纯齿轮 `IconButton`（`textMuted` 20 dp）→ `AppPillIconTextButton`（`ic_settings` + 「设置」，`colors.primary`）。
理由：图标是唯一线索，猜不中就等于没有入口；顺带把 `ProfileActionRow` / `ProfileSettingsCard` 私有实现
换成 `AppListRow` / `AppSectionGroup`，与设置页共用一套行。

**搜索面板「收起」**

整行透明文字 + 18 dp 箭头 → 底部居中的 `AppPillIconTextButton`（`KeyboardArrowUp` + 「收起」，主色 + 描边），
仍固定在面板下边界、不参与中间滚动区。底部条高度按 48 dp 热区算，只吃 8 dp 顶部留白。

**软件设置（`AppSettingsScreen`）**

| 项 | 旧 | 新 |
| --- | --- | --- |
| 主题 | `HealthDietProTheme`（绿），与「我的」两套色 | `HealthDietProTheme { AppTheme(...) }` 嵌套：外层保留字号倍率注入 `LocalDensity`（文字显示页要有反馈），内层接管颜色与排版 |
| 结构 | 11 行平铺 + 默认 `HorizontalDivider`，语言 / 通知 / 缓存 / 深色 / 关于混排 | 五张 `AppSectionCard`：常规（语言 / 深色模式 / 每周起始日）、显示（文字显示）、通知与权限（消息通知 / 权限）、存储（清除缓存）、关于（版本） |
| 行 | `SettingRow`：`titleLarge` 24 sp 标题 + `titleLarge+6dp` 图标 | `AppListRow`：`body` 16 sp、图标 20 dp、副标题 `caption`、尾值只给「当前值」、箭头只给「会切页」 |
| 深色模式 / 每周起始日 | 点一下就地循环到下一个值（三态循环要连点才知道有哪些选项） | 行只显示当前值，点开 `AppModal` 单选一次列全；深色模式仍走 `AppCompatDelegate` 立即生效 |
| 清除缓存 | 与普通行同形 | 尾值显示占用，不给箭头——点它是确认框，不是下一层页面 |

**子页同步**（`LanguageSettingsScreen` / `AboutScreen` / `TextDisplaySettingsScreen`）

设置根页换色后，这三页若继续沿用 `SettingRow` 会出现「24 sp 行标题 + 旧色板」的断层，故一并换行：
语言 = 单选卡；关于 = 居中标识 + 主色描边「检查更新」+ 资料与声明卡；文字显示 = 字号 / 溢出两张卡。
新增 `ic_language` / `ic_refresh` / `ic_dark_mode` 三枚图标。

**用户设置同步**（`UserSettingsActivity`）

首屏三项目录、单位偏好、提醒开关一并换成 `AppSectionGroup` + `AppListRow`，避免同一 App 里
「软件设置 56 dp 行 / 用户设置 24 sp 行」两种行高并存。

### 8.15 设置分组隐去标题 + 长按说明 + 单项设置统一 + 来源就地展开（第十五轮）

**分组标题不进界面**

五张 `AppSectionCard`（卡内图标 + 组名）→ 五张 `AppSectionGroup`（无标题），
卡与卡间距由 `space.3`(12 dp) 提到 `space.4`(16 dp)。分组关系仍写在代码里（一张卡 = 一组），
但用户看到的只是「一张张被间距分开的卡」。理由：分组名是给维护者看的目录，
「常规 / 显示 / 通知与权限 / 存储 / 关于」这类词本身没有信息量，却要在每张卡里占一行 28 dp 的标题区，
并让每张卡的第一个可点区域被标题挡住。

**说明改为长按浮层（Tooltip）**

所有设置行的 `subtitle` 一律撤掉，说明文字改走 `AppListRow(tooltip = …)`：

| 项 | 规格 |
| --- | --- |
| 触发 | 长按该行（移动端无悬停，取 §4.1 手势表的「长按看补充说明」） |
| 形态 | 气泡：`panelRaised` 底 + `borderStrong` 1 dp 描边 + `radii.label` 圆角 + `space.2` 投影；文字 `caption`/`text`，最大宽 280 dp |
| 箭头 | 12×6 dp 三角，锚定在触发行中心；朝内那 1 dp 压到气泡边框上盖掉直线（§5.2-1 要求 Tooltip 带锚点箭头） |
| 位置 | 优先挂行上方，上方不足两成屏高时改挂下方；水平居中并按屏幕留白 `space.4` 收边 |
| 关闭 | 4 s 超时自收（§5.1-1「超时关闭」）、再长按可与页面操作并存（非阻断、不抢焦点、不拦返回键） |
| 无障碍 | `onLongClickLabel` = `common_tooltip_long_press`（「查看说明」） |

组件：`common/ui/designsystem/AppTooltip.kt`（`AppTooltipPopup` + `TooltipPositionProvider`）。
行高因此回到一行 56 dp，设置列表能在一屏里看到更多选项。

**语言 / 深色模式 / 每周起始日统一为一个弹层**

三者同属「从固定选项里挑一个」，此前语言单独开一页、另两项弹层，是唯一的不一致项。
现统一为：行只显示当前值 → 点开 `AppModal`（标题 + 说明 + `AppRadioRow` 列表，一次列全）→ 选中即生效并关闭。
`SettingsPicker` 枚举把这三项的参数收在一处，弹层代码只有一份。
`LanguageSettingsScreen` 与 `SettingsRoute.Language` 随之删除（连同 `onOpenLanguage` 参数、`values` 里已失效的
`settings_section_*` 六个字符串）。

**来源与参考改为就地展开**

`ReferencesScreen` 整个删除。「来源与参考」这一行改成可展开行（`AppListRow(expanded = …)`，
下箭头随状态旋转 180°），展开后 DRIs 与 WHO 两条直接缩进列在父行下方（缩进到与父行文字同一条起始线
= `space.3 + 20 dp`），各带 `ic_open_in_new` 外链图标，点击直接开浏览器。
展开/收起用 `expandVertically + fadeIn` / `shrinkVertically + fadeOut`，时长 `motion.base`。

用户设置页同步：`user_settings_record_features_desc`、`user_settings_storage_unit_desc` 也改为长按说明，
两个设置页不再出现「有的行两行字、有的行一行字」。

### 8.16 选项弹层对齐免责声明 + Tooltip 收尾闪烁 + 「我的」纳入长按说明（第十六轮）

**单项设置的选项弹层改用 `AlertDialog`**

语言 / 深色模式 / 每周起始日此前用 `AppModal`（`radii.container` 4 dp 圆角、`fillMaxWidth` 贴边宽卡），
与【免责声明】的 `AlertDialog`（28 dp 圆角、屏幕居中、窄卡）两套观感。现三项统一改用 `AlertDialog`：

| 项 | 规格 |
| --- | --- |
| 圆角 | `shape = RoundedCornerShape(28.dp)`，与 Material 3 `shape.extraLarge` / `AlertDialogDefaults.shape` 一致（写死，避免主题换 shapes 后走样） |
| 位置 | 对话框自身在窗口居中，宽度取平台默认（不再 `fillMaxWidth`） |
| 标题 | `titleS` + `colors.text`，与规范其它弹层同一字号 |
| 说明 | 卡片描述降为 `caption` + `textMuted`，放在选项上方一行 |
| 选项 | `AppRadioRow(horizontalPadding = 0.dp)`：对话框内容区已有 24 dp 左内边距，行内不再二次缩进；分隔线 `leadingInset = false` |
| 操作 | 单个「取消」文字按钮（选中即生效并关闭，不需要「确定」） |

`AppRadioRow` 因此新增 `horizontalPadding: Dp? = null`（默认取 `space.3`，贴卡片时不变）。

**Tooltip 消失前闪烁**

自动收起的浮层如果不给预告就消失，用户会以为是自己误触或内容没读完。改为：
淡入 120 ms → 停留 2400 ms → **闪两下**（alpha 1 → 0.25 → 1，单程 260 ms，共 2 轮）→ 收起，总时长约 3.5 s。
闪烁由 `AppTooltipPopup` 内部 `Animatable` 驱动（`Modifier.alpha`），生命周期也收进组件里，
`AppListRow` 只保留「长按 → 显示 / 收起」的状态，不再自己计时。

**「我的」页三行纳入同一套长按说明**

`BMI` / `用户设置` / `导入导出用户数据` 三行的常显副标题撤掉，改用 `tooltip`：
说明文字分别是 `bmi_entry_desc`、`profile_user_settings_desc`、`profile_archive_data_desc`。
至此「我的」「用户设置」「软件设置」三处的行都是同一形态：图标 + 标题 + 当前值，长按才出解释。

### 8.17 Tooltip 生命周期下沉到状态对象（第十七轮）

**现象**：在 A 页长按出说明 → 进 B 页（说明随之看不见）→ 退回 A，说明又亮一下再自己消失。

**根因**：不是「重放动画」这种小问题，而是状态所有权错位。

1. 浮层的显隐原本是行内一个普通 `remember { mutableStateOf(false) }`；
2. 切到 B 页（另起 Activity）时，A 的 composition **不销毁**，只是 Activity 走到 `ON_PAUSE`；
3. 浮层的「淡入 → 停留 → 闪两下」跑在 `LaunchedEffect` 里，帧时钟随窗口不可见而停摆，
   动画停在半路，`onDismiss` 永远不会被调到；
4. 回到 A 页，帧时钟恢复，动画从暂停处继续 —— 于是已经「关掉」的说明又亮一遍，再走完收尾消失。

**修法**：把「显隐 + 锚点 + 生命周期」收进 `AppTooltipState`，调用方拿不到会残留的裸布尔值。

| 项 | 规格 |
| --- | --- |
| 状态 | `AppTooltipState`（`@Stable`）：`isVisible` 对外只读，只能经 `show()` / `dismiss()` 改；`updateAnchor(Rect)` 记录触发行窗口坐标 |
| 构造 | 唯一入口 `rememberAppTooltipState()`，内部 `remember(lifecycleOwner)` 建状态并挂 `LifecycleEventObserver` |
| 收尾 | 宿主 `ON_PAUSE` → **立即 `dismiss()`，不走闪烁**（那会儿已经看不见了，闪给谁看；回退时也不该补放） |
| 兜底 | `AppTooltipPopup` 另读宿主是否 `RESUMED`（`rememberIsHostResumed()`）；不在前台就 `dismiss()` 且不绘制，任何漏网的 `show()` 也留不到下次回来 |
| 组件签名 | `AppTooltipPopup(state: AppTooltipState, text: String)` —— 删掉 `anchorBounds` / `onDismiss` 两个参数 |
| 行 | `AppListRow` 只持有 `rememberAppTooltipState()`，长按 `show()`、布局时 `updateAnchor()`，不再自己管显隐 |

**为什么不再提供「布尔值 + 回调」的重载**：那样每个调用点都得自己接生命周期，漏一处就复现本 bug。
状态对象自带收尾后，「切页回来又亮一下」在 API 层面就写不出来。

### 8.18 四个入口图标重画（第十八轮）

四处图标此前都是「借用」来的，语义和位置对不上：

| 位置 | 原图标 | 问题 | 新图标 |
| --- | --- | --- | --- |
| 我的 · BMI | `ic_chart`（折线图） | 与「趋势/统计」共用，用户以为进去看曲线 | `ic_bmi`：**开环刻度盘 + 单根指针** |
| 记录 · 基础档案 | `ic_pk` | 来源不明的占位图（四角括号套一个 X），和「档案」毫无关系 | `ic_basic_profile`：**证件卡 + 人像 + 两条信息行** |
| 记录 · 日常记录 | `ic_list`（通用列表） | 全项目 10+ 处在用，读不出「每天记一笔」 | `ic_daily_record`：**日历 + 勾**（沿用 `ic_event` 的日历骨架） |
| 我的 · 导入/导出用户数据 | `ic_export`（单向向上箭头） | 只说了「导出」这半边 | `ic_import_export`：**左右并列的上下箭头**（左升右降） |

**画法约定**（新增图标照此执行）：

- 24×24 视口、实心填充、笔画视觉粗细 2 dp，与既有 Material 实心图标同网格；
- 外接宽度对齐到 18 dp（3–21 区间），同一列表里视觉重量一致，不出现「一个大一个小」；
- 需要镂空时用 `android:fillType="evenOdd"` + 同一 path 的内外子路径，不叠两个 path 去「描边」。

**BMI 为什么不是满环**：满环 + 单指针在 20 dp 下会被读成钟表（历史 / 时间），加底部 60° 缺口后才成立为仪表盘；
指针也不顶到外环、不画轴心点——小尺寸下轴心会糊成一个凸起。

**导入/导出为什么是上下箭头而不是左右箭头**：左右互指（`swap_horiz` 那种）读作「交换 / 重排顺序」，
和「数据进出本机」无关；上下箭头才对上「出去 / 进来」。两枚箭头左右并列——左枚朝上（导出）、右枚朝下（导入），
杆宽 2 dp、头宽 7 dp、外接 3–21 与其余三枚同宽，中间留 4 dp 空隙，20 dp 下不会粘连。

**改动落点**：`ProfileScreen`（BMI、导入/导出）、`RecordUiState.defaultRecordSections()`（基础档案、日常记录）。
`记录功能启用` 页的分节图标复用同一份 `titleIconRes`，因此自动同步；`ic_pk` 已无人引用，删除。

### 8.19 语义专用图标与启动图标候选（第二十轮）

**三枚语义专用图标**（与通用图标分开，不复用）：

| 用途 | 图标 | 画法要点 | 为什么不能复用 |
| --- | --- | --- | --- |
| 设置 · 清除缓存 | `ic_broom_clean` | 长柄 + 一道箍 + 宽扁帚头（顶宽 6、底宽 10、高 8）+ 底部三缕毛（evenOdd 镂空，缝底伸出轮廓外避免共边毛刺） | `ic_broom` 是拖把/刷子形，记录页「排便」在用 |
| 我的 · 出生日期 | `ic_cake` | 单层蛋糕 + 顶边四道 r=2 奶油弧 + 蜡烛 + 火苗 + 托盘 | `ic_birthday` 是日历，表达「某个日期」，时间选择/范围筛选在用 |
| 我的 · 导入导出 | `ic_import_export` | 左右并列、**等高对齐**的上下箭头，尾端各接横杠 | 见下 |

**导入/导出两枚箭头必须等高对齐**：两枚箭头的竖向起止都落在 3–21（18 dp），头在各自一端、横杠在各自尾端。
早前一版让左枚只占上半、右枚只占下半（各 14.6 dp、错位 3.4 dp），整枚图标读成「两枚箭头交错」，小尺寸下看着是歪的。
横杠宽度取箭头头宽的 0.75（5.2 dp）：留缝会像浮着的方块，等宽又会读成「⊥ / T」几何符号。

**启动图标（观颐）v9 已落地**：最终稿是矢量自适应图标，见 `mipmap-anydpi/ic_launcher.xml`
→ `drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml` / `ic_launcher_monochrome.xml`。
设计过程的候选位图（540 单图、圆/圆角/方三种遮罩、192→48 px 缩放、深浅桌面实景、汤色 × 纹色三档）
属一次性草稿，已清理；下方构图与配色的文字描述即为重建依据。

构图一句话：**俯视正对的一只陶白碗 —— 椭圆碗口 + 碗里一泓谷物黄的汤 + 鼓腹碗壁 + 一个扁椭圆圈足 + 碗壁四道自然绿的圆弧刻纹**。
已废弃的六代：v2「杏仁眼 + 瞳光 + 外倾 11°」（凝视感过强）、v3「近乎直线的刻纹」（太像平行直线，透视不成立）、
v4「正弦波浪刻纹」（起伏让线条读成弹簧/胡须）、v5「缺口等宽 + 直条底座」（像平面贴花，不像回转体）、
v6「弧度过平」（弓高只有碗口的 1/3，四道线读作贴在一个平面上）、
v8「碗身上半黄 / 下半绿」（两色在碗腹中部对切，看什么都像贴了双色纸）。

- **卦象**：颐卦（䷚ 山雷颐）自下而上 **阳、阴、阴、阴、阴、阳**。碗壁只画六二～六五这 **四道阴爻**；
  上九＝**碗口沿**、初九＝**圈足底座的下沿**，两条阳爻不另画线，由器物本身承担。早前「碗内三爻」画成阳-阴-阳，那是离卦，不是颐。
- **配色只落在两处，其余全部留白**：碗体（含圈足）陶白 `#F3EDE0`，剪影本身承担初九、上九两条阳爻；
  **汤面＝谷物黄 `#E2BB5E`**（颐卦「口中之物」的具象，也是画面的唯一暖色焦点）；
  **四道阴爻＝自然绿 `#5A8A43`**（下卦震的青苗之象）。
  留白让四道绿纹成为画面里唯一的绿色，卦象反而最清楚 —— 这比给碗身上半截染黄更有效。
- **不要在碗身上做上下分色**：v8 试过「上半谷物黄、下半自然绿」，色界落在六四/六三之间的 `y=12.90`，
  几何上完全对得上卦体，但视觉上必然出问题：色界若画成水平直线，会与碗口、与四道弧线刻纹全部错位，
  整只碗退回「贴了一张双色纸」；若做成与碗口同族的弧（用 de Casteljau 切分碗壁贝塞尔，端点 `x=5.30/18.70`），
  它又只与相邻两道爻各差 0.65，读起来像「第三道爻压着一条色带」。留白方案没有这个问题。
- **色界/纹样都不能靠描边强化**：试过在两色交界处压 0.12 / 0.22 的深墨线（当釉线），两条都被读成**第五条阴爻**，
  直接毁掉「四爻成卦」的计数。扁平风下圈足也不能描边显椭圆，同一个原因。
- **绿纹的线宽要 0.42**（不是深墨版的 0.38）：`#5A8A43` 对米白的对比低于深墨，0.38 在 72 px 下会发飘。
- **四条阴爻必须端点等高、上下等间距 1.30**：基线 `y = 10.95 / 12.25 / 13.55 / 14.85`。
  间距不同（v4 是 1.35 / 1.25 / 1.10，越往下越挤）时，四条读不成「同一卦里的四爻」，只像碗壁装饰；
  等距之后即便缩到 48 px 也能认出是被打断的四道横画。整体位置取碗壁**中上部**（弧顶 12.22–15.81），
  距碗口内沿约 0.45、距圈足上沿约 0.9 —— 再往下会在收窄的碗腹上挤成一堆。
- **弧线弯量取 `k = 0.20`，这是「与碗口平行」与「读得出横」的交点**：`y = 基线 + sag·√(1−u²)`，`u = (x−12)/hw`，
  `sag = k × 该爻所在碗壁半宽`，不含任何正弦起伏。
  碗口自身的扁率是 `2.3/7.3 = 0.315`，几何上「严格平行」就该取 0.315 —— 但那样单爻弓高 ≈ 2.0，
  超过 1.30 的爻间距，四道弧互相咬合成括号/眉毛，阴爻彻底读不出来（见 `_arc_options.png` 第一张）。
  k = 0.20 时弓高 ≈ 1.15，弧度明显随碗壁走、又保得住「两截短横」。k ≤ 0.12 则弓高 < 0.75，退回「贴在一个平面上」的观感。
- **缺口宽度只由该爻的高度决定，不是按半宽等比**：`t = (y − 10.95)/3.90`，`缺口 = 2.30 + (1.85 − 2.30)·t`，
  实取 **2.30 / 2.15 / 2.00 / 1.85**。v6 用的是 `0.362 × 半宽`，虽然也在收，但碗腹下半收得快，
  最下一道被压到 1.73，收缩节奏和视线不符。按高度线性收窄才对得上「越往下越远」的直觉。
  缺口等宽则是另一回事 —— 那等于宣告四道线贴在一个平面上。
- **端点必须 butt cap**（不是 round）：圆头在 24 视口下是 1.7 dp 的实心圆点，四条并排会读成一排胶囊。
  端点距碗壁边缘留 `inset 0.90`（3.9 dp）：留太多会悬在碗腹中间显飘，贴边则被碗壁轮廓吃掉。
- **断口居中**，左右两段等长；断口太窄读成划痕，太宽两段就各自断开不成一爻。
- **圈足是一个扁椭圆，不是一条横条**：`cx12 / cy17.15 / rx5.0 / ry1.2`，`M7,17.15 a5,1.2 0 1 0 10,0 a5,1.2 0 1 0 -10,0Z`。
  水平圆在正投影下必然是椭圆，圈足的上下沿都必须是椭圆弧。椭圆的上半落在碗身里、被同色盖住，
  露出来的只有下弧 —— 这条下弧同时就是**初九阳爻**，比 v5 的「直条 + r0.2 圆角」更符合「俯视一只碗」。
  rx 取 5.0 而非更大：圆形遮罩半径 7.33，端点 `(±5.0, 17.15)` 到中心 7.18，再宽就会被圆边切掉两头。
  ry 取 1.2 是「看得出椭圆、又不把碗底撑成球」的上限（见 `_tray_options.png` 四档）。
- **碗身剖面**：碗口 14.6 宽（`rx 7.3 / ry 2.3`），碗壁 `C 19.8,11.2 18.3,14.9 → 16,16.7`（上半近竖直、下半才收，才有鼓腹），
  碗底 `C 14.9,17.5 9.1,17.5 → 8,16.7`（下凸 0.6）。碗底半宽小于 3 就一定读成漏斗/高脚杯。
- **安全区**：24 视口内容落在 4.7–18.35（＝108 dp 画布的 21.2–82.6 dp），横向贴住 66 dp 安全圆。
- 已知短板：48 px 以下四道绿纹糊成一段淡色纹理（见 `_sizes.png`），但「墨绿底 + 米白碗 + 一泓黄」的剪影仍成立；72 px 仍可数清四条。

配色：底墨绿 `#12261F` × 碗体陶白 `#F3EDE0` × 汤面谷物黄 `#E2BB5E` × 四道阴爻自然绿 `#5A8A43`。
落地：`ic_launcher_foreground.xml`（24 视口，**四个 path**：碗身白 → 椭圆圈足白 → 汤面黄 → 四道绿弧）、`ic_launcher_background.xml`（纯墨绿）、
`ic_launcher_monochrome.xml`（主题图标，碗身 + 椭圆圈足两个剪影，只取 alpha，与配色无关）；`mipmap-anydpi/ic_launcher(_round).xml` 三件套引用。
`ic_app_badge.xml` 是**同一套 path 合成的完整图标**（墨绿圆角方底 + 前景四层，24 视口 r=5.4），
专供「关于」页的居中标识使用 —— 自适应图标是三件套拼装，App 内引用不到整只，而用 `foreground` 配 `Icon(tint=)` 会把碗连同汤、刻纹一起刷成一个纯色实心块。
`mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` 下模板自带的 10 张 `ic_launcher(_round).webp` 已删除：minSdk 35 全部命中 `anydpi`，这些密度图是死资源。
生成器：`%TEMP%/gen_xml.py`（改常量 → 直接写回 XML；第二参数给 badge 路径则一并重生成），校验：`%TEMP%/verify_xml.py`（解析 XML 的 pathData 回渲染 192→48 px 与三遮罩）。
绿纹若在深色壁纸下嫌淡，可换深一档的 `#4F7A3C`（见 `_palette_options.png` 第二张）；再深就与背景 `#12261F` 同族、读成镂空了。

### 8.20 逐屏重构批次 A：睡眠 / 容器 / 事件 / 切换用户（第一次成批推进）

**背景**：此前是「一屏一议」，每轮只动一个页面，没有可复用的做法。本轮先立计划书
`docs/UI_REFACTOR_PLAN.md`（盘点全部剩余界面 → 分四级 → 定 9 步单屏配方 → 分 A～G 批），
然后按计划把**全部单块结构的简单屏**一次做完，用来验证配方本身是否成立。

**本轮新增的设计系统组件**（此前各屏各自手写，样式不一致的根源）

| 组件 | 取代了什么 | 关键约束 |
| --- | --- | --- |
| `AppFilterChip` | 各屏自配的 M3 `FilterChip`、`Surface` 套 28 dp 小胶囊 | 视觉 32 dp / 热区 48 dp；选中同时改底、描边、文字三样，灰度可辨 |
| `AppSwitchRow` | 软件设置、睡眠编辑器各自手写的「Row + Switch」 | 行高 ≥ 56 dp、整行可点、开关只承担状态；关闭态文字转 `textMuted` 但不整行变淡 |
| `BaseScreen` 页头字号 | `headlineLarge`（旧主题 36 sp / 新令牌 32 sp） | 改 `titleLarge`（24 sp），与 §2.3 的 `font.title` 一致；影响 43 个屏的页头，属统一纠偏 |

**逐屏改动**

| 屏 | 文件 | 主要改动 |
| --- | --- | --- |
| 睡眠设置 | `SleepSettingsScreen.kt` | 3×`SettingRow` → `AppSectionGroup` + `AppListRow`；`SegmentedButton` → `EqualWidthSegmentedTabs`；记录时机改 `AppRadioRow`（`horizontalPadding = 0.dp` 贴卡内边距） |
| 睡眠列表 | `SleepListScreen.kt` | 页头齿轮 → `AppPillIconTextButton`；卡从 `Surface`+10 dp 圆角改 `panelRaised`+2 dp 描边+4 dp 圆角；空态 → `AppEmptyState`（含「记一笔」出口）；筛选 → `AppFilterChip`；时长改 `numeric` |
| 睡眠编辑 | `SleepEditorScreen.kt` | 五块各一张卡（类别分段置顶 / 入睡与醒来 / 夜尿 / 备注 / 计时器）；`Switch` 行 → `AppSwitchRow`；`Icons.Filled.Add` → `ic_add` 图标资源 |
| 容器列表 | `ContainerListScreen.kt` | 两组筛选的「灰条套小胶囊」→ `caption` 标签 + `AppFilterChip` 横滚行；空态分「一条都没有」与「筛完没有」两种，文案与出口不同 |
| 容器编辑 | `ContainerEditorScreen.kt` | `EditorSectionTitle` 平铺标题 → 卡标题；容量与空质量控制块改 `AppSectionGroup`（卡标题会与字段标签重复，故不给标题）；场景标签 → `AppFilterChip` |
| 容器设置（3 子屏） | `ContainerSettingsScreen.kt` | 2×`SettingRow` → 分组卡；标签行从「一行一块灰底」改同卡内行 + 分隔线；容量查询读数区改「主数值（`numeric`+主色）+ 三条 `caption`」 |
| 事件页 | `EventScreen.kt` | 屏内套 `AppTheme`（它挂在 `MainActivity` 页签里）；空态与日期分组头收令牌；运动页签 → `AppEmptyState` |
| 切换用户 | `UserSwitchActivity.kt` | 排序分段 → `EqualWidthSegmentedTabs`；用户行改卡语言（当前用户在底色与描边上同时体现）；删除按钮 `size(40.dp)` → 默认 48 dp |

**共享组件的取值口径（新增一条硬规则）**

`ContainerCardRow` 被容器设置与「记饮食」的容器选择抽屉同时引用，后者本轮还没迁。
这类**跨批共享**的组件一律：颜色只读 `MaterialTheme.colorScheme`，间距 / 圆角 / 字号只读
`AppSpacingDefault` / `AppRadiiDefault` / `AppTypeDefault` —— 迁到 `AppTheme` 的屏取到令牌值，
没迁的屏取到旧主题值，两边都不串色。这条比 §8.1 的写法更明确，后续批次照此办理。

**已知限制与未做项**

- 未上机验收：深浅两主题、100/125/150% 三档字号、圆形/圆角遮罩均未在真机看过，仅编译通过（APK 16:17）。
- 睡眠与容器的 `AlertDialog`（删除确认）、`OutlinedTextField`（新增标签）仍直接用 M3 组件：
  与已完成的软件设置一致，等批次 G 统一换 `AppModal` 时一起收。
- 事件页的空态在时间筛选条下面只给一行说明，没有套 `AppEmptyState`：空间被筛选条占掉后，
  再放一张插图卡会比筛选条本身还重。
- `Percent` 滑轨（容量查询）保留 220 dp 固定高度，属自绘控件几何量，不走间距令牌（已在代码注释标豁免）。
- 薄壳屏（`HeightDetailActivity` / `WeightDetailActivity` / `BodyMetricRecordActivity`）暂不换主题：
  它们的屏体是 `BodyMetricDetailScreen`，属批次 E；单独换壳会让屏体半新半旧。
- 配方第 7 步（补状态）在睡眠与容器上只补到「空」，「加载 / 错误」两态依赖 ViewModel 暴露的
  加载标志，本轮未改 ViewModel（计划书明令纯 UI），记入后续。

**与计划书的偏差**：计划书 A 批原列 16 屏，实际完成 8 个屏文件（对应 11 个可导航界面）。
用餐详情、围度详情、截面/餐段小组件顺延至 A 批第二段，原因见计划书 §7 的 A-4 条。

**批次 A 修正：说明文字一律改长按 tooltip**

*问题*：A 批把「这项设置是干什么的」那句话放在了两种位置——`AppListRow.subtitle`（睡眠设置首页、
容器设置两条）与卡内首行常显 `caption`（默认睡眠设置 → 习惯记录时机）。结果：
①同一组设置里只有带解释的那一行高出一截，看起来像被特意强调；②卡标题与第一条单选之间被一句小字隔开，
读者会把它当成第一条单选的副标题；③同一套「我的」页已经在用长按 tooltip，两种表达并存。

*结论（写进计划书 §2.1，作为全项目规则）*：**判据是「删掉这句话，用户还知道这个选项是什么吗」**——
知道就是说明，走 `tooltip`，长按才出；不知道才是标题或当前值，留在行里。
必须常显、不收进 tooltip 的只有四类：空状态描述、表单 `supportingText`、随状态变化的副标题
（前置条件/失效原因/当前生效值）、危险操作后果。

*设计系统补的能力*：`AppSwitchRow` 与 `AppRadioRow` 新增 `tooltip`（`combinedClickable`：
点按仍是切换/选中，长按才是解释，单选行的 `role` 与 `selected` 语义手动补回）；
`AppSectionCard` 新增 `tooltip`，长按卡头的图标与标题区才弹出——用 `detectTapGestures` 而非
`combinedClickable`，避免给卡头加一层假的可点击涟漪；卡内右侧 `action` 按钮不受影响。

*本轮改到的 6 处*：睡眠设置首页「默认睡眠设置」、默认睡眠设置「习惯记录时机」卡头、
睡眠编辑器「开始计时」「系统计时器」两个开关、容器设置「自定义标签」「容量查询」两行。
文案 key 未变（三套 `strings.xml` 不用改），只是换了承载方式。

### 8.21 血糖界面一次重构（原定 F 批，提前）

**背景**：血糖本排在计划书 F 批（自绘图表，风险最高）。这轮直接一次做完 8 个可导航界面 + 1789 行图表。
开工前先把「字号特殊」的四处交用户定，避免返工。

**用户拍板的四处字号**

| 位置 | 现状 | 决定 |
| --- | --- | --- |
| 图表坐标轴刻度 | `textSize = 20f`（px，3 倍屏≈6.7 sp） | 按密度换算成 11 sp 当量，并同步调整绘图区内边距 |
| 数据页血糖数值球（112 dp） | 主数值 20 sp、单位 12 sp | **保持 20 sp**（不加到 24） |
| 图表页卡片文字 | 混用 12 / 14 / 20 sp | 压到 12 / 14 两级，卡片标题 14 sp 加粗 |
| HbA1c 录入页的「%」 | 16 sp | **保持 16 sp**，与输入框数字同高 |

**逐屏改动**

| 屏 | 主要改动 |
| --- | --- |
| 血糖主屏（记录/图表/事件三页签） | Activity 里 `HealthDietProTheme { AppTheme { ... } }`；页面底 `colors.bg` |
| 数据说明页 | 三段说明改 `AppSectionGroup`，免责声明降 `caption` |
| 设置页 | 4 条 `SettingRow` → 分组卡 + `AppListRow`，说明改 `tooltip`；免打扰未实现，去掉箭头也不再接收点击 |
| 数据来源页 | 空态换 `AppEmptyState`；间距/底色走令牌 |
| 来源编辑页 | 仅底色与 contentPadding 令牌化（表单本身已合规） |
| 提醒设置页 | 5 张自绘 `Surface` 卡 → `AppSectionGroup` + `AppSwitchRow`；开关行自带 s3 内边距，字段区单独补，避免开关比输入框多缩进 |
| 目标范围页 | `SettingRow` → 分组卡 + `AppListRow`；「推荐目标范围」从 `titleLarge`（24 sp）降到 14 sp 加粗；表格区间列等宽 |
| 血糖录入 / HbA1c 录入 | 裸表单收成 3 张无名分组卡（时间+数值 / 时段 / 来源+备注）；格式与参考范围留在卡内常显（填写过程中没有长按余裕） |
| 数据页列表 | 空态换 `AppEmptyState`；血糖与 HbA1c 数值列 `numeric = true`（小数点对齐）；「共 N 条」降 14 sp 加粗 |

**图表（`BloodGlucoseFixedWindowChart.kt`）**

1. **轴字号**：`axisPaint` 从 `textSize = 20f` 改为 `DrawScope` 扩展 + `11 * density`。
   不乘 `fontScale`：刻度要待在一个固定高度的绘图区里，跟随系统字号放大会互相重叠。
2. **绘图区内边距**：`ChartBounds` 的四边从 px 常量（`12/52/12/42`）改为 dp 折算
   （`2/30/18/34 dp`），并把 `density` 作为构造参数；`chartTimestampAt` 的同一次命中测试共用它，
   否则会出现「点得到、选不中」。图表高度 210 dp 下绘图区约 158 dp，7 条 y 刻度间距 26 dp，不重叠。
3. **时间标签去重**：11 sp 的 `MM-dd HH:mm` 比原 6.7 sp 宽约六成，逐刻度都画会连成一条。
   先画全部网格线，再按 `measureText` 实测宽度取舍标签（间距不足 6 dp 就跳过）。
4. **文字层级**：`labelSmall → caption(12)`、`bodySmall/bodyMedium → bodyS(14)`、
   卡片标题 `titleSmall → bodyS + SemiBold`；统计数值与八点表样例值改 `numeric`（16 等宽）。
5. **卡片底**：原来 `surfaceVariant.copy(alpha=.24)` 在纯黑底上几乎看不出是卡，改实色
   `panelRaised`；卡内子块（目标时间环、四张指标卡）降一档用 `panel`，圆角换 `radii.container/label`。
6. **热区**：目标时间环旁的帮助按钮原来 `size(22.dp)`，去掉手动尺寸，回到 M3 默认 48 dp。

**已知限制**

- 未上机：坐标轴在 3 倍屏的实际观感、深色主题下 `panelRaised` 实色卡的层次、150% 字号下时间标签的去重效果，
  都需要真机确认。
- 八点血糖表仍是固定 382 dp 高：内部靠 `weight` 分行列，改成 `heightIn` 会因无界高度崩溃；
  字号放大时格内数字靠单行省略收尾。
- 图表左侧的极值/目标标注（`x = 2dp`）仍画在绘图区内、会压住一点曲线：它们是读数锚点，挪到轴外需要再加一列宽度。
- 提醒页的开关与字段仍是「关掉开关也显示阈值」：改联动属于逻辑变更，本轮只做 UI。

## 8.22 全站收口（B～G 批一次完成）

**诊断**

血糖之外的界面仍是两套体系：`HealthDietProTheme` 自带一份 `lightColorScheme`/`darkColorScheme` 与
`appTypography()`，`designsystem/AppTokens` 另有一份。设计系统组件按新令牌取色、业务代码按老
`MaterialTheme` 取色，同一张卡里出现「卡片是新色、文字是老色」。逐屏套 `AppTheme` 能解决，
但 19 个 Activity 各套一层，既重复又容易漏。

**改动**

1. **G-1 主题只有一个入口**。`HealthDietProTheme` 现在只做两件事：把系统字号倍率塞进 `LocalDensity`、
   把剩余全部交给 `AppTheme`。它自己不再持有任何色值或字号。屏内不必再套 `AppTheme`，
   历史上已套的保留也无害（同值覆盖）。这一步让 19 个 Activity、约 40 个屏同时进入新体系。
2. **A-7**：用餐详情的时间卡 → 无名分组卡，食物行改实色 `panelRaised` + 描边（原来半透明底在纯黑上读不出边界）；
   围度部位选择器 6 行 `SettingRow` → 一张卡 + `AppListRow`，说明改 tooltip；
   容器选择抽屉 `AlertDialog` → `AppBottomSheet`（`initialSnap = Half`）。
3. **B 批**：血压、疾病、药品目录、饮水四处列表空态统一为 `AppEmptyState`（结论 + 怎么办 + 主出口）；
   饮水设置 4 行、血压参考页 1 张 `Card` 换成 `AppSectionGroup` + `AppListRow`；
   「提醒」这类未上线项保留常显副标题说明原因，不用 tooltip 藏起来。
4. **C 批**：饮食设置 4 行设置项卡化；「餐前/餐后」原来是并排两个 `SettingRadioRow`，
   改为卡内纵向 `AppRadioRow`——互斥选项并排会读成「分段控件」，但它们并不切换下方内容；
   食物条目表单 `AlertDialog` → `AppModal(dismissOnScrimClick = false)`；饮食首页空态换组件。
5. **D 批**：地区三级下钻与物价物品选择两个 `AlertDialog` → `AppBottomSheet`。
   为此给抽屉新增 `contentScrollable`：内容区默认是 `verticalScroll`，放 `LazyColumn` 必崩；
   传 `false` 时把滚动权交给调用方，同时不画上下渐隐（渐隐读的是抽屉自己的滚动位置，与列表无关）。
6. **E 批**：个人信息编辑与 BMI 详情复核——两屏本来就是全令牌 + `AppSectionCard` 卡化（4 / 8 处），
   本轮不动结构，只确认没有回退。
7. **F-2 批**：营养表、饮食统计、血糖事件分析共 60 余处 `MaterialTheme` 换成令牌。
   卡内局部空提示（甜甜圈无数据、某天无分段）保留一行小字：整块空态会把卡撑开。
8. **G-2 债清理**：删除 `SettingRow` / `SettingSwitchRow` / `SettingRadioRow` 三个旧组件（已无引用，
   备份在 `%TEMP%/legacy_settingrow_backup`）；`ui/test` 与三个预览页一并令牌化。

**新增 / 修改的共享组件**

| 组件 | 变更 |
| --- | --- |
| `AppBottomSheet` | 新增 `contentScrollable: Boolean = true`；`false` 时内容区不套 `verticalScroll`、不画渐隐 |
| `AppSectionCard` / `AppSwitchRow` / `AppRadioRow` / `AppListRow` | 上一轮已补 `tooltip`，本轮全部调用点按「说明进 tooltip」重写 |
| `AppEmptyState` | 成为列表空态的唯一写法，12 个屏在用 |

**门禁口径修订**

- 第 3 条「`MaterialTheme.typography` 命中 = 0」**只对屏文件生效**。`common/ui` 下的共享组件
  （`AppDataTable`、`RecordTimeRangeFilter`、图表基类等）继续读 `MaterialTheme` 是正确做法：
  主题收口后它取到的就是令牌值，而直接读 `LocalAppTokens` 会在没有 `AppTheme` 的宿主里拿到默认深色令牌。

**连带影响（需要真机回归）**

- 全站 `titleLarge` 由 28 sp 降到 24 sp、`titleSmall` 由 22 sp 降到 20 sp、`bodySmall` 由 12 sp 升到 14 sp。
  前者让页内标题更紧凑，后者让原本 12 sp 的说明文字变大——文字变大的地方最可能出现换行与截断。
- 页面底色从 `#FFFBFE`/`#1C1B1F` 变为令牌 `bg`，浅色下更接近纯白、深色下更接近纯黑。
- 未迁屏若用了硬编码色（129 处），现在与周围令牌色的对比会变化，属已知遗留。

**已知限制**

- 未上机。深浅两主题 × 100/125/150% 字号 × 常见屏宽的组合没有实测，文字换行是最大风险点。
- `bodySmall → bodyS`（12→14 sp）会让部分表格行变高，`AppDataTable` 的行高常量未同步复核。

## 8.23 饮食统计/趋势卡化 + 默认饮食习惯收尾（第二十一轮）

**诊断**

全站收口之后仍留下三处「同一功能页里两种排版」的问题：

1. **记饮食-趋势 / 统计**：页内 4～5 个功能块只有分组小标题（`SectionTitle`）飘在页面底色上，
   块与块之间只靠 16 dp 间距分隔，滚动时无法判断「现在这一步是哪个块」。
2. **添加食物**：食物主名字用默认内容色（`text`），与下方别名、右侧种类徽章同权重；
   「一级标签 / 二级标签」两个分类名与 chip 内容同色，读起来像第一个 chip。
3. **默认饮食习惯**：「重置全部」在顶栏是 `TextButton`、「重置当前」在正文里又是一个 `TextButton`，
   两处样式与热区都不一样；「默认时长」「时段默认区间」两个块只有一行标题，字段直接浮在底色上。

**改动**

1. **趋势 / 统计卡化**：`DietStatisticsTab` 的每一块都换成 `AppSectionCard`——
   【当日摄入】【餐次分布】【按餐次小计】/【营养素趋势】。
   选中日的餐次明细留在趋势卡内、前面加一条 `AppListRowDivider`，
   因为它是图表读数的下钻结果，另起一张卡会被读成并列的第二个功能块。
   **统计日期不套带标题的卡**：只有一行内容的功能块用无名分组卡 `AppSectionGroup` + 一行式，
   与「容器管理」「记容器」等页的行式入口同一写法（第二十二轮修正，见 §8.24）。
2. **卡内不再套同色面板**。`DietStatCard`、`MealNutrientTable` 各加一个 `nested` / `inCard` 开关：
   - `DietStatCard(nested = true)` 底材降到 `panel`（原来 `panelRaised` 半透明，放进 `panelRaised` 卡里看不见边界）；
   - `MealNutrientTable(inCard = true)` 不画面板，改用分隔线分段，行内容抽成 `MealNutrientRowContent` 供两处复用。
   用餐详情页仍走原样（`inCard = false`），它是「卡 + 表格」的并列结构，没有外层卡片。
3. **添加食物取色**（已按 §8.25 修正）：食物主名字（列表行与已选食物条）与 `CategoryFilterRow` 的分类名
   （「一级标签 / 二级标签」）统一用**正文色 `text`**。分类名的字号仍为 `caption`。
   主色是「可操作 / 当前选中」的语义，静态文本不得占用——否则分类名会被误读成可点的 chip。
4. **默认饮食习惯**：【默认时长】与【时段默认区间】各套一张 `AppSectionCard`，说明走 tooltip；
   「自定义」标记从块标题行挪到卡片 `action` 槽位；两个复位动作统一成 `AppOutlinedIconTextButton`
   （`ic_refresh` + 文字 + 描边）。位置见 §8.24 的修正：**「重置全部」通栏放在时段选择条之上**，
   「重置当前」跟在时段相关设置之后。
   `DietTimeRangeField` 底材由 `panelRaised 0.45` 改 `panel` + 细描边，在卡里才能读出「这是一个可点的格子」。
5. **记录页搜索占位**：`record_search` 由「搜索记录功能」改为「搜索记录功能，支持模糊搜索」，
   把「支持模糊」这条能力写在入口上，省得用户去试。

**经验**

- **判断「要不要卡」的不是内容多少，而是块与块之间有没有并列关系。** 趋势页的 4 个块是并列的读数视角，
  必须卡化；而餐次明细是趋势图的下钻，留在同一张卡里才不会读成两个功能。
- **卡内底材只能往下走一级。** `panelRaised` 卡里再放 `panelRaised` 半透明面板，视觉上等于没画；
  正确做法是降到 `panel`，或者干脆改成分隔线列表（列表语言本来就是「卡 + 分隔线」）。

## 8.24 浮层安全区与抽屉高度模型（重大缺陷修复，第二十二轮）

**症状**

1. 选容器抽屉在手机上底部被系统导航栏压住；
2. 添加食物弹窗「不是居中的」，内容一多还会一直画到导航栏下面。

**根因（两个，叠在一起）**

- **Dialog 窗口里的 insets 是 0**。浮层都跑在 `Dialog(decorFitsSystemWindows = false)` 里，
  这种窗口上报的 `WindowInsets.safeDrawing` / `navigationBars` 在部分机型、以及「抽屉里再开弹窗」
  的嵌套层级下会返回 0。`AppModal` 之前用 `windowInsetsPadding(safeDrawing)`，等于宣告「安全区 = 整块屏幕」：
  弹窗按**含状态栏和导航条的整块窗口**居中（视觉偏低），高度上限也是整块窗口——
  内容一多就顶到导航栏下面。
  `AppBottomSheet` 早先已经绕开了这条（用 `overlayNavigationBarPadding()`），但取值链的第一档
  是**浮层自己的 View**，同样可能返回 0，落到最后一档常量（手势 24 dp），三键导航就不够。
- **抽屉的高度模型本身会让内容压在导航栏下**。早先是「面板恒为满档高度 + 整体向下偏移」表示收放，
  面板底边因此跑到屏幕下方，**可见区域的下沿变成屏幕底边**——最后一行正好落在系统导航栏下面，
  三键导航时既看不见也点不中；同时抽屉里的「确定」操作条在半屏档整个落在可见区域之外。

**改动**

1. `SystemInsets` 新增**宿主 Activity 窗口**这一档取值，并把它排在最前：
   `Context.findHostActivity()` 剥掉 `ContextThemeWrapper` 后读 `activity.window.decorView` 的
   `navigationBars` / `statusBars` —— 宿主窗口的 insets 一定是真实值，Dialog 自己的窗口会报 0。
   同时新增 `overlayStatusBarPadding()`（取值链与导航栏那支相同），供居中弹窗算上边距。
2. `AppModal` 不再 `windowInsetsPadding(safeDrawing)`，改为
   `max(Compose safeDrawing, 浮层通道) + space.5` 显式算四边内边距：既修「偏低」，也修「画到导航栏下」。
3. `AppBottomSheet` **改高度模型**：面板高度 = 当前档位高度（不再恒定满档 + 位移），
   底边永远停在导航条之上，多余内容靠内部滚动。进场位移改用 `graphicsLayer.translationY`，不参与布局。
   代价：拖拽过程中内容随高度重新布局（滚动位置由各自的 `scrollState` 保存，不受影响）。
4. `AppModal` 新增 `contentScrollable: Boolean = true`（与抽屉同名参数、同语义）：
   传 `false` 时内容区不套 `verticalScroll`、不画渐隐，由调用方放 `LazyColumn(weight(1f, fill = false))`。
   列表在 `verticalScroll` 里拿不到上界，之前只能写死 500/540 dp——写死之后短内容也撑满（看着没居中）、
   长内容叠出弹窗。
5. 选容器抽屉**去掉底部「确定」操作条**，点选即生效并关闭（通用规范 §5.5 单选：「选中即生效，不需要确定」）。

**经验**

- **浮层不要问「自己这个窗口」要 insets**。Dialog 窗口是 `decorFitsSystemWindows = false` 的，
  它是不是报告 insets 取决于机型与嵌套层级；宿主 Activity 的窗口才是稳定答案。
- **「恒定高度 + 位移」不适合有底部操作条的抽屉**。位移意味着底边离屏，可见下沿变成屏幕底边，
  底部内容必然被系统栏压住。要么改高度，要么保证底部永远没有内容。
- **写死列表高度等于把「自适应」这层交给运气**。列表该拿「父给的上限」，弹窗才会随内容伸缩。

### 8.25 取色回退：静态文本不用主色

§8.23 第 3 条把「添加食物」的食物主名字、分类名（一级/二级标签）染成了 `primary`，
本轮全部回退为 `text`。

**理由**

- 主色在本项目里是「可操作 / 当前选中」的语义（按钮文字、开关、选中项、图标 tint）。
  静态文本占用主色，用户会以为它能点——尤其是紧挨一排 `FilterChip` 的分类名，
  染成主色后读起来像第一枚 chip。
- 食物名本来就承担「这一行是什么」的主信息，用正文色已经是最高优先级，不需要再靠颜色抬。
  强调层级由字号（`body`）和位置（首行）完成。
- 选中行的底材是 `primaryContainer 0.5`，正文色在其上的对比度也更稳。

**落点**：`DietFoodSheet` 两处（列表行、已选食物条）、`EditorFormComponents.CategoryFilterRow`。
分类名的字号保持 `caption`（比 chip 标签整体小一档，读作「组名」）。

**规则**：静态文本只能取 `text` / `textMuted` / `textSubtle`；`primary` 保留给可交互元素与选中态。

## 8.26 浮层让位改按「屏幕坐标」结算（第二十四轮，含一次返工）

§8.24 的浮层模型只解决了「面板高度」，第二十三轮修「选容器抽屉压导航栏 / 添加食物不居中」时
连踩两个坑，都出在 `Dialog` 窗口的坐标系上。

### 8.26.1 现场数据（Redmi K80 模拟器，1440×3200，density 3.5，三键导航）

| 量 | 值 |
| --- | --- |
| 状态栏 inset | 84 px |
| 导航栏 inset | 168 px（资源 `navigation_bar_height` 只有 84，不可信） |
| `DietRecordActivity` 浮层窗口 | `frame=[0,84][1439,3032]` —— **系统已经把它裁进安全区** |
| 同一窗口里 Compose 拿到的约束高度 | 3200（整屏），内容原点在屏幕 y=84 |

窗口被裁、约束又不裁，这就是「让位两次」的来源。

### 8.26.2 失败过的三种写法

1. **Dp 让位**：在宿主 composition 里把 px 折成 Dp 再塞进浮层，浮层密度与宿主不同
   （实测 3.5），48 dp 只让出 86 px。
2. **px 让位 + 居中**：`padding(top=84, bottom=168)` 叠在已被系统裁过的窗口上，
   面板整体下沉 84 px（上留白 168、下留白 2）。
3. **状态栏高度补偿偏移**：假设窗口「从状态栏下沿开始」，补偿量依赖窗口与屏幕高度差；
   该宿主窗口本来就是裁好的，补偿算出来是 0，面板仍按含导航条的整屏居中。

### 8.26.3 最终模型：`rememberOverlayWindowFit`

`SystemInsets.kt` 里新增 `OverlayWindowFit(usableHeightPx, centerShiftPx, bottomLiftPx)`，
用**窗口在屏幕上的原点** + 屏幕尺寸 + 两条系统栏 inset 换算，全部是 px：

- `usableHeightPx = min(窗口高度, 屏高 − 状态栏 − 导航条)`
- `centerShiftPx = (状态栏 + 安全区高/2) − (窗口顶 + 窗口高/2)` —— 居中弹窗用它把「窗口中心」挪到「安全区中心」
- `bottomLiftPx = 窗口高 − (屏高 − 窗口顶 − 导航条)` —— 贴底抽屉用它把面板底边停在安全区下沿

窗口位置在第一帧之后才准，且它不是 Compose 状态：内部用 `delay(50)` + `delay(250)` 重读两次。

落点：`AppModal`（居中 + 限高）、`AppBottomSheet`（档位高度按 `usableHeightPx` 算，底边按 `bottomLiftPx` 抬）。

### 8.26.4 实测结果

| 场景 | 宿主窗口 | 面板上下边界（px） | 结论 |
| --- | --- | --- | --- |
| 添加食物（DietRecordActivity） | 裁过 `[0,84][1439,3032]` | 169 → 2948 | 上留白 85、下留白 84，居中 |
| 选容器抽屉（同上） | 同上 | 1263 → 3032（60% 档） | 底边正好在安全区下沿，不再压导航栏 |
| 探针页弹窗（全屏窗口） | `[0,0][1440,3200]` | 738 → 2381 | 中心 1559.5，对安全区中心 1558 |
| 探针页抽屉（同上） | 同上 | → 3032 | 抬升 168 px，正确 |

**规则**：浮层里所有跟系统栏有关的量都用 px，并且必须问 `rememberOverlayWindowFit`；
不要再写「按导航条高度偏移」这类假设——窗口是否已被系统裁过，只能现场量。

## 8.27 对话框按钮、单位行与地区抽屉的三处统一（第二十五轮）

用户一次报了三条「同款不一致」，根因都是**同一个动作在不同地方用了不同控件**，改法都在组件/调用点本身。

### 8.27.1 确认对话框的两个按钮（`ConfirmDialog`）

- **问题**：`ConfirmDialog` 的「确认 / 取消」用的是 `TextButton`——浮层上只剩两行字，读不出能按，
  也和页面上「图标 + 文字 + 描边」的动作按钮不同款。
- **影响面**：搜索面板的「清空搜索历史 / 清空点击历史 / 清空使用历史」与设置页「清除缓存」共用它，
  所以只在 `ConfirmDialog` 里改一次，三处同时生效（不要在调用点各自补样式）。
- **改法**：两个按钮改 `AppOutlinedIconTextButton`（`ic_check` / `ic_cancel`），与 §8.22 的
  「重置全部 / 恢复默认设置 / 检查更新」同一款描边胶囊。

### 8.27.2 记物价「价格」里的两格单位（`PriceEditorScreen`）

- **顺序必须跟保存后的读法一致**：`PriceRecord.priceDisplay` 是 `priceUnit + "/" + sellUnit`（「元/斤」），
  所以左格放**物价单位**（下拉，`weight 0.72`）、中间放 `/`、右格放**售卖单位**（必填输入，`weight 1`）。
  早先是售卖单位在左、物价单位在右，读起来是「斤/元」，和列表卡片对不上。
- 斜杠是语言无关符号，写成 `PRICE_UNIT_SEPARATOR` 常量，不进文案资源；改动等于改全站单价读法。
- **不要只靠 `CenterVertically`**：行盒带整段降部留白，纯居中会让「/」的墨迹浮在输入框文字上面约 13 px；
  这里额外 `.offset(y = spacing.s1)` 补偿。实测（1440×3200、density 3.5）：格子边框 1379 → 1585（中心 1482），
  「/」墨迹中心 1486.5、「元」1484、「斤」1483，三者落在同一行。

### 8.27.3 「选择地区」抽屉顶部（`PriceRegionPickerDialog`）

- 地区名是「使用资料中的地区」这条入口的**实际取值**，不是补充注脚：`textMuted` → `text`。
- 「使用资料中的地区 / 使用当前定位」是两个并列的二级动作，由 `TextButton` 改 `AppOutlinedIconTextButton`
  （`ic_region`、通栏），与 §8.27.1 同款。
- 地区名缩进随按钮形态一起改：旧代码用 40 dp 去对齐文字按钮的图标位，按钮变通栏胶囊后那个缩进是悬空的，
  改成 `start = s3` 贴着胶囊左边缘。

**回归方式**：`assembleDebug` + 装机逐屏截图；`DesignTokenContrastTest` 通过。

## 8.28 继承色清零、测试页瘦身（第二十六轮）

三条互不相干，共同点是「项目里出现了不该出现的东西」：一条是**不该出现的颜色**，两条是**不该留着的界面**。

### 8.28.1 「没写颜色」的文本其实是纯黑（`AppTheme` + `PriceRegionPickerDialog`）

- **现场**：物价「选择地区」抽屉里，省列表 / 市列表的地区名看不清，深浅背景下都不行。
- **根因**：那两处 `Text` 只写了 `style`、**没写 `color`**，于是取 `LocalContentColor`。
  Material3 的 `MaterialTheme` **并不提供** `LocalContentColor`——只有 `Surface` / `Button` / `TextField`
  这类容器才会在更深的位置覆盖它，落在容器外的 `Text` 直接吃掉它的默认值 **`Color.Black`**。
  浅色底上是纯黑（勉强能读但没有层级），深色底上是黑底黑字（等于隐形）。
- **改法分两层，两层都要**：
  1. `AppTheme` 里在 `MaterialTheme` 内部补一层
     `CompositionLocalProvider(LocalContentColor provides tokens.colors.text)`——
     全局兜底，让「以后谁忘了写颜色」也只会拿到主题正文色，不会拿到纯黑。
     `Surface` / `Button` / `TextField` 的覆盖位置更深，行为不变。
  2. 省 / 市列表的 `Text` 显式 `color = tokens.colors.text`——**不能只靠兜底**。
     兜底解决的是「看不见」，显式声明才能表达「这里本来就该是正文色」，且不受外层容器换色影响。
- **规则**（写进 §2 取色约束）：`Text` 的 `color` 必须显式写，静态文本只有 `text` / `textMuted` 两档
  （强调语义时才用 `danger` / `success` 等语义色）；任何「不写 `color`、靠 `LocalContentColor` 继承」的写法
  都视为缺陷。`DesignTokenContrastTest.fallbackContentColorIsNeverPureBlack` 守住兜底色不为纯黑；
  全量检查靠评审 + `assembleDebug` 后的截图核对，静态扫描不在本轮范围。

### 8.28.2 删掉「Meal icon candidates」

- 那是早期挑餐次图标时的一次性对照页，图标选型早就定了（`MealPeriodIcon` 已经是正式组件），
  留在【测试】里只会让人以为还有什么待定项。
- 删除范围：`ui/test/MealIconCandidatesScreen.kt` 整个文件 + `TestLandingScreen` 的
  `onOpenMealIcons` 参数与入口按钮 + `MainActivity` 的 `TestPage.MealIcons` 枚举项、
  对应 `when` 分支和 import。**四个地方一起删**——只删文件会留下编译不过的 import，
  只删入口会留下永远进不去的 Activity 分支。

### 8.28.3 「组件与设计令牌」→「设计令牌」，删掉四块

- 更名：`compose_components_preview_title` 三个语言目录同步改（values / values-en / values-zh：
  `Design Tokens` / `Design Tokens` / `设计令牌`）。
- 删除的四块与去向：

  | 删掉的块 | 为什么删 | 现在看哪里 |
  | --- | --- | --- |
  | 字体大小滑动块 | 字号调节已是正式功能 | 设置里的字号设置 |
  | 确认对话框 | `ConfirmDialog` 已是公共组件（§8.27.1） | 任意删除确认弹窗 |
  | 文字溢出处理 | 【通用UI功能测试】有同款演示 | 通用UI功能测试 |
  | Compose 图表 | 【通用UI功能测试】有同款演示 | 通用UI功能测试 |

- 这一页剩下的只有 `DesignSystemPreviewSection()`——色板、间距、字号、层级、动效、按钮状态，
  即**令牌本体**；凡是「用令牌拼出来的组件演示」都不该挂在这里。
- 注意 `ComposeChartPreviewSamples()` **没有删**：它被 `CommonUiTestScreen` 引用着，
  图表演示的正确归属就是通用UI功能测试那一页。删页面内容时先 grep 引用，别把被别人调用的也一起清掉。
- 配套删掉 `compose_font_scale_section`、`compose_confirm_dialog_section`、
  `compose_text_overflow_section`、`compose_chart_section`、`compose_show_confirm_dialog`、
  `compose_confirm_dialog_title`、`compose_confirm_dialog_message`、`text_overflow_marquee_speed_desc`
  八个字符串（三个语言目录都要删）。

**回归方式**：`assembleDebug` 通过；装机走「测试 → 设计令牌」确认标题与只剩令牌内容、
确认测试落地页不再有「Meal icon candidates」；`DesignTokenContrastTest` 通过。

## 8.29 记物价：筛选归统计、单位全展开（第二十七轮）

用户一句话定了信息架构：**「记录界面只会显示每条原始记录，所以筛选要放到统计界面」**。
这不是搬控件，是给两页各自定死职责。

### 8.29.1 两页的职责与数据源必须分开

| 页 | 职责 | 数据源 |
| --- | --- | --- |
| 【记录】 | 流水账：全部原始记录，按「物品+地区+日期+单位」合并成区间卡 | `PriceUiState.allGroups`（**恒为全量**） |
| 【统计】 | 看汇总：物品级价格区间，带搜索与三个维度筛选 | `PriceUiState.visibleStats`（`filteredRecords` 之后） |

- 关键决定：**筛选只作用于统计页**。记录页若也跟着筛，用户从统计切回来会发现条数变少，
  第一反应是「记录丢了」，而记录页当时又没有任何筛选控件可以提醒他为什么少。
- 因此旧的 `visibleRecords / visibleGroups` 拆成 `filteredRecords`（统计用）与 `allGroups`（记录页用）。
  两页共用同一份 `records`，只差一层过滤，不存在两份数据源不同步的问题。
- 记录页也随之删掉了「筛选无结果」分支：它不再可能被筛空。

### 8.29.2 统计页的四项筛选

搜索框 + **年份** + **月份** + **地区**，同一个 `PriceFilterRow` 排版：左侧固定语义标签（`caption`/`textMuted`，
`widthIn(min = s6)` 让四行 chip 起始位对齐），右侧横滚 chip 行（chip 多于一屏不撑破卡片）。

- **年份**：只列记录里真实出现过的年份（倒序），不给一个永远筛不出东西的空年。
- **月份**：1–12 固定全列，不按数据裁剪——「这个月没记过」本身就是用户想知道的信息。
- **地区**：沿用多选；「全部」chip **常驻**（第二十八轮改，见 §8.30.1），选中态＝一个地区都没选。
- 年份/月份是单选，点已选中的 chip 等于取消（回「全部」）；顺序按「时间 → 地点、粒度从粗到细」。
- 「清除筛选」放 `AppSectionCard.action`（卡头右上角），`hasFilters` 为假时不渲染——没有可清的东西时摆按钮是噪音。
- 空态文案跟着改：旧文案只说「换个关键词、或取消地区筛选」，现在补上年份/月份
  （`price_filter_empty_hint` 三套同步改），否则用户不知道还有两个维度卡着。

### 8.29.3 统计条目：单位全展开，不折叠

- 旧实现 `ranges.take(2)` + 「另有 N 种单位」。删掉：**单位数正是这一行要回答的问题的一半**，
  藏起来等于只答一半，还多一次点击。
- 现在一个物品下每个「物价单位/售卖单位」各占一行，全部列出，行高随单位数自然增长（`heightIn(min = rowMinHeight)`）。
- 数字与单位之间从 `s1` 加到 `s2`：`元/个` 这类单位文字短，4 dp 会读成 `0.5–2.2元/个` 一整串。
- 一律**不做单位换算**（缺密度、缺规格，换算必然是猜的），所以只分行、不合并、不给合计。

**实测**（Redmi K80 AVD，1440×3200）：注入 8 条记录，肉包子有「元/个 0.5–2.2、元/斤 6–8、元/笼 12–15」
三种单位，三行全部显示且右对齐；筛「2026 + 3 月」后只剩北京 2 条（1.8–2.2 元/个、6.5–8 元/斤），
同一切回【记录】仍是全量 8 条。

**回归方式**：`assembleDebug` 通过；装机核对记录页无筛选控件、统计页四项筛选可用、
多单位物品每单位一行；`DesignTokenContrastTest` 通过。

## 8.30 记物价：地区「全部」常驻 + 【物品历史】（第二十八轮）

用户两句话，一个改筛选、一个加详情页。

### 8.30.1 地区「全部」为什么必须常驻

年份和月份是**单选**：没选中就等于「全部」，chip 可以隐含表达。
地区是**多选**：选了「北京 + 上海」时，若没有「全部」，用户只能逐个点掉，点完还要确认自己没漏。
所以「全部」常驻，选中态＝`selectedRegions.isEmpty()`，点它走 `clearRegionFilters()`。
它和年份/月份那两行是同一句话：「全部」永远在第一个位置。

### 8.30.2 从统计行到【物品历史】

- 统计行**整行可点**，右端固定一枚 `ic_arrow_right`（`textMuted`，`s5`）：这是这条路由唯一的可点提示（§4.6）。
- 新路由 `PriceRoute.HISTORY`，只存**物品名**；物品键由 `priceItemKey()` 现算
  （`trim` + `lowercase`，与 `statsByItem()` 用的是同一个函数）。两处各写一遍大小写规则，迟早有一处漏。
- 数据源是 `filteredRecords` 再按物品收口（`PriceUiState.historyRecords`），**不是全量 `records`**：
  统计行上写着「3 个地区 · 7 条记录」，点进去必须正好是这些，否则统计看起来就是错的。
  代价是筛选被继承进详情页，所以页面里**不再摆第二套筛选控件**，避免「这里改了、回去又变了」。
- 两个部分用 `DetailTabBar`：**记录列表**（默认）/ **物价图表**。
  默认给列表，是因为点进来的常见动机是「这几次分别在哪买的、多少钱」；要看趋势再切一格。
- **tab 状态提到了路由层**（`priceTabId`）。第一版把它留在页面内用 `rememberSaveable`，
  实测「统计页 → 物品历史 → 返回」会掉回【记录】页：`rememberSaveable` 只在进程/配置重建时恢复，
  救不了「离开组合树」这种情况。

### 8.30.3 物价图表：自绘，不复用 `ComposeBaseChart`

`ComposeBaseChart` 带设置、全屏、平移、双轴那一整套，适合营养/体重这类密集时序；
物价是**稀疏**的（一个物品几条到几十条），那套控件全是噪音。所以自绘一个 `PriceHistoryChart`：

- **一个售卖单位一条线**，不换算（理由同 §8.29.3）；图例带文字标签，不只靠颜色。
- **折线连每天的价格下限，竖线画当天的价格区间**，单值记录退化为一个点。
  只连下限是因为「下限」是实际录到的数；区间用竖线表达，不去算术平均数（那会造出一个没被记录过的价）。
- **横轴按真实日期比例排布**，不是等距索引：两个月没记，图上就该留出那段空白。
- 纵轴取 1/2/5×10ⁿ 的步长（`niceStep`），上下各留 12% 余量；横轴只给第一个标签带年份，其余 `MM-dd`，
  标签放不下就跳过而不是叠字。首末点各留 10 dp，避免点被卡片边缘切掉。
- **色板顺序刻意避开 `energy` + `carbs`**：两者在深色面板上一篇浅棕、一篇琥珀，三条线时几乎分不出；
  改为 `carbs → fiber → organicAcid` 起手后（琥珀 / 青绿 / 紫）一眼可辨。
- 画布加 `semantics { contentDescription = … }`：「价格走势，共 N 种单位：元/个, 元/斤…」，读屏不再是空白。

### 8.30.4 实测（Redmi K80 AVD，1440×3200）

注入 11 条记录（肉包子 7 条 / 3 地区 / 3 单位，土豆 3 条，大米 1 条）：

| 动作 | 结果 |
| --- | --- |
| 统计页地区行 | 「全部」常驻且为选中态；点「北京市」→ 共 2 种，点「全部」→ 共 3 种 |
| 统计行 | 肉包子「3 个地区 · 7 条记录」，右端箭头，点开【肉包子 · 物品历史】 |
| 历史页记录列表 | 「共 7 条」，与统计行一致；日期倒序、地区与单位分行右对齐 |
| 历史页物价图表 | 元/个（琥珀，4 点折线）、元/斤（青绿，2 点）、元/笼（紫，单点＋区间竖线） |
| 筛选后进详情 | 先筛「北京市」再点肉包子 → 历史页「共 5 条」 |
| 返回 | 停在【统计】页，筛选仍在（「清除筛选」可见） |
| 浅色主题 | 刻度、日期、三色线、图例均清晰；点带面板底色描边 |
| 单条记录（大米） | 单点居中，横轴只有一个日期标签 |

顺带修掉一处遗留：上一轮删了 `PriceItemStats.primary`，`PriceStatsTest` 还在引用它（`assembleDebug` 不编译测试，所以没暴露）。
改成 `ranges.first()`，并新增 `PriceHistoryTest`（6 项：同日多地区取包络、点按日期升序、单位不共线、单位排序、空输入、物品键归一）。

**回归方式**：`assembleDebug` 通过；`PriceStatsTest` / `PriceHistoryTest` / `PriceRecordTest` 共 29 项通过。
`FoodNutrientRepositoryTest` 有 6 项失败（调味料标签、油脂 tag、密度缺失等），是**既有的食材数据断言问题**，
与本轮改动无关，未在本轮处理。

## 8.31 物品历史：单屏上下 + 图表可缩放 + 点按浮动窗（第二十九轮）

§8.30 的两 tab 结构（记录列表 / 物价图表）在本轮合并成一屏：**走势在上、明细在下**。
用户的原话是「数据量变多时内容就非常容易挤在一块」，所以要解决的是图本身看不下去，
而不是再加一个 tab 把图挪走。

### 8.31.1 为什么是上下而不是两 tab

两个 tab 的代价是「图和数对不上」：在图上看到 6 月有个尖，得切到列表再找回那一天。
同一屏上下摆，图形和明细是同一条时间线的两种读法，眼睛不用离开位置。

高度分工是硬约束：

- **走势卡固定高度**（图 232 dp + 图例 + 说明），不参与滚动；
- **历史记录卡 `Modifier.weight(1f)`**，内部 `LazyColumn` 自己滚。
  这样翻多久明细，图都停在原地——「看着图往下翻明细」才成立；
  换成一屏整体 `verticalScroll` 的话，图会被顶出屏幕，等于又回到了「看不到图」。
- 页面**没有外层滚动**：手在图上时，纵向拖动归图表（平移价格轴），不会被父容器抢走。

顺带一个坑：上一版 `BaseScreen(includeNavigationBarPadding = false)` 是为底部 tab 栏让位，
tab 栏没了以后这句话会让列表最后一行压在三键导航栏下面（装机第一版截图可见）。
改为默认 `true`，由 `innerPadding` 统一让位。

### 8.31.2 「X 条 / 共 M 条」两个数各是什么

- **X** = 列表实际渲染的条目数（与列表同一套合并：地区 + 日期 + 单位）；
- **M** = 这个物品在**不受统计页筛选影响**时的条目数（`PriceUiState.historyItemRowTotal`）。

两个数用同一把尺子（都是合并后的行），比的才是「筛选藏掉了多少条」。
拿原始记录条数当分母会和列表行数对不上——一行可能由多条记录合成。
历史页继承统计页的筛选、页面上又不摆筛选控件，所以这行计数是用户唯一能看出「还有多少被筛掉了」的地方。

### 8.31.3 图表交互：缩放、拖动、重置

视窗用「占完整范围的比例」表示，而不是绝对像素（`ChartViewport(xStart, xSpan, yStart, ySpan)`）：

- 缩放以**两指中心为锚点**：锚点下的日期/价格在缩放前后停在原位，否则每缩一次数据就要重新找；
- 拖动按「内容跟手」：手指往右拖，视窗往左移；纵轴上拖是往低价方向看；
- 夹紧到 0–1，且横向最多 24 倍、纵向 8 倍：再放大刻度只剩一两个数，没有信息量；
- 全貌时拖动是**空操作**（没有余量可移），这是刻意的——否则数据会被拖出画面；
- 动过视窗后卡头右侧出现「重置」（`AppTextIconButton` + `ic_refresh`），
  只在这一刻出现：没缩放时它点了没反应，白占地方。

坐标轴跟着视窗重算：纵轴步长按**可见范围**取 1/2/5×10ⁿ（用完整范围算的话，放大 10 倍后只剩一两条刻度）；
横轴只列可见日期，放不下就跳过；拖到没有记录的空档时画一句「这段区间没有记录」，而不是留一片空网格。
数据超出的部分用 `clipRect` 裁掉，不会画到纵轴刻度或日期标签上。

**手势冲突**：点按与变换分两个 `pointerInput`。轻点不产生位移，变换手势不会消费按下事件，点按照常触发；
一开始拖动/缩放，变换手势把手势吃掉，点按自然不触发，同时把浮动窗收掉（留着会挡住刚拖到的数据）。

### 8.31.4 点按浮动窗

- 命中判定按**屏幕距离**（24 dp 内找最近的关键点），不是按横轴区间：同一天两条线挨着时，
  用区间判定会选中上面那条。
- 内容四行：日期（`yyyy-MM-dd`）/ 价格区间 + 单位 / 地区 / `合并 N 条`。
  `合并 N 条` 只在 `sampleCount > 1` 时出现——同一天多地各记一次时，图上只有一个点，
  不说清会让人以为漏了一条记录。
- 地区在浮动窗里做**相邻重复段压缩**（「北京市 - 北京市 - 朝阳区」→「北京市 · 朝阳区」），
  并把「合并 N 条」放到第二行：一行放不下时，先截断地区而不是截掉条数。
- 位置默认在点的上方，上方放不下翻到下方；左右夹在绘图区内，不压纵轴刻度。
- 竖向虚线把点和下面的日期轴连起来；点的圆环加粗一档，和普通点区分。

### 8.31.5 实测（Redmi K80 AVD，1440×3200）

注入 32 条记录（肉包子 31 条，跨 2024-10～2026-10，3 种售卖单位，其中 2026-10-31 有北京/上海两条个价）：

| 动作 | 结果 |
| --- | --- |
| 进页 | 上走势、下历史记录；列表 5 行后截断，内部可滚 |
| 列表内部滚动 | 明细滚到 2026-09-09，图仍在原位 |
| 点按关键点 | 浮动窗「2026-10-31 / 3–3.8 元/个 / 北京市 · 朝阳区、上海市 · 浦东新区 / 合并 2 条」 |
| 双指捏合（`sendevent` 注入 MT-B） | 视窗放大到 2025-06～2025-12，纵轴 10–16，刻度与日期重算，右上角出现「重置」 |
| 单指拖动 | 日期轴平移，点与竖线跟着走，不越出绘图区 |
| 重置 | 回到 2024-10-05～2026-10-31 全貌，按钮消失 |
| 统计页筛「北京市」后进入 | 「26 条 / 共 31 条」 |
| 浅色主题 | 三条线、刻度、浮动窗（白底 + 描边）均清晰 |

**回归方式**：`assembleDebug` 通过；`PriceChartViewportTest`（缩放锚点、缩放夹紧、全貌拖动无位移、
拖动跟手、边界夹紧、日期窗贴边、地区压缩）与物价相关单测共 40 项通过；全量 322 项里仍是既有的
`FoodNutrientRepositoryTest` 6 项失败（Q-6），与本轮无关。

**踩到的一个精度坑**：日期窗原先用 `Long * Float` 算，两天跨度是 10 位毫秒，Float 只有 24 位尾数，
全貌时算出的终点比 `maxDate` 小一两秒——图上的末点照画，但它的日期标签被「是否在窗口内」判掉，
右侧凭空少一个 `10-31`。改 `Double` 后再取整，并保证终点至少比起点大 1 ms。
这一类只在「跨度大 + 全貌」时出现的偏差，靠单测很难碰巧撞上，是装机看出来的。
捏合没法用 `adb input` 稳定复现，装机时用 `sendevent` 按 MT-B 协议注入了双指手势；
日常回归靠 `PriceChartViewportTest` 守住算式。

## 8.32 记物价：搜索框改下拉、四格两行（第三十轮）

用户的要求是「搜索框改为下拉框，展开后第一行带搜索框，选项是物品名称」，并把四个筛选压进两行。
上一版（§8.29.2）是「一个关键词搜索框 + 三行横滚 chip」，两个毛病：

1. **搜索是包含匹配，永远拿不到精确条件**。输入「苹果」会同时命中「红富士苹果」和「苹果醋」，
   而这页是按物品聚合的，用户要的是「只看这一样」；关键词也解释不了「筛出来的行」和行上「N 条」的关系。
2. **三行 chip 把卡片撑到半屏**，年份一多还得横向划——和「筛选是给汇总让路」的定位相反。

### 8.32.1 四个格子，两行

| 行 | 左 | 右 |
| --- | --- | --- |
| 1 | 【商品】下拉（展开后首行是搜索框） | 【地区】下拉（多选，点空白收起） |
| 2 | 【年份】下拉 | 【月份】下拉 |

每格高度压到 `spacing.hitMin`（48 dp），标签留在格内左侧（`caption` / `textMuted`），值靠右、超出省略，
末端一个 `ic_arrow_drop_down`。有条件时值改用主色、描边换主色——不靠箭头方向、也不只靠颜色一个通道
（值本身已经和「全部」不同）判断「这格筛过没有」。

四格都置顶「全部」，它**钉在选项列表之外**、不随列表滚动：翻到第 12 个月，「回到全量」还在原位。

### 8.32.2 为什么自己写 `Popup` 而不是 `DropdownMenu`

M3 的 `DropdownMenu` 把内容整体塞进一个 `verticalScroll` 的 `Column`；把搜索框放进去，它会跟着长列表
一起滚走——而搜索框恰恰是「列表长才需要」的那个东西。所以 `PriceFilterDropdown` 自己用 `Popup`（`focusable`）：

- 搜索框固定在第一行，选项区是 `LazyColumn`（`weight(1f, fill = false)`，内容短时菜单自动收窄）；
- 宽度取 `max(字段宽, 248 dp)` 并夹进窗口左右 8 dp：菜单比字段宽是常态（中文物品名放不下），
  但**展开不改变字段宽度**，否则一行两格会互相推挤（§7.2）；
- 高度取 `min(320 dp, 到屏幕底剩余)`（§7.2 下拉最大 320 px），超出滚动；
- 图标、边框沿用设计令牌：浮层底 `panelRaised` + 1 dp **`borderStrong`** 描边。
  深色下 `panelRaised` 与卡片同色，用 `border` 的话浮层边界直接糊进卡片里（装机第一版就是这样）；
- 重新展开时把选中项滚到第一行（`initialFirstVisibleItemIndex`），物品按「最近买过」排，选中的可能是第 10 个。

**偏离 §7.2 一条**：规范说「少于 6 个选项优先用分段控件/单选框，不放进下拉」。年份通常只有 2–4 个，
本可以留在 chip 上；但用户要求四个统一成下拉、两行排布，混排会让四个格子高矮不齐，故四个都是下拉。

### 8.32.3 数据模型：`query` → `selectedItem`

| 项 | 旧 | 新 |
| --- | --- | --- |
| 状态 | `query: String` | `selectedItem: String?`（null = 全部） |
| 匹配 | `itemName.contains(token)` | `priceItemKey(record.itemName) == priceItemKey(selectedItem)` |
| 选项 | 无（自由输入） | `itemOptions`：记录里出现过的物品名，按物品键去重、最近的在前 |
| 操作 | `setQuery(String)` | `selectItem(String?)` |

比对一律过 `priceItemKey`（`trim().lowercase()`），和统计聚合是同一把尺子：记录里写成「红富士苹果 」
（尾空格）也能命中，否则会出现「筛出 3 条，行上写着 4 条」。

【地区】仍是多选：格子里 1 个地名直接显示，2 个以上显示「已选 N 个」；
点选项**不收起**（要能连着选），底部固定「完成」负责收起；点置顶「全部」= 清空多选。

### 8.32.4 实测（Redmi K80 AVD，1440×3200，density 3.5）

临时注入 56 条记录（14 个物品 × 4 个日期，5 个地区）：

| 动作 | 结果 |
| --- | --- |
| 打开【统计】 | 筛选卡两行四格，卡片高约 170 dp；统计条目单位全展开 |
| 展开【商品】 | 面板 248 dp 宽、首行搜索框、置顶「全部」带勾；列表可滚，「全部」不动 |
| 搜索框输入 `zz` | 只剩「没有匹配的商品」，面板收窄到内容高度 |
| 选「大白菜」 | 字段变主色、统计剩 1 种；再展开时列表以「大白菜」为第一行 |
| 地区选 2 个 | 字段「已选 2 个」、两行打勾、菜单不收起、统计实时收窄；「完成」收起（此两处已被 §8.33 的「开始查找」替换） |
| 月份滚到 11 月 | 置顶「全部」仍在原位 |
| 清除筛选 | 四格回「全部」，「清除筛选」消失，统计回 14 种 |
| 切【记录】 | 仍 56 条、14 个物品，未被统计页筛选影响 |
| 【物品历史】 | 继承筛选后「4 条 / 共 4 条」 |
| 浅色主题 | 浮层白底 + `borderStrong` 描边，与白卡片的边界清楚 |

**已知边界**：面板最大高度按屏幕高算，没有减掉输入法高度。筛选卡在页面顶部，正常情况下
（含键盘弹出）面板底部仍高于键盘；真遇到小屏 + 超长物品名时，靠列表内滚仍可够到最后一项。

**回归方式**：`assembleDebug` 通过；全量 322 项里仍是既有的 `FoodNutrientRepositoryTest` 6 项失败（Q-6），
物价相关 40 项全通过。

### 8.33 筛选改为「一次查找」生效

在统计页改条件时，列表不再逐次重算：四个格子只动草稿，点【开始查找】才一次性落到
`filteredRecords`；地区下拉同时去掉了底部的「完成」。

#### 8.33.1 为什么删掉地区下拉的「完成」

「完成」是**菜单内部**的一个确认动作，但它确认的东西（选了哪几个地区）在字段上已经写着
「已选 2 个」、在列表里也有勾——这个按钮只是告诉用户「可以关掉我了」。关掉浮层本来就有三条路：
点浮层外、再点一次触发格、按返回键。多一个「完成」等于给一个没有歧义的动作配专属按钮，
还把「多选」和「要确认」绑在一起，让人以为不点它就不生效。

现在：点选项只打勾、不收起（连着选几个不用重开）；点空白 / 点格子 / 返回键收起。
浮层内部除搜索框与选项之外没有任何按钮。

#### 8.33.2 草稿与生效值：为什么要两份

| 字段 | 含义 | 谁读 |
| --- | --- | --- |
| `selectedItem` / `selectedRegions` / `selectedYear` / `selectedMonth` | 草稿：界面正在编的值 | 四个格子的值、选中态、`hasPendingFilters` |
| `appliedItem` / `appliedRegions` / `appliedYear` / `appliedMonth` | 生效值：上次点【开始查找】的结果 | `filteredRecords` → `visibleStats` / `historyRecords` |

只留一份（改了就生效）的问题在**多选地区**上最明显：连选三个地区，列表要重算三次、跳动三次，
第三次点下去时列表已经换了一批行，手指落点跟着变；年份 + 月份连改更是每次都闪一下。

提交走 `applyDraft()`：四个字段**一次 `copy`**。分四次改状态会触发四次重组，中间几帧是
「只应用了一半」的条件组合，列表会闪过一个从来不存在的中间结果。没有改动时 `applyDraft()`
返回 `this` 而不是 `copy`——`copy` 造新对象等于让整页白重组一次。

`hasPendingFilters` 逐字段比（`Set` 用 `==`），草稿改回原值就自动消掉「有改动」；
`hasFilters` 只看生效值，所以「清除筛选」的显隐不受草稿影响。`clearFilters()` 两边一起归零
（`withoutFilters()`），不留「待应用」的中间态。

#### 8.33.3 【开始查找】为什么在格子上面

放上面有三个理由：条件填在下面、动作在上面，改完抬头就是它，不用滑过四个格子去找；小屏上
键盘弹起时放底部的按钮会被两行下拉一起顶出可视区；它同时兼作「这一下有东西可查」的状态提示。
按钮的具体形态（与「清除筛选」同排、描边、真禁用）在 §8.34 改过一版。

已知摩擦：地区浮层开着时点【开始查找】，第一下只关浮层（`Popup` 是独立 window，外部点击由
`onDismissRequest` 消费，不穿透）。实际路径通常是「选完地区 → 点空白 → 点开始查找」，两下都
落在本来就要做的动作上，故没有为它加特殊处理。

#### 8.33.4 实测（Redmi K80 AVD，1440×3200，density 3.5，56 条临时数据）

| 动作 | 结果 |
| --- | --- |
| 打开【统计】 | 卡片 = 两行四格 + 上方一行动作按钮（形态后被 §8.34 改过） |
| 展开【地区】 | 只剩「全部」+ 5 个地区，底部没有「完成」；置顶「全部」带勾 |
| 连选 2 个地区 | 字段「已选 2 个」、两行打勾、浮层保持展开、统计仍 14 种；按钮转主色、「清除筛选」出现 |
| 点空白 | 浮层收起；统计仍是 14 种（草稿未应用） |
| 再选年份 2026 + 月份 3 月 | 统计仍 14 种 |
| 点【开始查找】 | 共 6 种，每行「1 个地区 · 1 条记录」 |
| 清除筛选 | 四格回「全部」、分类回 14 种 |
| 商品选「红富士苹果」 | 字段变主色、按钮转主色、统计仍 14 种；点【开始查找】→ 共 1 种 |
| 进【物品历史】 | 继承生效条件，「4 条 / 共 4 条」 |
| 浅色主题 | 按钮两态（`primaryContainer` 浅蓝底 / `primary` 深蓝实心）文字均清晰 |

**回归方式**：`assembleDebug` 通过；全量 328 项里仍只有既有的 `FoodNutrientRepositoryTest`
6 项失败（Q-6）；新增 `PriceFilterApplyTest` 6 项（草稿不动列表、一次性应用、无改动不造新对象、
改回原值清 pending、清除两边归零、多条件叠加）全过。

### 8.34 筛选动作并成一行 + 血糖图表空态占位

#### 8.34.1 两个动作同一行、同一套样式

之前两个动作用的是两套语法、两个位置：【开始查找】是整行按钮（实心 / 低权重两态），
【清除筛选】是卡头右侧的 `TextButton`，只有存在条件时才出现。现在合成一行两个等宽按钮：

| 按钮 | 图标 | 可点条件 |
| --- | --- | --- |
| 【开始查找】 | `ic_search_empty`（放大镜） | 有未应用的改动（`hasPendingFilters`） |
| 【清除筛选】 | `ic_refresh`（重置箭头） | 已生效条件非空，或有未应用的改动 |

两者都是 `OutlinedButton` + 1 dp 描边 + 18 dp 图标 + `bodyS` 文字，行高 48 dp（`spacing.hitMin`）。

为什么不用整行实心主按钮：卡片里真正要读的是四个格子的值，一块通栏实心蓝会把这四行压成
「按钮的附件」；按 §1.3，一屏只留一个主操作——这一屏的主操作是「选条件」，不是「按下按钮」。

**禁用态为什么重画**：上一版用「容器色在 `primary` / `primaryContainer` 之间切换」表达状态、
始终可点，用户看不出禁用（他反馈的原话是「禁用状态不明显」）。现在用真的 `enabled = false`，
但把 `disabledContentColor` 从 M3 默认的 `onSurface` 38% 换成 `textMuted`、边框从 `primary`
退到 `border`：默认那层灰在 `panelRaised` 上几乎看不见，读起来像「按钮没画完」，
分不清「暂时不能点」和「这里坏了」。禁用时文字仍然读得清，只是从主色退回中性色。

两个按钮常驻、等宽、位置不动：这一下没改动时【开始查找】变淡，条件清空后【清除筛选】变淡，
行高不跳。

#### 8.34.2 血糖图表空态：占住画布的位置

之前空态是 `Column` 里的一行 `Text`：「此时间范围内没有血糖记录。」下一秒下面的图例、
统计卡、八点血糖卡就整块上移——切时间范围时页面高度来回跳，而且「这一块是图表」的视觉位置
也没了，看起来像这页本来就没有图表。

现在换成 `EmptyChartPlaceholder`：一个 `AppSectionGroup`（`panelRaised` + 1 dp `border` +
`container` 圆角），高度与画布一致（非全屏 210 dp，全屏 `fillMaxSize`），说明文字用 `bodyS` +
`textMuted` 居中、`textAlign = Center`，左右各留 `spacing.s4` 防长语种贴边。

补一个 `spacing.s2` 的间距：有数据时画布下面还有一条拖动条（`HorizontalPanArea`），空态没有它，
不加这口气的话图例直接贴在卡片底边上（装机第一版就是这样）。

#### 8.34.3 实测

| 动作 | 结果 |
| --- | --- |
| 打开【统计】（无任何条件） | 两个按钮同排、都灰（禁用），图标 + 文字 + 描边齐全；四格正常 |
| 选一个地区 | 两个按钮同时变主色可点；统计未变（草稿） |
| 点【开始查找】 | 统计收窄；【开始查找】回灰、【清除筛选】保持主色 |
| 点【清除筛选】 | 四格回「全部」，两个按钮同时变灰 |
| 浅色主题 | 禁用态（灰字灰框）/ 启用态（蓝字蓝框）都清楚，禁用态不发虚 |
| 血糖【图表】无数据 | 空态为与画布同高的卡片，文字居中；下方图例与卡片间距 s2；浅色下白底 + 描边清楚 |
| 血糖【图表】有数据（改动前） | 画布、图例、统计卡布局未变 |

**回归方式**：`assembleDebug` 通过；全量 328 项里仍是既有的 `FoodNutrientRepositoryTest` 6 项失败（Q-6）。

#### 8.34.4 【生成预测数据】的折行：不是缺陷，是轮播

`ChartBottomControls` 里【生成预测数据】按钮固定 `width(70.dp)`，6 个字的 `caption` 需要
约 78 dp，截图里看起来像被折成「据 / 生成预」并裁到屏幕外。**用户已确认这是预期行为**：
该按钮内部是轮播文字（`pref_text_overflow = marquee`，§5 的文字溢出策略），
文字在 70 dp 的框里横向滚动，静态截图必然截到「半句」。

由此关闭：不加宽按钮、不改文案。以后再看这个按钮的截图，按轮播中的一帧理解，
不要当折行 bug 修。

## 8.35 食材/食物/菜肴对比重做（第三十三轮）

### 8.35.1 旧界面为什么不能修修补补

旧的对比页是「候选表 + 结果表」两张 `AppDataTable`：

| 问题 | 具体表现 |
| --- | --- |
| 三列都放不下 | 窄屏下每列 96 dp 起，表头先被省略，最后留下的只有食物名首字 |
| 读不出「谁是谁」 | 结果表里两列数字并排，没有名字、没有单位归属，靠颜色区分 |
| 读不出「差多少」 | 差异只靠「更高就加粗」，两个值差 10% 和差 10 倍长得一模一样 |
| 结论是假的 | 单位没归一：一边 5 mg、一边 5000 mcg，直接比原始数字会得出「高 1000 倍」 |
| 页面在讲候选 | 候选表占掉首屏，结果要滚过一张表才看得见 |

这五条不是排版问题，是「用表表达对决」这件事本身不适合手机：表擅长横向对齐多项，
而对比只有两方，需要的是「一次只读一行的两个数」。

### 8.35.2 新结构：对决 → 结论 → 分组

```
对决卡（当前食物 ｜ VS ｜ 对比对象）      ← 固定的身份区，滚到哪都知道在比谁
├ 空态卡（还没选对手）
└ 结论卡（左高 N / 相同 N / 右高 N）
   └ 七个分组卡：能量与宏量 / 氨基酸 / 水溶性维生素 / 脂溶性维生素
                / 常量矿物质 / 微量矿物质 / 其他
```

- 分组按 `CompareGroup` 枚举 `ordinal` 排序，不跟元数据顺序走：数据文件里 `CHO` 排在
  `FIBER` 前是录入顺序，不是读者想读的顺序。
- 两侧都没有值的营养素整行不出现；只有一侧有值的保留，另一侧显示「—」。
- 组名用 `bodyS` + SemiBold，比页面标题（`titleL`）低两级：7 张卡的组名若用 `titleS`，
  滚动时会被误读成页面标题。

### 8.35.3 选对手搬进底部抽屉

候选在整库（200+ 项），压在页面上就是上表最后一条的老毛病。改成 `AppBottomSheet`
（`initialSnap = Full`、`contentScrollable = false`，见 §8.22）：

| 位置 | 内容 |
| --- | --- |
| 首行 | `AppSearchField`「搜索对比目标」，支持别名 |
| 第二行 | `AppFilterChip` × 4：全部 / 食材 / 食物 / 菜肴 |
| 列表 | 64 dp 行：缩略图 + 名称 + 「分类 · N 千卡」；选中行打勾 + `primaryContainer` 底 |

- **行副标题先解决「能不能比」**：没有营养表的菜肴（如「水煮肉片」「地三鲜」）写
  「菜肴 · 暂无营养数据」，让用户在点之前就知道和它比会是一页「—」。
- **重开抽屉时已选项置顶**：`LazyListState(firstVisibleItemIndex = selectedIndex)`；
  否则对手在列表第 80 行时，用户打开抽屉看到的是一堆无关项，还以为没选上。
- 改搜索词/分类时回顶部，用 `userNarrowed` 标志区分「用户主动改了条件」和「首次组合」，
  否则首帧就会把置顶位置滚掉。

### 8.35.4 单位归一 + 「相同」按显示精度判

对比前两边都换算到 `NutrientMeta.baseUnit`：`UnitConverter.toBase` + `fromBase`，
`ENERGY` 走 `energy` 类、其余走 `weight`。换算结果为 0 而原值非 0（单位 id 不认识）
时退回原值——宁可显示原始数字，也不要把「不认识」显示成「没有」。

「相同」的判定用 `abs(l - r) <= max(0.0005, max(l, r) * 0.001)`：0.10004 与 0.1 在
两位小数下都是 0.10，判成「一高一低」会让结论卡的三个数字对不上表里的行。

数值格式按量级取小数位（≥100 取 0、≥1 取 1、≥0.1 取 2、其余 3），再
`trimEnd('0').trimEnd('.')`：43 不写成 43.0，0.0456 写成 0.046。

### 8.35.5 双柱：以中缝为零点

`CompareDuelBar` 的左柱向左（`primary`）、右柱向右（`chart.carbs`），
满量程 `maxValue = max(left, right)`，所以较短那根的长度就是真实比例；
若两侧各自拉满，读者读到的「一样多」是假的。

两条实测后定下的规则：

1. **值为 0 不画柱**。第一版给了 `coerceAtLeast(size.height * 0.5f)` 的最小长度，
   碳水 15.3 vs 0 时右侧留了个橙色小圆点，被读成「有一点点」。
2. **不可比的行不画柱、也不画基线**。第一版画了一条贯穿的灰线代替柱形，理由是
   「别让满格柱暗示另一侧是 0」；实际上拿黄油（34 项）对南瓜时，氨基酸 + 维生素 +
   矿物质连着 20 多行全是灰长线，既像分隔线又像「两侧相等」，反而要多读一眼。
   「—」已经把「这边没有」说清楚了，柱区留空最干净。

### 8.35.6 空态、可访问性、状态保持

- 任一侧没有营养表 → 整页换成 `AppEmptyState`，标题点名是哪一项没有数据，结论卡与
  分组卡都不出现（否则就是 0/0/0 + 一页「—」，用户会以为对比功能坏了）。
- 每行 `semantics(mergeDescendants = true)` 一次性读出「名称：左名 左值 单位，右名 右值 单位」；
  颜色只作辅助，两个数字都写在文字里。
- `opponentId` 用 `rememberSaveable(food.id)`：切深色模式或转屏重建 Activity 后对手还在。

### 8.35.7 抽成纯函数层

`FoodCompareModel.kt`（新增）只放可测逻辑，`FoodComparisonScreen.kt` 只放画法：

| 函数 | 职责 |
| --- | --- |
| `compareGroupOf(meta)` | `category` → 七个显示分组 |
| `buildCompareSections(...)` | 组装分组与行，跳过两侧全空的项 |
| `compareSummary(sections)` | 左高 / 相同 / 右高 |
| `formatCompareValue(value)` | 按量级取位 |
| `CompareRow.sameAtDisplayPrecision()` | 显示精度下的「相同」 |

`FoodCompareModelTest` 9 项：分组映射、跳过双空、按枚举排序、5000 mcg→5 mg、
180 kJ→43.02 kcal、同单位原样返回、显示精度计数、数值格式、满量程取较大侧。

### 8.35.8 实测（Redmi K80 AVD，1440×3200，density 3.5）

| 场景 | 结果 |
| --- | --- |
| 食材对比 南瓜 vs 花蛤 | 对决卡对称、VS 居中；结论 1 / 0 / 3；能量 22 vs 62 双柱比例正确；碳水 5.3 vs 2.8 左长右短；膳食纤维 0.8 / — 柱区留空 |
| 菜肴对比 农家一碗香 vs 红烧猪脚 | 结论 1 / 0 / 3；碳水 15.3 vs 0 时右侧无柱（0 值规则生效） |
| 黄油（34 项）vs 南瓜 | 6 张分组卡全部出现：氨基酸 9 项、水溶性维生素 8 项、脂溶性维生素 3 项、常量矿物质 5 项、微量矿物质 3 项；单位逐行不同（mg / µg）且换算正确 |
| 抽屉首开 | 全部 / 食材 / 食物 / 菜肴 四胶囊；行副标题「菜肴 · 1300 千卡」「菜肴 · 暂无营养数据」 |
| 选完花蛤后重开抽屉 | 花蛤置顶第一行、打勾、`primaryContainer` 底 |
| 搜索「zzz」 | 空态：放大镜 +「没有匹配的对比目标」+「换个关键词，或切到其他分类再看看。」 |
| 对手是「水煮肉片」（无营养表） | 整页空态，点名是哪一项；无 0/0/0 结论 |
| 深色 / 浅色 | 两套都过：卡片 `panelRaised` + 描边、蓝/琥珀柱、结论色点、`—` 的 `textMuted` 都读得清 |

**回归方式**：`assembleDebug` 通过；全量 337 项里 331 过、仍是既有的
`FoodNutrientRepositoryTest` 6 项失败（Q-6）。

### 8.35.9 发现但未改

1. **数据只有一套宏量**：263 个食物记录里只有 `food:dairy:butter` 一条带完整的
   维生素 / 矿物质 / 氨基酸，其余 196 条只有能量、蛋白、脂肪、碳水（部分有纤维）。
   所以分组卡绝大多数时候只出现「能量与宏量营养素」一张。这是数据问题，不是布局问题；
   补数据后七张卡会自动出现，无需改代码。
2. **食物库左栏分类标签被裁**：「奶类及制品」显示成「类及制」，长标签全部左边截断；
   同时搜索框叠加着分类筛选时，搜「黄油」只会返回当前分类里的项（要先清掉「大豆」筛选）。
   属食物库页（`NutritionScreen` 列表），不在本轮范围。
3. **抽屉遇到软键盘只露 3 行**：抽屉高度按 `Full` 结算，键盘弹起后列表被压掉；
   功能可用（可滚动），但与「搜索 → 选」的顺手动线有摩擦。要改需抽屉让出 IME 高度，
   属 `AppBottomSheet` 的高度模型（§8.24），留作待决。

## 8.36 对比双侧可换 + 记睡眠整体重做（第三十四轮）

### 8.36.1 对比页：左侧也是「一侧」，不是锚点

旧版左格是入口带进来的那一项，固定当基准，只有右侧能换。这等于规定「先点的就是标准」——
想换个基准只能退出去重进。现在两格都可点：

```kotlin
private enum class CompareSide { LEFT, RIGHT }
var pickingSide by remember(food.id) { mutableStateOf<CompareSide?>(null) }
```

抽屉标题随侧变（左「更换这一项」 / 右「选择对比对象」），选完回写到 `leftId` / `opponentId`。

两个连带决定：

| 决定 | 原因 |
| --- | --- |
| 抽屉候选**不排除**任何一侧 | 本侧当前项要留在列表里打勾 + 置顶，否则「我现在选的是哪个」看不出来 |
| 选到「对面那一项」时**交换两格** | 不交换的话两格变成同一样，整页全是「相同」，用户会以为选错了 |

标题按 `left.kind` 出（`comparisonTitleRes()`），所以换完基准项标题会从「食材对比」变成
「菜肴对比」——标题描述的是这一页在比什么，不是入口从哪来。

### 8.36.2 禁用态文字加删除线：必须写进 `style`

`OutlinedButton` 的内容会套一层 `LocalTextStyle`，`Text(textDecoration = TextDecoration.LineThrough)`
这个参数在合并时被吃掉（放大截图确认没有横线）。写进 `style` 才稳定生效：

```kotlin
style = if (enabled) tokens.type.bodyS
        else tokens.type.bodyS.copy(textDecoration = TextDecoration.LineThrough)
```

只给禁用态加线，启用态不加——删除线在这里表示「这个动作现在不成立」，不是一种装饰。

### 8.36.3 记睡眠记录页：先总览，再按天

```
概览卡（最近一次 + 日均睡眠 + 条数）
筛选胶囊（全部 / 夜间睡眠 / 小憩）
进行中
今天 / 昨天 / M 月 D 日
  卡片…
```

- 空库时整页居中空态，**不挂筛选**：空态贴在页头下面会被读成「加载失败」；筛选后为空另用一套文案。
- 概览右列只在记录多于一条时出现：「平均」等于自己那条时，多写一列等于骗人。
  「近 7 日平均」改叫「日均睡眠」——除的是**有记录的天数**，不是 7。
- 卡片层级：类别小胶囊（主色 12% 底）→ 时长 `titleS` 右对齐 → 起止时间 + 夜尿（`caption`）
  + 编辑/删除两枚图标 → 备注 → 计时器。编辑/删除压到时间行右端，不占第一行。
- 主操作收成右下角常驻胶囊 FAB（替换原顶部通栏按钮）；列表底部留 `hitMin + s6`，
  滚到底时最后一张卡能整张露在按钮上方。
- 时长文案随语言（「8 小时」/「8h」），整点不写「0 分」；时间到分钟
  （新增 `formatRecordClockMinute`），不再出现 `12:27:00` 的尾巴。

### 8.36.4 编辑页：结果放最上面

类别切换下面加「本次时长」卡：大数字 + 紧凑时间段（同日 `12:27 → 20:27`，跨天才带月日）。
它是**结果**，两个时间字段是**输入**，混在同一张卡里读者会以为时长也能点开。

夜尿计数改 48 dp 步进按钮，0 时减号进禁用色——「0 次还能再减」是最容易误点的一格。

计时器档位的默认值必须落在当前类别的档位里：旧代码一律取小憩第一档（10 分钟），
夜间睡眠下这个值不在 15/30/45/60 里，界面写着 10、存下去却被 `coerceIn` 夹成 15。

### 8.36.5 统计页：概览 / 趋势 / 当日明细

| 卡 | 结构 | 本轮修的点 |
| --- | --- | --- |
| 概览 | `EqualWidthSegmentedTabs`（全部/夜间/小憩）+ 2~3 列指标 | 三列各 1/3 宽时 `titleS` 顶出卡片边界 → 改 `type.body` + SemiBold |
| 趋势 | 7/30 天 `AppFilterChip` + `DateStackedBarChart` + 图例 | 7 根柱 × 52 dp 比卡片宽、最右日期被切一半 → `columnWidth = 40.dp`；30 天横向可滚 |
| 当日明细 | 日期标题 + `DayRecordRow` | 标题与其它卡统一写「10-31 · 8 小时」，不再用图轴的 `8h` |

### 8.36.6 默认时长：输入框 + 单位下拉 → 时 / 分步进器

旧版是一个数字输入框 + 一个 h/min 下拉。默认时长就那几档（8 小时、1 小时 30 分），
点两下比唤出键盘再输 `7.5` 快；而且下拉选单位还得先自己换算这 0.5 到底是 30 分还是 50 分。

单位摆到行首（「小时」「分钟」两行）之后，「单位」这个字段就不需要了，
`sleep_settings_default_duration_value` / `sleep_settings_unit` 两条字符串一起删掉。
单位偏好（`nightUnitId` / `napUnitId`）仍留在 `SleepPrefs` 里兼容旧数据，这一页不再写它。

减到 0 时显示灰字「0 分钟」并禁用保存：0 是非法值，但大字号位置要留住，
否则整张卡会跳一下高度。设置首页那行副标题（「夜间 8 小时 · 小憩 1 小时 30 分」）保留——
只有标题的一行，既看不出默认多少，也看不出改没改。

### 8.36.7 计时器：三个问题

1. **平方衰减**（真 bug）。`advance()` 只改 `remainingSeconds`、不改 `startedAtMillis`：
   下一次 tick 会把同一段流逝再减一遍。每秒推进一次、累计起来是平方级衰减，
   15 分钟的计时器四十来秒就报「结束」——本轮就是在设备上看到 `剩余 15:00` 变成
   `计时结束` 才发现的。修法：推进时把起点挪到 `now`，并补两条单测
   （线性推进、599 次「过 1 秒」之后才该结束）。
2. **档位越界**：默认值可能不在当前类别的档位里（§8.36.4）。
3. **倒计时读不懂**：旧卡片只有一个裸的 `15:00`，压在 `12:47 → 20:47` 下面像第三个时间点，
   而且只有重新进页面才变。现在：时钟图标 + 「剩余 15:00」（主色）/「计时结束」（`success`），
   右侧取消键从红垃圾桶换成灰叉（一张卡两个垃圾桶，分不清哪个是删记录）；
   倒计时用 `produceState` 每秒本地重算——按 `(remainingSeconds, startedAt)` 这对一致的值推进，
   不必等 ViewModel 刷新。

### 8.36.8 实测（Redmi K80 AVD，1440×3200，density 3.5）

| 场景 | 结果 |
| --- | --- |
| 对比页左格「更换」 | 抽屉标题「更换这一项」、首行当前项带勾；选西葫芦后左格换、标题仍是「食材对比」 |
| 左侧再选「水煮肉片」（对面那一项） | 两格交换，标题变「菜肴对比」，不出现整页「相同」 |
| 物价统计禁用按钮 | 两个按钮文字都有删除线（放大截图确认），图标/描边仍是 `textMuted` / `border` |
| 睡眠记录页（深/浅） | 概览「最近一次 · 夜间睡眠 / 8 小时 / 12:27 → 20:27」、筛选胶囊、日期分组、卡片三层信息；一条记录时右列不出现 |
| 计时器运行 | 「剩余 15:00」→ 8 秒后 14:52，每秒自减；结束时变绿「计时结束」+ 「补录醒来时间」 |
| 编辑页 | 「本次时长 8 小时 / 12:27 → 20:27」；夜尿 0 时减号禁用；保存条随改动启用 |
| 统计页 | 概览三项、7 根柱完整（含 10-25 标签）、平均虚线；切 30 天后横滑到 10-15 正常 |
| 默认时长（夜间/小憩） | 8 小时 / 1 小时 30 分；分钟点两下 → 1 小时 40 分、保存条亮起、设置页副标题跟着变 |
| 深色 / 浅色 | 两套都过：大数字（`primary`）、`--`/禁用色、图表柱与虚线、FAB 阴影都读得清 |

**回归方式**：`assembleDebug` 通过；全量 339 项（新增 2 项计时器单测）里 333 过，
仍是既有的 `FoodNutrientRepositoryTest` 6 项失败（Q-6）。

### 8.36.9 发现但未改

1. **新建记录按返回也弹「放弃未保存的修改？」**：`hasChanges = current != existing`，
   新建时 `existing == null` 恒为真。功能上是「保守起见」，但空表单上弹这个对话框像在找茬（Q-12）。
2. **删记录不连带删计时器**：`TimerArchiveStore` 里会留下没人引用的实例——界面上看不见，
   但数据会一直躺着（Q-13）。
3. **同一天多条记录会让「日均睡眠」超过 24 小时**：一天三条 8 小时显示 24 小时，数学没错、
   读起来别扭。要治得在录入侧做同日提示，属产品决定。

## 8.37 睡眠类别代表色 + 双入口 + 统计统一筛选（第三十五轮）

### 8.37.1 为什么给两个类别各配一种颜色

夜间睡眠和小憩在数据模型里只有 `kind` 一个字段不同，但用户对它们的心智完全不同：
前者是「一整晚」，后者是「白天补的一觉」，时长量级差一个数量级。旧界面两者共用
主色（蓝），一列卡片扫下来只能靠文字首字区分。

新增 `AppSleepColors`（`tokens.sleep`，深 / 浅两套）：

| 令牌 | 浅色 | 深色 | 用在哪 |
| --- | --- | --- | --- |
| `night` | `#33409E` | `#6C79E0` | 夜间睡眠徽标、记录页左上按钮、趋势柱、图例 |
| `nap` | `#8F5A0B` | `#B06A16` | 小憩徽标、记录页右上按钮、趋势柱、图例 |
| `onSleep` | `#FFFFFF` | `#141414` | 徽标上的文字 |

两种色都按「实心底 + 反白字」校准过对比度（≥ 4.5:1），且色相拉开到靛蓝 / 暖橙，
红绿色盲也能分开；颜色之外仍保留文字标签，不单靠色相传递信息。

**用在哪一层是有讲究的**：只有卡片上的类别徽标用实心底，记录页顶部两个入口用
「同色淡底 + 同色描边 + 同色字」。一页里并排两块实心色块，谁也说不清哪个是主操作。

### 8.37.2 入口：一个「记一笔」→ 两个类别按钮

上一版把新增收成右下角胶囊 FAB（更现代，但与项目里其他记录功能不一致）。本轮按用户
要求改成记录页顶部并排的两个按钮：

- 左【记夜间睡眠】（月亮带 Zzz）、右【记小憩】（钟）。
- 点哪个进去，编辑页的类别分段就已经选中哪个（`initialKind`），仍可切换。
- 默认时长随之不同：夜间 8 小时、小憩 1 小时 30 分。
- FAB 与其阴影、`semantics` 一并删除；列表底部让出的 `hitMin + s6` 也改回 `s6`。

图标不是通用加号：两个加号并排等于「两个入口长得一样」，用户还得读一遍字。

### 8.37.3 统计页：三块数据共用一个顶部筛选

旧版统计页的筛选只作用于「当日明细」，概览与趋势各自按固定 7 天算，页面上同时存在
两种时间范围。本轮照【记物价】-【统计】-【查找与筛选】的模型重做：

- 一张 `AppSectionCard`（标题「查找与筛选」+ 放大镜图标）放在最上面；
- 两个动作按钮：`AppFilterActionButton`（从物价页抽到 `common/ui/designsystem`，
  禁用态文字带删除线，见 §8.36.2）；
- 两行胶囊：**时间范围**（近 7 天 / 近 30 天 / 近 90 天 / 全部）、**记录类型**（全部 /
  夜间睡眠 / 小憩）；
- **草稿 - 应用**模型：点胶囊只改草稿，按【开始查找】才写进 `applied`；草稿与已应用
  不一致时【开始查找】才可用，已有筛选时【清除筛选】才可用；
- 概览、趋势、当日明细三块**全部**从同一份 `applied` 派生，不会再出现两套范围。

概览卡的指标名随类别变化：选「小憩」时显示「小憩总时长 / 小憩平均时长 / 小憩平均
入睡时间 / 小憩平均醒来时间」，选「全部」时才是「总睡眠时长」。

### 8.37.4 页签：统计在左、记录在右，并记住上次停在哪

顺序与物价、饮水、用药对齐（统计左 / 记录右）。状态提到路由层
（`SleepRecordActivity`）：

```kotlin
var sleepTabId by rememberSaveable {
    mutableStateOf(if (AppPrefs.getSleepTab(context) == 0) TAB_STATISTICS else TAB_RECORDS)
}
fun selectTab(id: String) {
    sleepTabId = id
    AppPrefs.setSleepTab(context, if (id == TAB_STATISTICS) 0 else 1)
}
```

`tab_sleep` 存进 `UserPrefs`（与 `tab_medication` 同一约定），默认值 1 = 记录页
（这一页才有新增入口）。从【记录】退出的用户，下次进来还是【记录】。

### 8.37.5 顺带修掉的五处

1. **概览卡大数字被截断**：左右两列各 `weight(1f)` 时，32 sp 的「1 小时 30 分」只有
   半张卡宽，真机上显示成「1 小时 30 ...」。右列改成按内容取宽（`horizontalAlignment
   = End`），左列拿到剩余全部宽度。
2. **参考线标签被切成「1h30r」**：`AxisValueLabel` 用 `fillMaxWidth()` 在 58 dp 的轴宽
   里排版，「平均 1h30m」装不下。新增 `allowOverflow`：单行 + 不换行 +
   `wrapContentWidth(unbounded = true)`。**注意**：`horizontalScroll` 与无限宽约束不能
   共存（会抛 `Horizontally scrollable component was measured with an infinity maximum
   width`），两者只能二选一，参考线走不裁剪分支。
3. **90 天日期标签折成两行**（「10-2 / 7」）：日期 `Text` 补 `maxLines = 1` +
   `softWrap = false` + `TextOverflow.Visible`，列宽 24 → 30 dp；柱身两侧留白改成
   `min(列宽 × 0.25, 10 dp)`，否则 24 dp 一列只剩 4 dp 柱身，像头发丝。
4. **切时间范围后图表不贴右**：`rememberScrollState(Int.MAX_VALUE)` 只在首次组合取值，
   同一 `ScrollState` 复用时 maxValue 从 0 变几千，视图仍停在最左。改成
   `LaunchedEffect(entries.size)` + `snapshotFlow { maxValue }.filter { it > 0 }.first()`
   再 `scrollTo(maxValue)`。
5. **选中列描边是灰色**：浅色主题下和柱子轮廓混在一起，看着像「这根柱没上色」。改用
   `colorScheme.primary`。

另有一处文案统一：记录中心「记睡眠」的摘要从 `1h 30m` 改成 `1 小时 30 分`
（`RecordViewModel` 走 `sleep_duration_*` 三条字符串），一张列表里不留两种时长记法。

### 8.37.6 实测（Redmi K80 AVD，1440×3200，density 3.5）

| 场景 | 结果 |
| --- | --- |
| 记录页顶部 | 【记夜间睡眠】靛蓝淡底 + 描边、【记小憩】暖橙淡底 + 描边，等高 |
| 记录卡徽标 | 夜间睡眠 = 靛蓝实心白字；小憩 = 暖橙实心白字；时长右对齐 `titleS` |
| 记小憩 → 编辑页 | 分段已选「小憩」、时长 1 小时 30 分；记夜间睡眠 → 8 小时、计时默认 15 分钟（小憩 10 分钟） |
| 概览卡 | 「最近一次 · 小憩 / 1 小时 30 分 / 03:26 → 04:56」+ 右列「日均睡眠 / 4 小时 45 分 / 近 7 日 2 条」，大数字不再截断 |
| 统计页筛选 | 近 7 / 30 / 90 / 全部 + 全部 / 夜间睡眠 / 小憩；改草稿后【开始查找】变可用，按下去三卡一起变 |
| 筛选「小憩」 | 概览换「小憩总时长 / 小憩平均时长 / 小憩平均入睡时间 / 小憩平均醒来时间」；趋势只剩暖橙柱 |
| 筛选「全部」 | 记录数 1、趋势只有 10-31 一根柱；【清除筛选】变禁用（当前无筛选） |
| 近 30 / 90 天 | 打开即贴最右（11-01 可见）；日期标签单行；柱身随列宽变宽；横滑到 10-27 正常 |
| 页签记忆 | 统计 → 返回 → 重进 = 统计；记录 → 返回 → 重进 = 记录 |
| 深 / 浅两套 | 徽标、按钮、柱图、选中描边、参考线标签都完整可读 |
| 记录中心 | 「记睡眠」摘要 = 「15 小时前更新 8 小时」 |

**回归**：`assembleDebug` 通过；全量 339 项 333 过，仍是既有的
`FoodNutrientRepositoryTest` 6 项失败（Q-6），无新增失败。

### 8.37.7 发现但未改

1. **滚到最右时最左的边缘日期被切一半**（90 天档位显示成「25」）：横向滚动容器的常态，
   要求整标签可见就得给内容两端各留一列宽的内边距，会让可视区变窄（Q-16）。
2. **筛选是草稿 - 应用模型，但胶囊改了不会提示「未应用」**：只有【开始查找】从禁用变
   可用这一处暗示。是否要加角标或改成即时生效，属产品决定（Q-15）。
3. **记录中心摘要的「15 小时前更新」是相对当前时间算的**，睡眠记录以入睡时间为基准，
   读起来像「15 小时前才更新过这条记录」，实际含义是「上一次睡眠开始于 15 小时前」（Q-17）。

## 8.38 睡眠筛选即时生效 + 记录卡重做 + 判脏基线（第三十六轮）

### 8.38.1 统计筛选：撤掉【开始查找】，改即时生效

上一版把【记物价】的「草稿 - 应用」模型整套搬到了睡眠统计（§8.37.3），这一轮撤掉：

- **代价不对等**。物价那边改一次条件要重跑全库聚合，值得先攒好再算；睡眠统计只有几十条记录，
  点一下的代价就是一次过滤求和，中间那道按钮只剩下「多一次点击」。
- **中间态不可见**。胶囊改了但没按【开始查找】时，三张卡还是旧结果，界面上没有任何提示说明
  「你看到的是上一次的条件」（Q-15 就是这么来的）。去掉按钮，这个状态就不存在了。

顺带两处：

1. 时间范围改成 **全部 / 近 7 天 / 近 30 天 / 近 90 天**，【全部】在最左——按「限制由弱到强」读，
   开头才是「什么都不限」；放在末尾时用户点完【近 7 天】想退回来，要把整行扫一遍。
2. 【清除筛选】从「和开始查找并排各占一半」改为条件行下方右对齐的一枚小按钮。
   筛选卡里最醒目的元素应该是条件本身；一枚占满整行的按钮放在卡顶，会让打开统计的第一眼
   看到的不是「我在看哪段时间」，而是「一个能点的按钮」。

### 8.38.2 类别文本卡抽成一个组件

`SleepKindBadge`（`ui/sleep/SleepKindBadge.kt`）：代表色实心底 + 白字，圆角 `radii.pill`。
【记录】列表卡、【统计】当日明细行、【记录】页概览卡的「最近一次」三处共用。

- 三处此前各画一份：列表卡是实心、明细行是 12% 淡底 + 同色字，同一个「夜间睡眠」在两屏里
  长成两个样子。形状相同代表它们是同一类标签，这是读者跨屏建立映射的唯一线索。
- 概览卡的标题从 `最近一次 · 小憩`（把类别拼进字符串）改成「最近一次」+ 一张标签：
  类别不再是文案的一部分，语言切换不会把标签拼丢，颜色也不再需要另写一遍。

### 8.38.3 记录卡：四层信息 + 底部动作行

旧卡是三行挤在一起：徽标 + 时长、`22:30 → 06:00 · 夜尿 2 次`、备注，编辑删除两枚图标压在
时间行右端。问题在于「几点睡的」和「夜尿几次」同字同级，而删除这个不可逆动作紧贴时间文本。

新结构（自上而下每层换一种字级）：

| 层 | 内容 | 字级 / 颜色 |
| --- | --- | --- |
| 1 | 类别标签 + 时长（进行中则「● 进行中」） | 标签实心代表色；时长 `titleS`，**染代表色** |
| 2 | 入睡时间 / 醒来时间，两列各带标签 | 标签 `caption` + 图标；值 `numeric` + SemiBold |
| 3 | 夜尿次数（有备注再跟一行） | `caption` + `textMuted` |
| 4 | 分隔线 → 右端【删除】文字按钮 | danger 色（§8.39 改为图标 + 两字，并去掉编辑按钮） |

- 时长染代表色是这一轮唯一新增的颜色用法：一列卡片扫下来先按颜色分组，再看数字，
  比读两遍标签快；深浅两套主题下都过了正文对比度（靛蓝 / 暖橙本身是给文字用的深色档）。
- 两个时刻竖排：它们要能左右对齐着比（时长几乎就是这两列的减法），横排成 `22:30 → 06:00`
  会和第三层的细节混在同一级。
- 动作行下沉：整卡仍可点击进编辑，右端不再堆图标。低频动作不该占第一行的高度。

### 8.38.4 「放弃未保存的修改」：判定基准与返回键归属

这一轮最费劲的一处，问题有三层，逐层看：

1. **基准错了**。旧判定是 `current != existing`。新增时 `existing` 恒为 `null`，
   于是**刚点开【记夜间睡眠】、一个字都没改，按返回也会被问「放弃未保存的修改」**（Q-12）。
   现在基准是 `initialForm`——进页面那一下的表单，新增时就是按偏好算好的默认起止时间。
   默认值是系统给的建议，不算用户改动。
2. **保存按钮不该跟着判脏**。新增页允许「原样保存」：预填的 8 小时本身就是一条能入账的记录，
   用户一路点进来直接保存应当成立；只有编辑页才要求「有改动」。
3. **两个 `BackHandler` 抢同一个返回键**。`SleepRecordActivity` 有一个路由级
   `BackHandler(enabled = route != LIST)`，编辑页自己也有一个。两个都开着时，谁先收到事件
   取决于注册顺序——**真机表现是顶栏箭头会弹确认框、系统返回键直接退走**，同一个动作两种后果。
   现在路由级只管设置页那两层，编辑页的返回键完全归编辑器。

> **坑（值得单记一笔）**：编辑器的 `requestBack` 是通过 `BackHandler` 注册的，它在更早一次组合里
> 就被捕获了；闭包里读 `hasChanges` / `current` 这类**派生 val**，拿到的是那一刻的快照。
> 真机现象：切到「小憩」后按系统返回，判定仍读到切之前的「夜间睡眠」，于是直接退走、不弹框。
> 解决方式不是在回调里传值，而是让回调**现场读 State**——`snapshot()` 从 `remember` 出来的
> `kind` / `sleepStartAt` 等 State 重新拼一条记录（State 对象跨组合稳定，读到的永远是最新值）。
> 同一条规则也用在 `save()` 上，否则「改完就保存」可能存下旧值。

判定本身抽成了纯函数 `sleepFormHasChanges(current, baseline, timerVisible, timerEnabled)`，
配套 9 条单测（新增未改 / 换类别 / 改时间 / 改备注 / 编辑未改 / 关掉已有计时器 / 醒来时间在过去时
拨开关不算 / 在未来时算 / `sleepTimerVisible` 三种输入）。这类逻辑在真机上失效时不崩不报错，
只能靠断言兜住。

### 8.38.5 对话框按钮：一个实心主操作 + 一个 danger 描边

`DiscardChangesDialog`（全站编辑器共用）的两个动作从文字按钮改成按钮：

- 【保存修改】= `AppIconTextButton`（实心主色），这是这一屏唯一的主操作；
- 【放弃修改】= `AppOutlinedIconTextButton(danger = true)`（danger 描边 + danger 文字），
  它同样会关掉这一屏，但代价是丢内容——形状与颜色必须同时说明这一点；
- 右上角的关闭仍然保留，「我再想想」不需要第三个按钮。

`AppOutlinedIconTextButton` 新增 `danger` 参数（默认 `false`），其余调用点不变。
**只在不可逆动作上用**：描边 + 红字已经是这一屏最刺眼的一块，同一处再并列第二个红，
读者就得先猜哪个才是「点了就回不去」（仍保留图标与文案，颜色不是唯一线索）。

### 8.38.6 实测（Redmi K80 AVD，1440×3200，density 3.5）

| 场景 | 结果 |
| --- | --- |
| 统计筛选卡 | 只剩两行条件 + 右下一枚【清除筛选】；范围胶囊「全部 / 近 7 天 / 近 30 天 / 近 90 天」 |
| 点【全部】 | 趋势立刻只画有数据的两天（10-31、11-01）；【清除筛选】转禁用态（灰字 + 删除线） |
| 记录卡 | 标签实心、时长染代表色、入睡/醒来两列、夜尿·记录于、分隔线、编辑/删除文字按钮 |
| 概览卡 | 「最近一次」+ 实心标签；换小憩记录后标签即时变暖橙、大数字跟着变 |
| 统计当日明细 | 行首是实心标签，与列表卡同一个组件、同一套颜色 |
| 新增页原样返回 | 不弹框（Q-12 已修），直接回列表 |
| 新增页改类别后系统返回 | 弹框；【放弃修改】→ 回列表；【保存修改】→ 落库为「小憩 1 小时 30 分」 |
| 编辑页未改动 | 保存条禁用；返回不弹框 |
| 编辑页改类别后返回 | 弹框（顶栏箭头与系统返回键行为一致） |
| 删除记录 | 确认框仍是文字按钮（未改动）；删完列表回到单条 |
| 深 / 浅两套 | 标签、时长染色、danger 描边、禁用删除线都完整可读 |

**回归**：`assembleDebug` 通过；全量 348 项 342 过，仍是既有的
`FoodNutrientRepositoryTest` 6 项失败（Q-6），无新增失败。

### 8.38.7 发现但未改

1. **夜尿次数没进记录**。编辑页的「夜尿次数」步进器只改 `nocturiaCount`，而落库的是
   `nocturia` 列表；不点【根据夜尿次数生成默认夜尿数据】就保存，记录里夜尿是 0 次——
   界面上写了 3、存下来是 0（Q-18）。
2. **删除确认框仍是文字按钮**。它走的是另一条 `AlertDialog`（不是 `DiscardChangesDialog`），
   与刚刚改过的「放弃修改」样式不统一（Q-19）。

## 8.39 记录卡收口：点卡即编辑、删除只留一枚（第三十七轮）

上一版卡底还是一条「【编辑睡眠】【删除睡眠记录】」双按钮行，第三层还挂着「记录于 2026-11-01 04:56」。
这一轮按「一个动作一个入口」收掉：

### 8.39.1 编辑只留「点卡片」

- 删除卡底的【编辑睡眠】。CK 的 `clickable` 本来就覆盖整张卡（§4.6 可点区域要有可视提示，
  卡片自身的抬升面板 + 右侧时长已经承担了「这是一条可进的对象」），再放一枚文字按钮等于
  同一件事两个入口——用户还得先想「点按钮还是点卡」。
- `SleepCard` 的 `onEdit` 参数随之删掉，只留 `onOpen`（`EventScreen` 里本来就是传 `onOpen`，
  调用面反而更窄了）。

### 8.39.2 删除：图标 + 两字，卡右侧竖居中

- 文案从「删除睡眠记录」缩到「删除」（`sleep_delete` 三语种同步；确认框的正文仍写全称，
  那里脱离卡片语境，得说清删的是什么）。
- 位置是**卡片右侧、上下居中**（外层 `Row(verticalAlignment = CenterVertically)`，内容列
  `weight(1f)`），不是右下角单独一行：竖着叠要为它空出一整行（分隔线 + 两个 12 dp 间距
  ≈ 60 dp），而按钮本身只有 40 dp 高——卡片高度几乎全是为排版付的。
  改完后单卡高度 568 → 354 px（约 61 dp），删除按钮中心与卡片中心严格重合（实测都在 y=1540）。
- 夜尿次数留在内容列里、紧跟时间行的下方，只在夜间且有记录时出现。

### 8.39.3 两个排版陷阱（都只有上真机才看得见）

1. **`AppDestructiveTextButton` 放进 Row 会撑满剩余宽度**。它内部给文字挂了 `weight(1f)`，
   按钮因此按最大约束测量：文字看着仍贴左，实际右半个卡片都成了可点的删除热区
   （误触直接弹「确认删除」）。卡里改用本地的 `SleepCardDeleteButton`（`TextButton` +
   `Icon` + `Text`，按内容测量），宽度就只有「图标 + 删除」那一截。
2. **`weight(1f, fill = false)` 会让后面的元素到不了右边缘**。夜尿文字一开始写成
   `weight(1f, fill = false)`：用不满的那份额度留在行尾、不再重新分配，删除按钮因此
   停在离右端 84 dp 的地方（实测按钮右边缘 x=1037，卡片内容右边缘 x=1331）。
   改成 `weight(1f)` 后两张卡的删除按钮 x 区间一致（1216–1314）。

### 8.39.4 实测

| 场景 | 结果 |
| --- | --- |
| 记录卡 | 标签 + 时长 / 入睡·醒来 / 夜尿（可选）/ 备注（可选），右侧竖居中【🗑 删除】；无编辑按钮、无分隔线、无「记录于」 |
| 删除按钮位置 | 两张卡 x 区间都是 1216–1314，中心 y 与卡片中心重合（1540） |
| 单卡高度 | 568 → 354 px（去掉动作行省下约 61 dp） |
| 点卡片任意处（含内容区留白） | 进【编辑睡眠】；留白不会误触删除 |
| 点【删除】 | 弹出删除确认框，取消后回到列表 |
| 深 / 浅两套 | danger 红字与图标对比度足够；夜尿文字 `textMuted` 可读 |

**回归**：`assembleDebug` 通过；全量 355 项 349 过，仍是既有的 `FoodNutrientRepositoryTest`
6 项失败（Q-6），无新增。

## 8.40 入睡与醒来：按默认设置一键推另一端（第三十八轮）

【添加睡眠】里的两个时刻一直靠手填。默认设置里其实已经写明了「习惯几点记」和「一般睡多久」，
但这两条信息在编辑器里完全没露面——用户得自己记住「我一般睡 8 小时」，再心算 22:00 + 8h。

### 8.40.1 规则：按钮挂在「被推导的那一端」

默认设置的 `nightTiming / napTiming`（`SleepRecordTiming`）决定谁是锚点：

| 记录时机 | 锚点（用户填的那端） | 推出来的那端 | 按钮出现在 |
| --- | --- | --- | --- |
| 睡前记录 `BEFORE_SLEEP` | 入睡时间 | 醒来时间 = 入睡 + 默认时长 | 醒来时间行 |
| 起床记录 `AFTER_WAKE` | 醒来时间 | 入睡时间 = 醒来 − 默认时长 | 入睡时间行 |

- 默认时长按类别分别取（夜间 8 小时 / 小憩 90 分钟），见 `sleepDefaultMinutes()`。
- 按钮只在**锚点有值**时出现：算不出结果时挂一个不能用的按钮，只是挡路。
- 两个字段各挂一个「互相推」的按钮反而更糟——用户得先猜哪个才是默认设置里说的那个方向。
- 只有按钮被点才改值。改入睡时间不会偷偷把醒来时间一起改掉：用户可能就是想录一段异常短的睡眠。

### 8.40.2 落地

- `model/sleep/SleepPrefs.kt` 新增三个纯函数：`sleepDefaultMinutes` / `sleepDerivedField` /
  `deriveSleepCounterpart`（时长为 0 时按 1 分钟兜底，否则按钮点了等于没点）。配 7 条单测。
- `RecordTimePickerField` 新增 `assistIconRes / assistLabel / onAssistClick` 三个可选参数，
  与已有的 `reset*`（设为现在）共用抽出来的 `RecordTimeFieldAction`。
  **没标签就不画**：第一版只判 `onAssistClick != null`，结果入睡时间行上挂出一枚光秃秃的
  `<` 图标按钮——它既不说明自己要干什么，还和行尾的 `>` 撞在一起。
- 图标用 `ic_refresh`（重算），不用箭头：行尾本来就有一个 `>`，两个箭头并排读起来像「两页」。

### 8.40.3 实测

| 场景 | 结果 |
| --- | --- |
| 睡前记录 + 8 小时 | 入睡改到 22:00 → 点按钮 → 醒来变次日 06:00，本次时长 8 小时 |
| 起床记录 + 8 小时 | 醒来改到 07:00 → 点按钮 → 入睡变前一天 23:00，本次时长 8 小时 |
| 锚点为空（如醒来时间未填） | 该行按钮不渲染——按实现判定，未在真机专门造一条「无醒来时间」的记录验证 |
| 小憩 | 按 90 分钟推（单测覆盖，界面同一条路径） |
| 深 / 浅两套 | 按钮用 `primaryContainer`，与行内文字、`>` 都分得开 |

## 9. 待决事项

| 编号 | 事项 | 影响 | 需要谁定 |
| --- | --- | --- | --- |
| Q-1 | 是否采用 `AppFoodKindAccessible`（D-3） | 需同步修改 `AGENTS.md` 的固定色不变量 | 产品 / 设计 |
| Q-2 | `AGENTS.md` 的退出动效 140 ms 是否改为 150 ms（D-5） | 一处常量 + 一份规范 | 工程 |
| Q-3 | 主色是否由绿改蓝（D-4） | 影响主题、品牌感、`FoodKind` 可辨识度 | 产品 / 设计 |
| Q-4 | `FontTokens` 一次性替换还是保留兼容层过渡 | 500 处调用点的改造节奏 | 工程 |
| Q-5 | 是否现在做 Compose Multiplatform 依赖替换（仅 `androidTarget()`） | 决定将来加 iOS 是否要再动 541 个 Composable | 工程 |
| Q-6 | `FoodNutrientRepositoryTest` 6 项失败（`food:animal_fat:lard` 未标 oil、`food:seasoning:soy_sauce` 有体积无密度、`zingiber_officinale:raw` 未标 seasoning 等） | 全量单测跑不绿；不修就一直在噪音里找回归 | 数据 / 测试维护方 |
| Q-6 | `AppDataTable` 是否增加「随父滚动」模式 | 详情页营养素表（480 dp 内滚）与页面滚动嵌套，用户需先滚完表才能翻页 | 工程 |
| Q-7 | ~~`AppBottomSheet` 是否支持「内容区自行滚动」模式~~ **已解决** | 新增 `contentScrollable = false`，见 §8.22；物价两个选择器已改回抽屉 | 工程 |
| Q-9 | ~~全局主题是否现在就由 `HealthDietProTheme` 切到 `AppTheme`~~ **已解决** | `HealthDietProTheme` 已改为 `AppTheme` 的包装，全站一套色一套字号，见 §8.22 | 产品 / 设计 |
| Q-8 | 个人信息页是否恢复「特殊病史」入口 | 现在 `showDiseasePicker()` 仍在、但界面没有入口，`selectedDiseaseIds` 只读不可改（本轮未擅自加，属产品决定） | 产品 |
| Q-10 | `AppBottomSheet` 是否让出软键盘高度 | 搜索型抽屉（对比对象、物价物品）在键盘弹起后只露 3 行，可滚但与动线有摩擦（§8.35.9） | 工程 |
| Q-11 | 营养数据补齐维生素 / 矿物质 / 氨基酸 | 263 条记录里只有黄油一条完整，对比页七张分组卡平时只出现一张（§8.35.9） | 数据 |
| Q-12 | ~~新建睡眠记录未填任何内容时返回，是否还要弹「放弃未保存的修改？」~~ **已解决** | 判脏基准改为 `initialForm`（进页面那一下的表单），新增页原样返回不再弹框，见 §8.38.4 | 产品 / 工程 |
| Q-13 | 删除睡眠记录是否连带删除其计时器实例 | 现在 `TimerArchiveStore` 会留下孤儿实例，看不见但一直存着（§8.36.9） | 工程 |
| Q-14 | 同一天多条睡眠记录是否要提示合并 | 「日均睡眠」会出现 24 小时这种读着别扭的数（§8.36.9） | 产品 |
| Q-15 | ~~睡眠统计的筛选是否改成即时生效~~ **已解决** | 【开始查找】已撤掉，点胶囊即刻重算三张卡；【清除筛选】退到条件行右下方，见 §8.38.1 | 产品 |
| Q-16 | 时间轴滚到最右时最左的边缘日期被切一半 | 90 天档位显示成「25」；要整标签可见就得两端各留一列内边距，可视区变窄（§8.37.7） | 设计 |
| Q-17 | 记录中心「记睡眠」摘要用入睡时间算「N 小时前更新」 | 语义是「上一次睡眠开始于 N 小时前」，与「更新」二字不符（§8.37.7） | 产品 |
| Q-18 | 编辑页「夜尿次数」是否改成「改次数即生成默认时段」 | 现在次数只是生成工具，不点生成就保存则记录里夜尿 0 次——界面写 3、存下来 0（§8.38.7） | 产品 |
| Q-19 | 删除确认框（与「放弃修改」同一屏）是否也换成实心 + danger 描边按钮 | 现在是 `AlertDialog` 默认文字按钮，与 §8.38.5 刚统一的样式不一致（§8.38.7） | 设计 |
