# 项目文件梳理：可删除项与待更新项

> 生成日期：2026-11-01 · 基于 `git ls-files`（1528 个跟踪文件）+ Android Lint `lintDebug` 全量扫描
> 
> 检测口径：Lint `UnusedResources`（210 条）与自研脚本（正则扫 `R.xxx.name` / `@xxx/name` 全量引用）
> 交叉验证，两者在 string 175/175、drawable 16/16、dimen 8/8 上完全吻合。
>
> **本文只做梳理，不代删。** 涉及删除的操作需逐项确认后再执行（见 §5）。

---

## 0. 项目现状速览

| 指标 | 值 |
|---|---|
| git 跟踪文件 | 1528 |
| Kotlin 文件 | 403（主源 337 / 单测 66） |
| 字符串（三语种） | 1841 / 1840(en) / 1841 |
| drawable | 99 |
| res 布局 | **无 layout 目录**（已全面 Compose） |
| release APK | 58 MB |
| Lint | **17 error / 376 warning** |

---

## 1. 可以直接删除：未使用的资源（Lint 确认 210 条）

### 1.1 string（175 条）

按前缀分布：

| 前缀 | 数量 | 说明 |
|---|---|---|
| `nutrition_*` | 32 | 多为 `nutrition_editor_*`、`nutrition_recipe_timer_*`、`nutrition_nutrient_*`、`nutrition_help_*` |
| `medication_*` | 24 | 集中在 `medication_timing_*`（6）、`medication_record_spec_*`（3）、`medication_catalog_*` |
| `blood_*` | 18 | `blood_glucose_trend_level_*`（5）、`blood_glucose_relative_minutes*`（3） |
| `view_*` | 14 | `view_chart_*`（坐标轴/平移提示，旧 View 图表体系残留） |
| `profile_*` | 13 | `profile_plain_json_*`（6，明文 JSON 导入导出）、`profile_user_*_at` |
| `text_*` | 11 | `text_overflow_*`（自动缩字号/降级策略） |
| `diet_*` | 11 | `diet_meal_subtotal_*`、`diet_statistics_*`、`diet_container_empty_mass` |
| `compose_*` | 11 | `compose_chart_fullscreen_area_*`（5）、`compose_chart_crosshair_basis_*` |
| `test_*` | 5 | `test_access_*`（测试入口密码/手势盘，未接线） |
| `reminder_*` | 5 | `reminder_settings_*`（喝水/用药/经期提醒，未接线） |
| `bmi_*` | 5 | `bmi_result_*`（分级结果已被 `BmiUtil.kt` 硬编码取代） |
| `disease_*` | 4 | `disease_duration_*`（3）、`disease_record_family_relation` |
| `water_*` | 3 | `water_statistics_*` |
| `tab_*` | 3 | `tab_nutrition_title` / `tab_record_title` / `tab_profile_title` |
| `sleep_*` | 3 | `sleep_kind_title`、`sleep_history`、`sleep_statistics_kind_all` |
| `search_*` / `record_*` | 6 | `search_activity_scroll_*`、`record_search_*` |
| 其它 | 5 | `container_cross_section`、`detail_tab_records`、`body_record_date`、`price_item_hint`、`user_settings_unit_preferences`、`settings_default_font_size_desc`、`medication_record_*` |

完整清单可从 Lint 报告重新导出：

```bash
R=app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt
grep "UnusedResources\]$" $R | grep -oE "R\.string\.[A-Za-z0-9_.]+"
```

> **注意**：`data_table_vertical_scroll_hint` 同时命中「未使用」和「缺 en 翻译」两条，
> 属死资源，删掉即可，不必补翻译。

### 1.2 drawable（16 个）

按来源可归为三类，都是旧 View 体系的遗留：

