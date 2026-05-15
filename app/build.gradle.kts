// GalaxyPods 앱 모듈 빌드 스크립트
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Firebase plugins는 google-services.json 추가 후 활성화
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.galaxypods.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.galaxypods.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Google Maps API 키 — local.properties의 MAPS_API_KEY를 주입.
        // local.properties는 .gitignore 대상. 누락 시 placeholder 키 사용 (지도 표시 X).
        val mapsApiKey =
            providers.gradleProperty("MAPS_API_KEY").orNull
                ?: System.getenv("MAPS_API_KEY")
                ?: "MISSING_KEY"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // GitHub Pages URL 상수 (BuildConfig 노출).
        // 우선순위. gradle property > env > 기본값(firstlover99)
        val pagesBase =
            providers.gradleProperty("PAGES_BASE_URL").orNull
                ?: System.getenv("PAGES_BASE_URL")
                ?: "https://firstlover99.github.io/galaxypods"
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$pagesBase/privacy-ko/\"")
        buildConfigField("String", "TERMS_URL", "\"$pagesBase/terms-ko/\"")
        buildConfigField("String", "DATA_DELETION_URL", "\"$pagesBase/data-deletion-ko/\"")
    }

    signingConfigs {
        // 릴리스 키는 로컬 keystore 추가 후 환경 변수로 주입
        // create("release") { ... }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs +=
            listOf(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/licenses/**"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Maps & Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Firebase (google-services.json 추가 후 활성화)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.crashlytics)
    // implementation(libs.firebase.analytics)

    // Test — JVM
    testImplementation(libs.bundles.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // Test — Android
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
