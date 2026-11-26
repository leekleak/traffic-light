plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.leekleak.trafficlight.baselineprofile"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        missingDimensionStrategy("version", "full")
    }

    targetProjectPath = ":app"

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api33") {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api33"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
}
