package com.orderMate.fragment.orderDetail

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clover.sdk.v3.order.Order
import com.clover.sdk.v3.order.PaymentState
import com.orderMate.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real instrumented coverage for the order-detail/"checkout" screen
 * (fragment/orderDetail/OrderDetailFragment.kt), launched in isolation via FragmentScenario
 * instead of through real navigation - there's no synced Clover order to click through to on a
 * bare CI emulator, so this constructs a minimal Order and passes it the same way SafeArgs nav
 * does (OrderDetailFragmentArgs.Builder(order).build().toBundle()).
 *
 * Order() is a no-arg constructor with chainable setters, confirmed against the real
 * com.clover.sdk:clover-android-sdk:304 classes (Maven Central) rather than assumed.
 *
 * onViewCreated()/onResume() both call into Order/AppsConnector, which throw when there's no
 * Clover account registered (guaranteed true on a bare CI emulator) - previously unguarded,
 * fixed alongside this test in OrderDetailFragment.kt (exceptionHandler{}/try-catch around
 * those calls) since nothing had ever exercised this fragment in CI before.
 */
@RunWith(AndroidJUnit4::class)
class OrderDetailFlowTest {

    private fun buildTestOrder(): Order = Order()
        .setId("test-order-1")
        .setCurrency("USD")
        .setTotal(1999L)
        .setState("open")
        .setPaymentState(PaymentState.OPEN)
        .setCreatedTime(System.currentTimeMillis())

    @Test
    fun orderDetail_rendersOrderIdAndKeyFields_fromNavArgs() {
        val args = OrderDetailFragmentArgs.Builder(buildTestOrder()).build().toBundle()

        launchFragmentInContainer<OrderDetailFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_OrderAppClover
        )

        onView(withId(R.id.orderId)).check(matches(withText("Order test-order-1")))
        onView(withId(R.id.totalValue)).check(matches(isDisplayed()))
        onView(withId(R.id.orderPlacedStatusValue)).check(matches(isDisplayed()))
        onView(withId(R.id.paymentStatusBadge)).check(matches(isDisplayed()))
    }
}
