package com.leekleak.trafficlight.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

private const val PACKAGE_NAME = "com.leekleak.trafficlight"

class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() {
        rule.collect(PACKAGE_NAME, includeInStartupProfile = true) {
            prepareDevice()

            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }
    }

    @Test
    fun userJourneys() {
        rule.collect(PACKAGE_NAME, includeInStartupProfile = false) {
            prepareDevice()

            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            navigateTo("Plans")
            navigateTo("History")
            scrollDown()
            navigateTo("Tools")
            scrollDown()
            navigateTo("Overview")
            scrollDown()

            navigateTo("Tools")
            navigateTo("Notification")
            device.pressBack()
            navigateTo("Overlay")
            device.pressBack()
            navigateTo("iPerf3")
            device.pressBack()
        }
    }
}

private fun MacrobenchmarkScope.prepareDevice() {
    device.executeShellCommand("appops set $PACKAGE_NAME GET_USAGE_STATS allow")
    device.executeShellCommand("pm grant $PACKAGE_NAME android.permission.POST_NOTIFICATIONS")
    grantIgnoreBatteryOptimizations()
}

private fun MacrobenchmarkScope.grantIgnoreBatteryOptimizations() {
    device.executeShellCommand(
        "am start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:$PACKAGE_NAME"
    )
    val allow = device.wait(Until.findObject(By.text(Pattern.compile("(?i)allow"))), 5_000)
    if (allow != null) {
        allow.click()
    } else {
        device.pressBack()
    }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.navigateTo(label: String) {
    device.findObject(By.text(label))?.click()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.scrollDown() {
    device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.5f)
    device.waitForIdle()
}
