package com.orbin.minimal.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

private const val PACKAGE_NAME = "com.orbin.minimal"
private const val WAIT_TIMEOUT_MS = 10_000L

/**
 * Records startup + followed-boards feed paths for ahead-of-time ART compilation.
 *
 * Needs a rooted emulator or unlocked device. Prefer the **Baseline profile** workflow, or:
 *
 * ```
 * gradle :app:generateReleaseBaselineProfile
 * ```
 *
 * Output lands in `app/src/release/generated/baselineProfiles/` and should be committed.
 */
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndFeed() {
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), WAIT_TIMEOUT_MS)
            device.findObject(By.scrollable(true))?.let { feed ->
                repeat(SCROLL_PASSES) {
                    feed.fling(Direction.DOWN)
                    device.waitForIdle()
                }
            }
        }
    }

    private companion object {
        const val SCROLL_PASSES = 3
    }
}
