import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { load(it) }
}
val mapTilerKey: String = localProperties.getProperty("maptiler.api_key") ?: ""

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "ao.ridehailing.driver"
    compileSdk = 35

    defaultConfig {
        applicationId = "ao.ridehailing.driver"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPTILER_API_KEY", "\"${mapTilerKey.escapeForBuildConfig()}\"")
        val baseUrl = providers.gradleProperty("BASE_URL").get()
        val backendUrl = providers.gradleProperty("BACKEND_BASE_URL").get()
        val mqttUser = providers.gradleProperty("MQTT_USERNAME").get()
        val mqttPassword = providers.gradleProperty("MQTT_PASSWORD").get()
        val mqttPin = providers.gradleProperty("MQTT_CERT_PIN").get()
        val useMock = providers.gradleProperty("USE_MOCK_API").get()
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
        buildConfigField("String", "MQTT_USERNAME", "\"$mqttUser\"")
        buildConfigField("String", "MQTT_PASSWORD", "\"$mqttPassword\"")
        buildConfigField("String", "MQTT_CERT_PIN", "\"$mqttPin\"")
        buildConfigField("Boolean", "USE_MOCK_API", useMock)
        buildConfigField("boolean", "DEV_BYPASS_LOGIN", "true")
        // false: app opens on Home. true: AuthScreen (phone + OTP) is the start destination again.
        buildConfigField("boolean", "LOGIN_UI_ENABLED", "false")
        buildConfigField("String", "DEV_DRIVER_ID", "\"D-DEV-001\"")
        buildConfigField("String", "DEV_DRIVER_NAME", "\"Dev Driver\"")
        buildConfigField("String", "DEV_VEHICLE_MAKE", "\"Dev Car\"")
        buildConfigField("String", "DEV_DRIVER_PLATE", "\"LD-0000-XX\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEV_BYPASS_LOGIN", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST}"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    testOptions {
        unitTests.all { test ->
            test.useJUnitPlatform()
        }
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*_Factory*",
        "**/*JsonAdapter*",
        "**/*Module*",
        "**/Hilt_*",
    )
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(excludes) }
    classDirectories.setFrom(kotlinClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.coroutines.android)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.webrtc)
    implementation(libs.hivemq)
    implementation(libs.play.services.location)
    implementation(libs.maplibre)

    testImplementation(libs.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
