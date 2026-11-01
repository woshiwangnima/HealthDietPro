# HealthDietPro · Release 构建发布流程

本文件是**唯一权威流程**，后续发布直接照此执行，不用重新摸索。
适用：打包 release APK / AAB 并发布到 GitHub Release。
产物路径约定见 §4，GitHub Release **由用户手动创建上传**（不代传，见 §7）。

---

## 0. 环境前置（每台机器只需确认一次）

| 项 | 值 | 说明 |
|---|---|---|
| JDK | `C:/Program Files/Android/Android Studio/jbr` | **系统 PATH 里没有 java**，`gradlew` 不设 `JAVA_HOME` 会直接报 `JAVA_HOME is not set` |
| Android SDK | `C:\Users\WSW\AppData\Local\Android\Sdk` | |
| build-tools | `37.0.0`（同时装了 36.0.0 / 36.1.0） | `apksigner.bat` 在这里 |
| `uv` | `C:/Users/WSW/AppData/Local/Programs/Python/Python314/Scripts/uv` | `compileFoodCatalog` / `buildFoodImages` 靠它，缺了会构建失败 |
| platform-tools | `C:\Users\WSW\AppData\Local\Android\Sdk\platform-tools` | adb；**但不得用于调试/截图/改分辨率**，见 §8 |

统一前缀（后面所有命令都带）：

```bash
cd "c:/Users/WSW/AndroidStudioProjects/HealthDietPro"
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:/c/Users/WSW/AppData/Local/Android/Sdk/platform-tools:$PATH"
```

> Git Bash 下 `adb shell cat /sdcard/...` 会被 MSYS 路径转换搞坏。
> 需要 adb 时先 `export MSYS_NO_PATHCONV=1`。

## 1. 版本号

`app/build.gradle.kts` → `android.defaultConfig`：

```kotlin
versionCode = 1      // 整数，每次发布必须 +1
versionName = "1.0.0"
```

`minSdk 35` / `targetSdk 36` / `compileSdk release(36) minorApiLevel 1`，一般不动。
**是否升版由用户决定，不要自作主张。**

## 2. 签名密钥

首次发布时生成（已完成，勿重复生成）：

| 文件 | 位置 | 是否入库 |
|---|---|---|
| 密钥库 | `<项目根>/hdpro-release.p12` | 否（`.gitignore` 的 `*.p12`） |
| 口令文件 | `<项目根>/keystore.properties` | 否（`.gitignore`） |

内容：

```properties
storeFile=hdpro-release.p12
storePassword=<见文件>
keyAlias=hdpro
keyPassword=<见文件>
storeType=pkcs12
```

`app/build.gradle.kts` 里的接线（已存在）：`signingConfigs.releaseKey` 读该文件，
**仅当文件存在时**才挂到 `release` buildType——所以在没有密钥的机器上 `assembleRelease`
仍能跑通，只是产出未签名包。

> **密钥库丢了就无法再发布该包名的更新**（装新包会报签名不匹配）。换机器务必带上
> `hdpro-release.p12` + `keystore.properties`。

## 3. 构建命令

```bash
# 1) 编译校验（快，~10s）
./gradlew compileDebugKotlin

# 2) 全量单测（~10s）
./gradlew testDebugUnitTest

# 3) release APK（~90s）
./gradlew assembleRelease

# 4) release AAB（Google Play，~10s，依赖 assemble 产物）
./gradlew bundleRelease
```

产物：

| 变体 | 路径 |
|---|---|
| APK | `app/build/outputs/apk/release/app-release.apk` |
| AAB | `app/build/outputs/bundle/release/app-release.aab` |

当前基线：APK ≈ 58 MB，AAB ≈ 21 MB（体积主要来自 `assets/food_catalog/images`）。
release 的 `optimization.enable = false`，**未开启混淆/压缩**，所以体积与 debug 接近。

## 4. 签名校验（必做）

```bash
"C:/Users/WSW/AppData/Local/Android/Sdk/build-tools/37.0.0/apksigner.bat" \
  verify --print-certs app/build/outputs/apk/release/app-release.apk
```

期望看到 `V2 Signer:` 且 DN 为 `CN=HealthDietPro, OU=Dev, O=woshiwangnima, L=Shenzhen, ST=Guangdong, C=CN`。

