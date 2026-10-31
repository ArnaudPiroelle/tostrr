import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * The release signing key, from Gradle properties (on CI, `ORG_GRADLE_PROJECT_tostrr…` environment variables) or else
 * from a git-ignored `signing.properties` at the project root. Without it the release build is left unsigned.
 */
val signing = Properties().apply {
    rootProject.file("signing.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    listOf("tostrrStoreFile", "tostrrStorePassword", "tostrrKeyAlias", "tostrrKeyPassword").forEach { key ->
        providers.gradleProperty(key).orNull?.let { setProperty(key, it) }
    }
}

android {
    namespace = "app.tostrr"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.tostrr"
        minSdk = 26
        targetSdk = 36
        // The release workflow sets the code from its run number; bump the name to publish a new release from main.
        versionCode = providers.gradleProperty("tostrrVersionCode").orNull?.toInt() ?: 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (signing.getProperty("tostrrStoreFile") != null) {
            create("release") {
                storeFile = file(signing.getProperty("tostrrStoreFile"))
                storePassword = signing.getProperty("tostrrStorePassword")
                keyAlias = signing.getProperty("tostrrKeyAlias")
                keyPassword = signing.getProperty("tostrrKeyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // English (the default resources) and French only: libraries' other translations are left out of the APK.
    androidResources {
        localeFilters += listOf("en", "fr")
    }
    // Robolectric renders the screens on the JVM: it needs the merged resources and manifest.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ksp {
    // Fails the build when a Koin definition has a dependency nothing provides.
    arg("KOIN_CONFIG_CHECK", "true")
}

dependencies {
    implementation(project(":data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // The empty activity the Compose test rule hosts screens in.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
}
