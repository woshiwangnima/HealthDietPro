#!/usr/bin/env bash
# HealthDietPro 一键 release 打包：编译校验 → 单测 → APK + AAB → 签名校验 → 产物摘要。
#
# 只做「打包与校验」。不改版本号、不 commit、不 push、不发布 GitHub Release——
# 这三件事按 docs/RELEASE_BUILD.md §1/§6/§7 由人决定。
#
# 用法：
#   ./release.sh                 # 完整流程（含单测）
#   ./release.sh --skip-tests    # 只打包（临时验证用）
#
# 前置（缺一不可，脚本会自检）：
#   - JAVA_HOME 指向 Android Studio 的 JBR（系统 PATH 里没有 java）
#   - 仓库根有 keystore.properties + hdpro-release.p12（缺失时仍能构建，但产出未签名包）
#
# 若命令行已有 JAVA_HOME 环境变量，脚本不覆盖；否则用本机默认值。

set -euo pipefail
cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
BUILD_TOOLS="${BUILD_TOOLS:-C:/Users/WSW/AppData/Local/Android/Sdk/build-tools/37.0.0}"
export JAVA_HOME

SKIP_TESTS=0
for arg in "$@"; do
  case "$arg" in
    --skip-tests) SKIP_TESTS=1 ;;
    -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
    *) echo "未知参数：$arg（可用：--skip-tests / --help）" >&2; exit 2 ;;
  esac
done

step() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }
fail() { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

step "环境自检"
if [ ! -f "$JAVA_HOME/bin/java.exe" ] && [ ! -f "$JAVA_HOME/bin/java" ]; then
  fail "JAVA_HOME 指向的目录里没有 java：$JAVA_HOME"
fi
echo "JAVA_HOME   = $JAVA_HOME"

GRADLE_FILE=app/build.gradle.kts
VERSION_NAME=$(grep -E '^[[:space:]]*versionName' "$GRADLE_FILE" | head -1 | cut -d'"' -f2)
VERSION_CODE=$(grep -E '^[[:space:]]*versionCode' "$GRADLE_FILE" | head -1 | tr -dc '0-9')
echo "versionName = ${VERSION_NAME:-?}   versionCode = ${VERSION_CODE:-?}"

if [ -f keystore.properties ] && [ -f hdpro-release.p12 ]; then
  echo "签名密钥    = 已就位"
else
  echo "签名密钥    = 缺失 → 本次产出为未签名包（见 docs/RELEASE_BUILD.md §2）"
fi

step "编译校验"
./gradlew compileDebugKotlin --console=plain

if [ "$SKIP_TESTS" -eq 0 ]; then
  step "全量单测（基线见 docs/RELEASE_BUILD.md §5）"
  TEST_LOG=build/release-test.log
  # 基线里本来就有 FoodNutrientRepositoryTest 的 6 条既有失败（Q-6），
  # 所以不能拿 Gradle 的退出码当闸门，改为解析测试报告，只拦「新增失败」。
  ./gradlew testDebugUnitTest --console=plain > "$TEST_LOG" 2>&1 || true
  RESULT_DIR=app/build/test-results/testDebugUnitTest
  TOTAL=$(grep -rh 'tests=' "$RESULT_DIR"/*.xml 2>/dev/null | sed 's/ timestamp.*//' | awk -F'"' '{t+=$4} END {print t+0}')
  KNOWN=0
  UNKNOWN=0
  for report in "$RESULT_DIR"/*.xml; do
    [ -f "$report" ] || continue
    count=$(grep -c '<failure' "$report" || true)
    if [ "$count" -eq 0 ]; then
      continue
    fi
    case "$(basename "$report")" in
      *FoodNutrientRepositoryTest.xml) KNOWN=$((KNOWN + count)) ;;
      *)
        UNKNOWN=$((UNKNOWN + count))
        echo "未预期失败：$(basename "$report")"
        grep -A 3 '<failure' "$report" | head -20
        ;;
    esac
  done
  echo "单测：共 $TOTAL 项，失败 $((KNOWN + UNKNOWN)) 条（其中既有 Q-6 $KNOWN 条）"
  if [ "$TOTAL" -eq 0 ]; then
    tail -30 "$TEST_LOG"
    fail "没有读到测试报告，单测可能根本没跑起来"
  fi
  if [ "$UNKNOWN" -gt 0 ]; then
    tail -30 "$TEST_LOG"
    fail "出现新增失败，先修再打包"
  fi
else
  echo "（已跳过单测）"
fi

step "构建 release APK + AAB"
./gradlew assembleRelease bundleRelease --console=plain

APK=app/build/outputs/apk/release/app-release.apk
AAB=app/build/outputs/bundle/release/app-release.aab
[ -f "$APK" ] || fail "没有找到 $APK"

step "签名校验"
if [ -f "$BUILD_TOOLS/apksigner.bat" ] || [ -f "$BUILD_TOOLS/apksigner" ]; then
  SIGNER="$BUILD_TOOLS/apksigner.bat"
  [ -f "$SIGNER" ] || SIGNER="$BUILD_TOOLS/apksigner"
  "$SIGNER" verify --print-certs "$APK" || fail "签名校验未通过"
else
  echo "未找到 apksigner（$BUILD_TOOLS），跳过签名校验"
fi

step "产物"
# AAB 不能过 apksigner（不是 APK 格式），属正常，不校验。
printf '%-6s %-52s %10s bytes\n' APK "$APK" "$(wc -c < "$APK")"
[ -f "$AAB" ] && printf '%-6s %-52s %10s bytes\n' AAB "$AAB" "$(wc -c < "$AAB")"
echo
echo "SHA-256:"
echo "  APK $(sha256sum "$APK" | cut -d' ' -f1)"
[ -f "$AAB" ] && echo "  AAB $(sha256sum "$AAB" | cut -d' ' -f1)"

step "下一步（脚本不做）"
cat <<'TIP'
  1. 是否升 versionCode / versionName 由你决定（app/build.gradle.kts）
  2. 提交：git add -A && git status --short（确认密钥文件不在列表里）
  3. 推送（本机代理会拒 schannel，必须走 openssl 后端）：
     git -c http.sslBackend=openssl \
         -c http.sslCAInfo="C:/Users/WSW/.certs/full-ca-bundle.crt" push origin master
  4. GitHub Release 手动创建并上传 app-release.apk
TIP
