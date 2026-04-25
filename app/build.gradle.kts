plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
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
        minSdk = 26
        targetSdk = 37
        versionCode = 29
        versionName = "2.15.2"
        base.archivesName = "$namespace-$versionName"

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
        create("Full") {
            dimension = "version"
            buildConfigField("Boolean", "SHIZUKU", "true")
        }
        create("Play") {
            buildConfigField("Boolean", "SHIZUKU", "false")
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
        aidl = true
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

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.timber)

    implementation(libs.coil.compose)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.preview)

    /**
     * Not actually used explicitly, however it is used by glance widgets.
     * The problem is that the version that the latest glance widget release uses is 2.7.1
     * which _for some reason_ breaks after R8.
     *
     * Without this manual import the widget still works fine on debug but on release it never loads
     * throwing WorkManager errors into the logs.
     *
     * Really really stupid and I hope I'll be able to remove this some time in the future.
     */
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.haze)
    implementation(libs.haze.materials)
}