| 文件 | 归属 |
|---|---|
| `avatar_circle.xml`、`card_user_info.xml` | 用户卡/头像背景 |
| `bg_nav_center.xml`、`bg_nav_center_selected.xml`、`tab_center_capsule.xml` | 旧自定义 TabBar 中心凸起 |
| `tab_default_bg.xml`、`tab_selected_bg.xml`、`tab_pill_selected.xml` | 旧 TabBar 背景选择器 |
| `card_bg.xml`、`edit_text_bg.xml`、`input_bg.xml`、`ripple_bg.xml` | 通用 View 背景/水波纹 |
| `marker_bg.xml`、`timeline_thumb_bg.xml` | 旧图表标记/时间轴滑块 |
| `ic_cleaning_services.xml`、`ic_fullscreen_exit.xml` | 图标（未被引用） |

> 与之配套的 `res/values/attrs.xml` 里的 `<declare-styleable name="TabBar">` 也已无代码使用
> （`common/ui/DetailTabBar.kt` 是 Compose 组件，与它无关）。

### 1.3 其它 res（19 项）

| 类型 | 条目 |
|---|---|
| dimen（8） | `text_size_headline` / `text_size_subtitle` / `text_size_body` / `text_size_label` / `text_size_caption` / `text_size_micro` / `view_chart_label_width` / `view_chart_label_height` |
| color（6） | `black` / `white` / `nav_center_bg` / `nav_center_bg_selected` / `nav_icon_tint` / `nav_label_color` |
| array（4） | `chart_type_options` / `time_range_options` / `medication_catalog_frequencies` / `medication_catalog_timings` |
| raw（1） | `bmi_classification.json` |

> `raw/bmi_classification.json` 是重点：BMI 分级已硬编码在 `ui/profile/chart/BmiUtil.kt`
> 的 `BmiBand(18.5f, 24f, ...)` 里，这个 JSON 是重复数据源，删之可避免两份分级不同步。

---

## 2. 可以删除：历史调试产物与草稿

### 2.1 已入库、但明显是一次性产物

| 路径 | 说明 |
|---|---|
| `tools/device-backups/` | 6 个 `adb pull` 出来的设备 `health_diet_prefs.xml` 备份（7 月），含真实用户偏好数据 |

### 2.2 未入库（被 gitignore 挡住）的工作区残留

| 路径 | 体积 | 说明 |
|---|---|---|
| `tools/agp_pdf/input/agp_report.pdf` | 1.5 MB | **用户的真实健康报告 PDF，含隐私，建议删除或移出项目** |
| `tools/agp_pdf/output/` | 488 KB | 该 PDF 解析出来的中间产物 |
| `tools/*.mjs`（5 个）+ `blood_glucose_prediction_lab.py` | ~96 KB | 血糖预测离线拟合/回测实验脚本（9–10 月） |
| `tools/BLOOD_GLUCOSE_PREDICTION_LAB.md`、`FINAL_OFFLINE_SCHEME.md` | 7 KB | 上述实验的笔记 |
| `cache/` | 752 KB | 7 月地区数据抓取的调试缓存 |
| `docs/launcher_icon_*.png`（37 张） | ~2.6 MB | v2–v9 图标设计草稿，定稿后无保留价值 |
| `app/build/`、`build/`、`.gradle/`、`.kotlin/` | 765 MB+ | 构建产物，`./gradlew clean` 可清 |
| `.probe/` | 550 KB | 3 张调试截图 |
| `.superpowers/brainstorm` | — | 空目录 |

### 2.3 一处配置矛盾（需二选一）

`.gitignore` 末尾有 `tools/`，整体忽略 tools 目录；但 `git ls-files` 显示 tools 下仍有
**281 个已跟踪文件**（因为 gitignore 不影响已跟踪文件）。这导致：
根目录下的新实验脚本被静默忽略（上面 2.2 那些 .mjs 就是这么来的），而子目录的旧文件仍在库里。

建议：保留 `tools/food_catalog`、`tools/agp_pdf`、`tools/device-backups` 的现有白名单，
把根级的 `tools/`、`app/src/debug/AndroidManifest.xml` 两行挪到合适位置并加注释说明用途。

---

