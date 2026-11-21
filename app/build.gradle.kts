import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

fun configuredString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(character)
        }
    }
    append('"')
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val supabaseUrl = providers.gradleProperty("knotSupabaseUrl")
    .orElse(localProperties.getProperty("knotSupabaseUrl").orEmpty())
val supabaseKey = providers.gradleProperty("knotSupabasePublishableKey")
    .orElse(localProperties.getProperty("knotSupabasePublishableKey").orEmpty())
val knotApiUrl = providers.gradleProperty("knotApiUrl")
    .orElse(localProperties.getProperty("knotApiUrl", "https://mwzkvyackmetcjrpzahu.supabase.co/functions/v1/knot-api"))

android {
    namespace = "com.taile.knot"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.taile.knot"
        minSdk = 29
        targetSdk = 36
        buildConfigField("String", "SUPABASE_URL", configuredString(supabaseUrl.get()))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", configuredString(supabaseKey.get()))
        buildConfigField("String", "KNOT_API_URL", configuredString(knotApiUrl.get()))
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Lifecycle (ViewModel & LiveData) cho MVVM
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.4")

    // Glide hỗ trợ load & cache ảnh mượt mà
    implementation("com.github.bumptech.glide:glide:4.16.0")

}
