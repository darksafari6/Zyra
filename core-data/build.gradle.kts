plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.safari.zyra.core.data"
    compileSdk = 36
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx-test-core)
    androidTestImplementation(libs.androidx-test-ext-junit)
    androidTestImplementation(libs.androidx-test-runner)
}