## 3. 待更新项

### 3.1 构建配置（收益最高）

| 项 | 现状 | 建议 | 收益 |
|---|---|---|---|
| **`optimization { enable = false }`** | release 未启用 R8 | **改为 `true`** | dex 从 **52 MB**（classes.dex 30.7 + classes2 8.8 + classes3 13.4）压缩下来，APK 预计降到 15 MB 以内 |
| **`dataBinding = true`** | 开启 | **删除整行** | 项目无 `res/layout` 目录、`R.layout` 引用数为 0，纯死配置，每轮构建白白跑 databinding 注解处理器 |
| `versionCode / versionName` | `1 / 1.0.0` | 首个 release 已发出，下次发版应升 `2 / 1.1.0` | 便于区分已分发版本 |

### 3.2 依赖版本（Lint 报告 13 项可升级）

| 依赖 | 当前 | 最新 | 风险 |
|---|---|---|---|
| AGP (`com.android.application`) | 9.2.1 | 9.3.1 | 低 |
| **compileSdk** | 36 | **37** | 中，需完整回归 |
| **compose-bom** | **2024.12.01** | **2026.08.00** | **高**：跨了约 20 个月版本，Material3 / foundation API 有 breaking change，需单独安排一轮 |
| Kotlin（含 compose / serialization 插件） | 2.2.10 | 2.4.21 | 中 |
| androidx.core:core-ktx | 1.12.0 | 1.19.0 | 低 |
| androidx.appcompat:appcompat | 1.6.1 | 1.8.0 | 低 |
| androidx.activity:activity-ktx | 1.8.0 | 1.13.0 | 低 |
| androidx.activity:activity-compose | 1.9.3 | 1.13.0 | 低 |
| androidx.navigation:navigation-compose | 2.8.5 | 2.9.8 | 中 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 | 2.11.0 | 中 |
| com.google.android.material:material | 1.10.0 | 1.14.0 | 低 |
| androidx.constraintlayout:constraintlayout | 2.1.4 | 2.2.2 | 低（且见下） |
| androidx.test.ext:junit | 1.1.5 | 1.3.0 | 低 |
| androidx.test.espresso:espresso-core | 3.5.1 | 3.7.0 | 低 |
| com.google.code.gson:gson | 2.10.1 | 2.15.0 | 低（且见下） |
| kotlinx-serialization-json | 1.7.3 | 1.12.0 | 低 |
| **sh.calvin.reorderable:reorderable** | 2.4.3 | **3.1.0** | **高**：major 升级，API 不兼容，需改代码 |

**可考虑移除的依赖**（Kotlin 代码零引用）：

- `androidx.constraintlayout:constraintlayout` —— 无引用
- `androidx.fragment:fragment-ktx` —— 无引用
- `com.google.android.material:material` —— 无引用（主题用的是 `Theme.Material3`，
  由 compose-material3 提供，不依赖这个库）

> `androidx.appcompat:appcompat` **不要删**：`BaseActivity`/`BaseBackActivity` 继承
> `AppCompatActivity`，且 `AppCompatDelegate` 用于夜间模式切换。

### 3.3 必须修的 Lint error（17 条，全是 strings.xml）

`lintDebug` 因这些 error 直接判失败，**`lintVitalRelease` 同样会拦截**，
也就是说 release 流程存在隐患（`assembleRelease` 当前能过是因为未触发 lintVital）。

根因：营养说明类文案里含百分号（如 `carbohydrate 50–65%, fat 20–30%`），
被当成格式化串，而 `%` 后面跟的不是合法转换符。

涉及 key：

| key | 触发片段 |
|---|---|
| `nutrition_help_energy_ratio_guideline` | `% a`、`%, f`（×2，en/默认各一次） |
| `nutrition_help_nutrients_intro` | `% s` |
| `nutrition_help_edible_example` | `% e` |

修法（二选一）：

