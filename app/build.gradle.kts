import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.aboutLibraries)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

android {
    namespace = "com.leekleak.trafficlight"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.leekleak.trafficlight"
        minSdk = 28
        targetSdk = 37
        versionCode = 43
        versionName = "3.0-beta2"
        base.archivesName = "$namespace-$versionName"

        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    flavorDimensions += "version"
    productFlavors {
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        val admobAppId = localProperties.getProperty("admob.app.id") ?: "ca-app-pub-3940256099942544~3347511713"
        val admobUnitIdOverview = localProperties.getProperty("admob.unit.id.overview") ?: "/21775744923/example/native"

        create("full") {
            dimension = "version"
            buildConfigField("Boolean", "SHIZUKU", "true")
            buildConfigField("Boolean", "ADS", "false")
        }
        create("play") {
            dimension = "version"
            buildConfigField("Boolean", "SHIZUKU", "false")
            buildConfigField("Boolean", "ADS", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_UNIT_ID_OVERVIEW", "\"$admobUnitIdOverview\"")
            manifestPlaceholders["admobAppId"] = admobAppId
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    lint {
        abortOnError = false
    }
}

aboutLibraries {
    library {
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.GROUP
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    implementation(libs.kotlinx.serialization.json)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.androidx.compose.adaptive)

    implementation(libs.androidx.lifecycle.service)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.timber)

    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)

    /**
     * Not actually used explicitly, however it is used by glance widgets.
     * The problem is that the version that the latest glance widget release uses is 2.7.1
     * which _for some reason_ breaks after R8.
     *
     * Without this manual import the widget still works fine on debug but on release it never loads
     * throwing WorkManager errors into the logs.
     *
     * Really, really stupid and I hope I'll be able to remove this some time in the future.
     */
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.haze)
    implementation(libs.haze.materials)
    "playImplementation"(project(":playIntegration"))
    "playImplementation"(libs.ads.mobile.sdk)
    "fullImplementation"(project(":shizukuIntegration"))

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
