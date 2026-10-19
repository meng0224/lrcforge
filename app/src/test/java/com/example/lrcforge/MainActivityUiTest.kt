package com.example.lrcforge

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.example.lrcforge.util.SettingsManager
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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
}
