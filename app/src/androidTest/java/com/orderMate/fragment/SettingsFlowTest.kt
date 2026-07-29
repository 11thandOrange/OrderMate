package com.orderMate.fragment

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orderMate.R
import com.orderMate.activities.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real instrumented coverage for the settings flow (fragment/SettingsFragment.kt), reached via
 * the side nav (MainActivityNavigationTest already covers the nav click itself activating
 * navSettingsIndicator - this class covers what SettingsFragment actually renders once there).
 * Grounded directly in the fragment's own onViewCreated()/switchToTab():
 *
 * - switchToTab("general") is the initial state set up when the fragment's tabs are wired
 *   (fragment_settings_redesign.xml has panelGeneral visible, the rest View.GONE, by default
 *   in the layout XML itself, matching switchToTab's own show/hide logic).
 * - switchToTab() toggles panel visibility via View.VISIBLE/View.GONE directly (not an
 *   animation or fragment transaction), so clicking a tab and asserting the corresponding
 *   panel's visibility is a direct, real behavioral check.
 *
 * Same overlay-permission dialog dismissal as MainActivityNavigationTest/OrderListFlowTest
 * (MainActivity.onResume(), always shown on a bare CI emulator).
 */
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun dismissOverlayPermissionDialogAndNavigateToSettings() {
        try {
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
        } catch (e: NoMatchingViewException) {
            // Overlay permission already granted (or dialog not shown for some other
            // reason) - nothing to dismiss.
        } catch (e: NoMatchingRootException) {
            // No dialog root present either - same as above.
        }

        onView(withId(R.id.navSettings)).perform(click())
    }

    @Test
    fun settings_generalPanelIsDisplayedByDefault() {
        onView(withId(R.id.panelGeneral)).check(matches(isDisplayed()))
        onView(withId(R.id.panelFilter)).check { view, _ ->
            org.junit.Assert.assertEquals(View.GONE, view.visibility)
        }
    }

    @Test
    fun clickingFilterTab_showsFilterPanel_hidesGeneralPanel() {
        onView(withId(R.id.tabFilter)).perform(click())

        onView(withId(R.id.panelFilter)).check(matches(isDisplayed()))
        onView(withId(R.id.panelGeneral)).check { view, _ ->
            org.junit.Assert.assertEquals(View.GONE, view.visibility)
        }
    }
}
