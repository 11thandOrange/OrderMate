package com.orderMate.fragment

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orderMate.R
import com.orderMate.activities.MainActivity
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
 * Not independently verified against a real emulator run - this environment has no Android
 * SDK/emulator available. First real CI run (agent-ops#5 / OrderMate#101) is the actual
 * verification.
 */
@RunWith(AndroidJUnit4::class)
class OrderListFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
