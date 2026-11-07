import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val backend = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun configString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val offlineChecks = providers.gradleProperty("diarOffline").orNull == "true"

android {
    buildFeatures { buildConfig = true }
    namespace = "com.example.diar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.diar"
        minSdk = 24
        targetSdk = 37
        versionCode = 11
        versionName = "1.3.3"

        buildConfigField("String", "SUPABASE_URL", configString(if (offlineChecks) "" else backend.getProperty("supabase.url", "")))
        buildConfigField("String", "SUPABASE_KEY", configString(if (offlineChecks) "" else backend.getProperty("supabase.publishableKey", "")))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}