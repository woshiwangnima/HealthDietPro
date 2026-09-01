import org.gradle.api.tasks.Exec

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

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Assets")) {
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

    buildTypes {
        release {
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
