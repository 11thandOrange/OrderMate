package com.orderMate.fragment

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
 * Real instrumented coverage for the order list flow (fragment/OrderListRedesignFragment.kt),
 * MainActivity's default nav destination (nav_graph.xml startDestination). Grounded directly
 * in the fragment's own setupClickListeners()/showFilterDialog():
 *
 * - filterButton/syncButton/resetButton (shared_orders_header.xml, included as "header") are
 *   the real header controls setupClickListeners() wires up.
 * - showFilterDialog() calls FilterCategoryBuilder.buildCategories(allItemList, ...) with
 *   allItemList empty on a bare emulator with no synced Clover orders - buildCategories()
 *   still returns the default (non-Clover-data-dependent) categories in that case, so the
 *   dialog is expected to open cleanly. filterSectionsContainer (dialog_filters.xml) is the
 *   one stable id in that dialog to assert against.
 *
 * Verified against a real emulator run (OrderMate#101/#102 CI): MainActivity.onResume()
 * unconditionally shows an overlay-permission AlertDialog when Settings.canDrawOverlays()
 * is false, which it always is on a fresh CI emulator - this dialog becomes the focused
 * root and blocks Espresso from matching any underlying activity view until dismissed,
 * hence the @Before dismissal below.
 */
@RunWith(AndroidJUnit4::class)
class OrderListFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun dismissOverlayPermissionDialog() {
        try {
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
        } catch (e: NoMatchingViewException) {
            // Overlay permission already granted (or dialog not shown for some other
            // reason) - nothing to dismiss.
        } catch (e: NoMatchingRootException) {
            // No dialog root present either - same as above.
        }
    }

    @Test
    fun orderList_isTheDefaultDestination_headerControlsAreDisplayed() {
        onView(withId(R.id.filterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.syncButton)).check(matches(isDisplayed()))
        onView(withId(R.id.resetButton)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingFilterButton_opensFilterDialog() {
        onView(withId(R.id.filterButton)).perform(click())

        onView(withId(R.id.filterSectionsContainer)).check(matches(isDisplayed()))
    }
}
