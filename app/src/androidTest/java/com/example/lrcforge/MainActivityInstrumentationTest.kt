package com.example.lrcforge

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.lrcforge.util.SettingsManager
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {

    @After
    fun resetSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, false, false))
    }

    @Test
    fun coldLaunchShowsOptimizedEmptyState() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnConvert)).check(matches(allOf(isDisplayed(), withText("選擇文件"))))
            onView(withId(R.id.emptyStateContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.tvEmptyFormats)).check(matches(withText("支援 VTT、ASS、SSA、SRT、STR、SMI、SUB")))
        }
    }

    @Test
    fun coldLaunchShowsDefaultStorageSummary() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvStorageModeChip)).check(matches(withText("預設")))
            onView(withId(R.id.tvOutputDir)).check(matches(withText("預設應用下載目錄")))
            onView(withId(R.id.btnSelectOutputDir)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun sourceDirectoryModeShowsStorageStateOnLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, true, false))

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvStorageModeChip)).check(matches(withText("原目錄")))
            onView(withId(R.id.tvOutputDir)).check(matches(withText("輸出到原文件目錄")))
        }
    }

    @Test
    fun recursiveImportSwitchRestoresEnabledStateOnLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, false, true))

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.switchRecursiveImport)).check(matches(isChecked()))
        }
    }
}
