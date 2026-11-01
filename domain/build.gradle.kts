plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    // Only for HttpUrl, to build the car image URL; no networking happens here.
    implementation(libs.okhttp)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(platform(libs.koin.annotations.bom))
    implementation(libs.koin.annotations)
    // No KOIN_CONFIG_CHECK here: the use cases depend on repository interfaces implemented in :data. The whole graph
    // is verified by KoinGraphTest in :app.
    ksp(libs.koin.ksp.compiler)
    testImplementation(libs.junit)
}

// Dates are written in the device's language: the tests expect the French way.
tasks.test {
    systemProperty("user.language", "fr")
    systemProperty("user.country", "FR")
}
