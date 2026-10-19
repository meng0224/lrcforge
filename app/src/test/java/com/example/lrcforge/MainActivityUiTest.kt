package com.example.lrcforge

import android.view.View
import android.net.Uri
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.example.lrcforge.util.SettingsManager
import com.example.lrcforge.model.SubtitleFile
import com.example.lrcforge.model.FileStatus
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityUiTest {
    @Before
    fun resetSettings() {
        val context = RuntimeEnvironment.getApplication()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, false, false))
    }

    @Test
    fun primaryActionStaysOutsideScrollingContent() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val root = activity.findViewById<LinearLayout>(R.id.rootLayout)
            val scroll = activity.findViewById<NestedScrollView>(R.id.mainScroll)
            val dock = activity.findViewById<LinearLayout>(R.id.actionDock)
            val button = activity.findViewById<MaterialButton>(R.id.btnConvert)
            assertEquals(root, scroll.parent)
            assertEquals(root, dock.parent)
            assertEquals(dock, button.parent)
            assertEquals(activity.getString(R.string.select_files), button.text.toString())
            assertTrue(button.isEnabled)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.secondaryActionsRow).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.emptyStateContainer).visibility)
        }
    }

    @Test
    fun recursiveImportRestoresFolderAction() {
        val context = RuntimeEnvironment.getApplication()
        SettingsManager.saveSettings(context, SettingsManager.fromStoredValues(null, false, true))
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertEquals(
                activity.getString(R.string.select_folder),
                activity.findViewById<MaterialButton>(R.id.btnConvert).text.toString()
            )
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun nightThemeUsesLightSystemBarIcons() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertFalse(activity.resources.getBoolean(R.bool.light_system_bars))
            assertEquals(
                activity.getString(R.string.expressive_subtitle),
                activity.findViewById<TextView>(R.id.tvHeroSubtitle).text.toString()
            )
        }
    }

    @Test
    fun conversionFailureClearsStaleOutputAndShowsLineDiagnostic() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val uri = Uri.parse("content://conversion/broken.ass")
            val content = "[Events]\n" +
                "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Good\n" +
                "Dialogue: 0,broken,0:00:04.00,Default,,0,0,0,,Lost"
            shadowOf(activity.contentResolver).registerInputStream(uri, ByteArrayInputStream(content.toByteArray()))
            val file = SubtitleFile(uri, "broken.ass", content.length.toLong(), FileStatus.ERROR,
                lrcContent = "stale result", outputFileName = "stale.lrc")
            val filesField = MainActivity::class.java.getDeclaredField("files").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val files = filesField.get(activity) as MutableList<SubtitleFile>
            files.add(file)
            activity.findViewById<MaterialButton>(R.id.btnConvert).performClick()
            val busyField = MainActivity::class.java.getDeclaredField("isConversionInProgress").apply {
                isAccessible = true
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (busyField.getBoolean(activity) && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertFalse("Conversion did not finish", busyField.getBoolean(activity))
            assertEquals(FileStatus.ERROR, file.status)
            assertNull(file.lrcContent)
            assertNull(file.outputFileName)
            assertEquals("轉換失敗：ASS/SSA 第 3 行，時間格式或數值無效", file.errorMessage)
            assertTrue(ShadowLog.getLogsForTag("SubtitleConversion").any {
                it.msg.contains("Failed file=broken.ass") && it.throwable?.message?.contains("line 3:") == true
            })
        }
    }

    @Test
    fun invalidEncodingShowsErrorAndPreservesDiagnosticCause() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val uri = Uri.parse("content://conversion/invalid-utf8.srt")
            val bytes = "1\n00:00:01,000 --> 00:00:02,000\nA ".toByteArray() + byteArrayOf(0xC3.toByte(), 0x28)
            shadowOf(activity.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
            val file = SubtitleFile(uri, "invalid-utf8.srt", bytes.size.toLong(), FileStatus.ERROR,
                lrcContent = "stale result", outputFileName = "stale.lrc")
            val filesField = MainActivity::class.java.getDeclaredField("files").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val files = filesField.get(activity) as MutableList<SubtitleFile>
            files.add(file)
            activity.findViewById<MaterialButton>(R.id.btnConvert).performClick()
            val busyField = MainActivity::class.java.getDeclaredField("isConversionInProgress").apply {
                isAccessible = true
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (busyField.getBoolean(activity) && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertFalse("Conversion did not finish", busyField.getBoolean(activity))
            assertEquals(FileStatus.ERROR, file.status)
            assertNull(file.lrcContent)
            assertNull(file.outputFileName)
            assertEquals("轉換失敗: ${activity.getString(R.string.conversion_invalid_utf8)}", file.errorMessage)
            assertTrue(ShadowLog.getLogsForTag("SubtitleConversion").any {
                it.msg.contains("Failed file=invalid-utf8.srt") &&
                    it.throwable?.cause is java.nio.charset.CharacterCodingException
            })
        }
    }
}
