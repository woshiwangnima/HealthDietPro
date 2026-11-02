# 存档安全规则

> 真机上丢过一次数据，原因是签名切换导致卸载。后来排查又发现存档链路本身还有三处会让数据
> 静默消失的缺陷。这份文档记录现在的防护边界，以及**以后改存档格式时必须遵守的规则**。

## 1. 存档放在哪

应用私有目录，卸载即删除，没有例外：

```
/data/data/com.woshiwangnima.healthdietpro/
├── shared_prefs/health_diet_prefs.xml    用户索引：user_archive_ids_v1 / current_user_id
├── shared_prefs/app_prefs.xml            全局偏好
└── files/user_archives/<userId>/
    ├── profile.json                      用户资料
    ├── body_metrics.json                 身高 / 体重 / 围度
    ├── sleep.json、diet.json、prices.json、timers.json ...
    ├── user_preferences.json             该用户的偏好项
    └── manifest.json                     各域校验和与版本（缺失可重建）
```

每个域文件都是一层信封：

```json
{ "domainId": "sleep", "schemaVersion": {"major":1,"minor":4,"patch":0},
  "appVersion": "1.1.0", "updatedAtMillis": 0, "payload": { ... } }
```

`domainId` 用来防止串域读错；`schemaVersion` 是判断兼容性的唯一依据。

## 2. 三道防线

改动集中在 `model/archive/ArchiveFileGuard.kt`，全部存储层共用。

| 防线 | 位置 | 行为 |
|---|---|---|
| 读取容错 | `DomainEnvelope.decodeDomain` | 版本判断是 `<=` 而非 `==`；`ignoreUnknownKeys = true`，多出来的字段直接忽略 |
| 失败隔离 | `decodeDomainSafely` | 文件存在但解析失败时，先复制一份 `<name>.corrupt-<时间戳>` 再返回 null，原始字节不丢 |
| 写入留底 | `prepareArchiveWrite` | 覆盖前把旧文件复制为 `<name>.prev`，误覆盖可退回上一版 |

为什么第一条是「读取」而不是「写入」：问题出在读——旧存档被判非法，异常又被
`runCatching` 吞成 null，`load()` 返回空档案；用户随手记一条，`save()` 就把
「空档案 + 1 条新数据」写回磁盘。写入本身没错，是读的结果被误当成「没有数据」。

## 3. 改存档格式时必须遵守

1. **只加不改**。新字段必须有默认值或可空，不要改已有字段的名字和类型——
   kotlinx.serialization 遇到类型不匹配会直接抛，而抛了就落到「隔离 + 空档案」。
2. **字段加完不必升版本**。旧存档缺字段时由默认值补齐，这是最安全的加法。
3. **语义变更才升 `ArchiveSchemaVersion.Current`**，并在对应领域的 `migrateXxxArchive()`
   里补齐旧数据。升版本前先确认：`decodeDomain` 已放宽到 `<=`，旧存档仍可读。
4. **不要在日常 debug 构建里随手升 schema 版本**。debug 装新版 → 升版本写盘 →
   装回 release 时那份存档比代码新，会被隔离（数据保留但界面显示为空）。要验证迁移，
   用专门的一次构建跑完整流程。
5. **新增存储层**时照抄现有写法：读取用 `json.decodeDomainSafely(file, DOMAIN_ID, ...)`，
   写入用 `prepareArchiveWrite(file)`。不要再用裸 `runCatching { ... }.getOrNull()`。

## 4. debug 与 release 之间互转

能不能覆盖安装，Android 只看「包名 + 签名」。签名不一致会强制要求卸载，而卸载会
连存档一起删——这是那次事故的真正原因。

现在 `app/build.gradle.kts` 里 `debug` 与 `release` 共用 `releaseKey` 签名：

```kotlin
debug   { signingConfig = signingConfigs.getByName("releaseKey") }
release { signingConfig = signingConfigs.getByName("releaseKey") }
```

因此 Android Studio 直接 Run 的包与 `assembleRelease` 的 APK 可以互相覆盖，存档保留。
`debuggable` 由 buildType 决定，与签名无关，调试能力不受影响。

**前提**：`keystore.properties` 存在（见 `docs/RELEASE_BUILD.md`）。缺这个文件时两者
都退回未签名 / debug 签名，又会回到互相覆盖不了的状态。

> 切换签名的那一次仍然需要卸载（设备上装的是旧签名的包）。顺序是：
> 应用内导出 JSON → 卸载 → 装新包 → 导入。导入会覆盖当前用户，先确认导出成功。

## 5. 出事后怎么救

按代价从低到高：

1. **`<name>.prev`**：上一版存档。直接改名覆盖回去即可，代价是丢最后一次写入。
2. **`<name>.corrupt-<时间戳>`**：解析失败时的原始字节。多半是版本不符或某个字段类型
   变了，手工修正 JSON 后改名回正式文件。
3. **应用内导出**：`我的 → 导出存档`，是跨设备、跨安装唯一可靠的通道，支持加密。
4. **adb**：仅对 `debuggable` 包有效（`run-as` / `adb backup`）。release 包拉不出来。

`.corrupt-*` 每个文件最多保留 3 份，避免反复读取把磁盘写满。

## 6. 尚未覆盖的场景

| 场景 | 现状 | 缺口 |
|---|---|---|
| 被迫卸载 | 无自动备份 | 只有手动导出。可加「每日打包写到 `Download/HealthDietPro/`」（MediaStore，无需权限，卸载不删） |
| 装回更旧版本 | 隔离保留，不覆盖 | 界面表现为「没有数据」。可加版本闸门：检测到更高版本时提示并禁止写入 |
| 从没导出过的用户 | 无保护 | 首次进入时提醒一次 |

前两项是已知待办，不是遗漏。