- AAB **不能**用 apksigner 校验（不是 APK 格式，会报 `Missing AndroidManifest.xml`），属正常。
- 装到已有 debug 包的设备上会报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（签名不同），
  这本身**反证 release 包确实用了独立密钥签名**，不是构建问题。

## 5. 单测基线

`testDebugUnitTest` 当前 **356 项，350 过，6 失败**。
失败的 6 项全部是 `FoodNutrientRepositoryTest`（food 资产标签/密度数据问题，编号 Q-6），
**与本轮改动无关，属既有问题**。只要不是新出现的失败，就算通过。

## 6. Git 提交与推送（有坑，见 §8）

```bash
git status --short          # 先确认没有把密钥/口令文件加进去
git add -A
git commit -m "<中文简要说明>"
git push origin master
```

提交前必查：`keystore.properties`、`hdpro-release.p12` **不得出现在 `git status` 里**。
用 `git check-ignore -v keystore.properties hdpro-release.p12` 确认已被忽略。

## 7. 发布到 GitHub Release（用户手动）

仓库：`https://github.com/woshiwangnima/HealthDietPro`（公开，默认分支 `master`）。

用户侧操作：GitHub → Releases → Draft a new release → 新建 tag（如 `v1.0.0` 指向 `master`）
→ 上传 `app-release.apk` → Publish。

需要 GitHub Release 里的说明时，按本仓库约定写中文条目，参考 `git log` 的提交标题风格
（如「【记物价】【记睡眠】UI优化」）。

---

## 8. 已知坑（按踩过的顺序）

### 坑 1：`assembleRelease` 报 implicit dependency（已修）

`lintVitalAnalyzeRelease` 直接扫 `app/src/main/assets`，而这个目录由
`compileFoodCatalog` / `buildFoodImages` 写入。原配置只给 `merge*Assets` 加了 `dependsOn`，
lint 那一支漏了，Gradle 9 直接判构建失败。

修法（`app/build.gradle.kts`）：

```kotlin
tasks.configureEach {
    val readsAssets = name == "preBuild" ||
        (name.startsWith("merge") && name.endsWith("Assets")) ||
        name.startsWith("lint")
    if (readsAssets) dependsOn(compileFoodCatalog, buildFoodImages)
}
```

### 坑 2：`git push` 报 `CRYPT_E_NO_REVOCATION_CHECK`（未修，需绕）

本机所有 HTTPS 出口都被本地代理（`http://127.0.0.1:10214`）用
`CN=SteamTools Certificate` 重新签发。系统信任该 CA，但 schannel 取不到它的 CRL，
于是握手失败——**git 默认后端和 Windows `curl` 都挂**；只有 `gh`（Go TLS，默认不查吊销）能通。

可用命令：

```bash
git -c http.sslBackend=openssl \
    -c http.sslCAInfo="C:/Users/WSW/.certs/full-ca-bundle.crt" \
    push origin master
```

`full-ca-bundle.crt` = PortableGit 自带 `ca-bundle.crt` + 从 `Cert:\CurrentUser\Root`
导出的 SteamTools CA（已在 `C:/Users/WSW/.certs/`）。

**`http.schannelCheckRevoke=false` 无效，别再试。**

### 坑 3：行尾

仓库里 Kotlin / XML 一律 **CRLF**。用 Python 读写这类文件会把行尾改成 LF，
改完必须转回 CRLF，否则 diff 满屏。

### 坑 4：不要用 adb 调试（用户明令）

历史事故：用 `adb shell wm size` 改测试机逻辑分辨率后没 reset，重启仍生效。
**用户裁定：不再用 adb 调试 / 截图 / 改分辨率，代码写完由用户自己装真机验收。**
（近期为核验 UI 短暂用过 adb，后续应停止。）

## 9. Gradle 9 提示（不影响构建）

构建末尾会打 `Deprecated Gradle features ... incompatible with Gradle 10`，
属工具链版本提示，不是本仓库错误，可忽略。

---

## 10. 一页速查（复制即用）

```bash
cd "c:/Users/WSW/AndroidStudioProjects/HealthDietPro"
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleRelease
./gradlew bundleRelease

"C:/Users/WSW/AppData/Local/Android/Sdk/build-tools/37.0.0/apksigner.bat" \
  verify --print-certs app/build/outputs/apk/release/app-release.apk

git add -A && git status --short
git commit -m "<说明>"
git -c http.sslBackend=openssl \
    -c http.sslCAInfo="C:/Users/WSW/.certs/full-ca-bundle.crt" push origin master
```
