plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.leekleak.core"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        minSdk = 28
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)
    api(libs.androidx.navigation3.runtime)
    api(libs.haze)

    implementation(libs.haze.blur)
    implementation(libs.haze.blur.materials)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
}
