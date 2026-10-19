package com.example.lrcforge

import android.content.Context
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.lrcforge.util.SettingsManager
import com.example.lrcforge.model.SubtitleFile
import com.example.lrcforge.model.FileStatus
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {

    @Before
    @After
    fun resetSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, false, false))
    }

    @Test
    fun coldLaunchShowsExpressiveEmptyStateAndPinnedImportAction() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnConvert)).check(matches(allOf(isDisplayed(), withText(R.string.select_files))))
            onView(withId(R.id.emptyStateContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.tvEmptyFormats)).check(matches(withText("支援 VTT、ASS、SSA、SRT、STR、SMI、SUB")))
        }
    }

    @Test
    fun coldLaunchShowsDefaultStorageSummary() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.tvStorageModeChip)).check(matches(withText("預設")))
            onView(withId(R.id.tvOutputDir)).check(matches(withText("預設應用下載目錄")))
            onView(withId(R.id.btnSelectOutputDir)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnConvert)).check(matches(isDisplayed()))
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

    @Test
    fun malformedSubtitleShowsLineDiagnosticWithoutPartialOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("conversion-error-", ".srt", context.cacheDir)
        try {
            source.writeText("1\ninvalid --> 00:00:03,000\nHello", Charsets.UTF_8)
            val file = SubtitleFile(Uri.fromFile(source), source.name, source.length())
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val field = MainActivity::class.java.getDeclaredField("files").apply { isAccessible = true }
                    @Suppress("UNCHECKED_CAST")
                    val files = field.get(activity) as MutableList<SubtitleFile>
                    files.add(file)
                    activity.findViewById<RecyclerView>(R.id.recyclerView).adapter!!.notifyDataSetChanged()
                }
                onView(withId(R.id.btnConvert)).perform(click())
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
                var completed = false
                while (!completed && System.nanoTime() < deadline) {
                    scenario.onActivity {
                        completed = file.status == FileStatus.ERROR || file.status == FileStatus.SUCCESS
                    }
                    if (!completed) Thread.sleep(20)
                }
                assertTrue("Conversion did not finish", completed)
                assertEquals(FileStatus.ERROR, file.status)
                assertNull(file.lrcContent)
                onView(withId(R.id.tvErrorMessage)).check(matches(withText(
                    "轉換失敗：SRT 第 2 行，時間格式或數值無效"
                )))
            }
        } finally {
            check(source.delete()) { "Could not delete test subtitle" }
        }
    }
}
