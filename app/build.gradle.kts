import org.gradle.api.tasks.Exec
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

val compileFoodCatalog = tasks.register<Exec>("compileFoodCatalog") {
    description = "Compile maintained food catalog records into Android assets."
    group = "food catalog"
    workingDir(rootProject.projectDir)
    commandLine("uv", "run", "python", "tools/food_catalog/catalog_tool.py", "compile")
    inputs.dir(rootProject.file("tools/food_catalog/source/records"))
    inputs.file(rootProject.file("app/src/main/assets/DRIs/nutrients_meta.json"))
    outputs.dir(rootProject.file("app/src/main/assets/food_catalog/records"))
    outputs.dir(rootProject.file("app/src/main/assets/food_catalog/indexes"))
    outputs.file(rootProject.file("app/src/main/assets/food_catalog/manifest.json"))
}

val buildFoodImages = tasks.register<Exec>("buildFoodImages") {
    description = "Build reproducible thumbnail and detail images for the food catalog."
    group = "food catalog"
    workingDir(rootProject.projectDir)
    commandLine("uv", "run", "python", "tools/food_catalog/catalog_tool.py", "build-images")
    inputs.dir(rootProject.file("tools/food_catalog/images/source"))
    inputs.dir(rootProject.file("tools/food_catalog/source/records"))
    outputs.dir(rootProject.file("app/src/main/assets/food_catalog/images/thumb"))
    outputs.dir(rootProject.file("app/src/main/assets/food_catalog/images/detail"))
    outputs.file(rootProject.file("app/src/main/assets/food_catalog/images/manifest.json"))
}

// catalog / 图片是生成物，落在 `app/src/main/assets` 里。所有会读到这个目录的任务都必须在生成任务之后跑：
// 只接 `merge*Assets` 时 lint 任务会漏（它直接扫 assets 目录），Gradle 9 会按「隐式依赖」直接判构建失败。
tasks.configureEach {
    val readsAssets = name == "preBuild" ||
        (name.startsWith("merge") && name.endsWith("Assets")) ||
        name.startsWith("lint")
    if (readsAssets) {
        dependsOn(compileFoodCatalog, buildFoodImages)
    }
}

android {
    namespace = "com.woshiwangnima.healthdietpro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.woshiwangnima.healthdietpro"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release 签名：口令只放在仓库外的 keystore.properties（已 gitignore），
    // 文件不存在时保持未签名构建，换机器/CI 也能正常 assembleRelease。
    signingConfigs {
        create("releaseKey") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply {
                    propsFile.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
                storeType = props.getProperty("storeType") ?: "pkcs12"
            }
        }
    }

    buildTypes {
        release {
            // 只有密钥齐全时才挂签名，否则仍产出未签名包、构建不失败。
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("releaseKey")
            }
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        dataBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.core.ktx)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.reorderable)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