```xml
<!-- 方案 A：声明这不是格式化串 -->
<string name="nutrition_help_energy_ratio_guideline" formatted="false">...50–65%, fat 20–30%...</string>

<!-- 方案 B：转义百分号 -->
<string name="nutrition_help_energy_ratio_guideline">...50–65%%, fat 20–30%%...</string>
```

三个语种（`values`、`values-en`、`values-zh`）都要改。

### 3.4 待更新的迁移状态（README 与代码不一致）

README 里多处把项目描述为「迁移中」，但实际已迁移完成：

| README 表述 | 实际情况 |
|---|---|
| 「从原 XML + DataBinding 单 Activity 多 Fragment 迁移至 Jetpack Compose」 | 已完成：无 layout 目录、`R.layout` 引用 0 |
| 「持久化: SharedPreferences **过渡到** DataStore」 | 需核实是否还有大量 SharedPreferences |
| 「序列化: kotlinx.serialization（**替换** Gson）」 | 未完成：仍有 6 个文件在用 Gson（`ChartTimeConfigRepository`、`ProfilePrefs`、`UserMetadata`、`ProvinceRepository`、`RegionRepository` 等） |

建议：更新 README 的技术栈章节，把「过渡中」改成完成状态，并明确 Gson 的收尾计划。

### 3.5 文档待处理

| 项 | 说明 |
|---|---|
| `docs/` 下 11 个 md 未入库 | 被 `.gitignore` 的 `docs/*` 挡住，只有 5 个在白名单里 |
| 其中 `DESIGN.md` 被 README 正文引用 | 但没入库 —— 新克隆的仓库打开 README 会指向一个不存在的文件 |
| `UI_SPEC_PROJECT.md`、`UI_REFACTOR_PLAN.md` | UI 规范与重构计划，可能是当前开发依据 |
| `docs/build_regions_json.py` | 地区数据构建脚本，未入库 |

建议：把 README 引用的 `docs/DESIGN.md` 和仍在用的 `UI_SPEC_PROJECT.md` 加进白名单。

---

## 4. 建议保留（看似冗余但有用）

| 路径 | 保留理由 |
|---|---|
| `tools/food_catalog/`（含 `images/source/`） | 构建期生成 `app/src/main/assets/food_catalog`，`.gitignore` 里已明确写了「必须保持可跟踪」，删了别人拉下来就编不出来 |
| `tools/agp_pdf/parse_agp_pdf.py` 等脚本 | AGP 报告导入流程的实现，配套 README 已入库（只需清掉 input/output 里的个人数据） |
| `app/src/main/assets/location/`（2.1 MB） | 运行时离线地区数据 |
| `.workbuddy/`、`.venv/` | 工作区配置与 Python 虚拟环境，已忽略 |
| `keystore.properties`、`hdpro-release.p12` | release 签名，已忽略但**必须备份** |

---

## 5. 建议的执行顺序（每项需确认后再动）

1. **修 17 条 Lint error**（§3.3）—— 先解除 release 流程隐患
2. **删 210 条未使用资源**（§1）—— 改完立刻 `./gradlew assembleDebug` 验证
3. **构建配置三项**（§3.1）—— 开 R8 后**必须完整回归**，尤其是序列化与反射相关代码
4. **依赖升级**（§3.2）—— 低风险的先升；`compose-bom` 与 `reorderable` 单独安排
5. **清理历史产物**（§2.1 / §2.2 / 隐私 PDF）
6. **更新 README / 文档白名单**（§3.4 / §3.5）

> 每完成一步就提交一次，方便回滚。推送命令见 `docs/RELEASE_BUILD.md` §6。

---

## 6. 复现检测的命令

```bash
cd /c/Users/WSW/AndroidStudioProjects/HealthDietPro
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"

# 未使用资源权威报告
./gradlew lintDebug
# 报告路径：
# app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt

# 只看未使用资源
R=app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt
grep "UnusedResources\]$" $R | grep -oE "R\.[a-z]+\.[A-Za-z0-9_.]+" | awk -F. '{print $2}' | sort | uniq -c
```
