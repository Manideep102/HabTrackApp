plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Keep this only if you have the compose plugin applied in your top-level build file
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.habtrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.habtrack"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("habtrack_key.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "habtrack"
                keyAlias = "key0"
                keyPassword = "habtrack"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}
dependencies {
    // We use explicit strings here to fix the "Unresolved reference to version catalog" errors
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    // Room Database dependencies
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Compose
    val composeBom = "2025.01.01"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // WorkManager - for scheduling background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Notifications - Core notifications library
    implementation("androidx.core:core:1.15.0")

    // Anthropic Claude API SDK - for AI habit insights
    implementation("com.anthropic:anthropic-java:2.34.0")

    // Encrypted local storage for the user's Anthropic API key
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Health Connect - on-device fitness data sync (steps, calories, distance)
    implementation("androidx.health.connect:connect-client:1.1.0")

    // Test dependencies used by the default ExampleUnitTest / ExampleInstrumentedTest
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
