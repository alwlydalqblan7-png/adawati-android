plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.taifdigital.adawati"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taifdigital.adawati"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-rc1"
        manifestPlaceholders["appLabel"] = "أدواتي"
    }

    val releaseStore = providers.environmentVariable("ADAWATI_KEYSTORE_PATH").orNull
    signingConfigs {
        if (releaseStore != null) create("production") {
            storeFile = file(releaseStore)
            storePassword = providers.environmentVariable("ADAWATI_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("ADAWATI_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("ADAWATI_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            manifestPlaceholders["appLabel"] = "أدواتي — تجربة"
        }
        getByName("release") {
            isDebuggable = false
            if (releaseStore != null) signingConfig = signingConfigs.getByName("production")
        }
    }
    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
