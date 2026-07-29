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
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real instrumented coverage for the calendar flow (fragment/CalendarFragment.kt), reached via
 * the side nav (MainActivityNavigationTest already covers the nav click itself activating
 * navCalendarIndicator - this class covers what CalendarFragment actually renders once there).
 * Grounded directly in the fragment's own onViewCreated()/setupClickListeners()/navigateMonth():
 *
 * - filterButton/syncButton/resetButton are shared_orders_header.xml controls, included the
 *   same way OrderListRedesignFragment includes them (fragment_calendar_redesign.xml:14) - same
 *   ids, same shared dialog_filters.xml behavior verified in OrderListFlowTest.
 * - monthYearTitle's text is reassigned by renderMonthView()/renderWeekView()/renderDayView()
 *   every time navigateMonth() re-renders - clicking btnNext changing its text is a real,
 *   direct behavioral assertion rather than relying on internal state.
 *
 * Same overlay-permission dialog dismissal as MainActivityNavigationTest/OrderListFlowTest
 * (MainActivity.onResume(), always shown on a bare CI emulator).
 */
@RunWith(AndroidJUnit4::class)
class CalendarFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun dismissOverlayPermissionDialogAndNavigateToCalendar() {
        try {
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
        } catch (e: NoMatchingViewException) {
            // Overlay permission already granted (or dialog not shown for some other
            // reason) - nothing to dismiss.
        } catch (e: NoMatchingRootException) {
            // No dialog root present either - same as above.
        }

        onView(withId(R.id.navCalendar)).perform(click())
    }

    @Test
    fun calendar_headerAndMonthViewControlsAreDisplayed() {
        onView(withId(R.id.monthYearTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.calendarGrid)).check(matches(isDisplayed()))
        onView(withId(R.id.filterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.syncButton)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingFilterButton_opensFilterDialog() {
        onView(withId(R.id.filterButton)).perform(click())

        onView(withId(R.id.filterSectionsContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingNextMonth_changesMonthYearTitle() {
        var titleBeforeNavigating = ""
        onView(withId(R.id.monthYearTitle)).check { view, _ ->
            titleBeforeNavigating = (view as android.widget.TextView).text.toString()
        }

        onView(withId(R.id.btnNext)).perform(click())

        onView(withId(R.id.monthYearTitle)).check { view, _ ->
            assertNotEquals(titleBeforeNavigating, (view as android.widget.TextView).text.toString())
        }
    }
}
