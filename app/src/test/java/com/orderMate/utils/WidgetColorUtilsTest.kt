package com.orderMate.utils

import com.orderMate.R
import com.orderMate.modals.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for WidgetColorUtils' per-widget-type color/icon lookups (#139 adds QUANTITY).
 */
class WidgetColorUtilsTest {

    @Test
    fun `every widget type resolves to a color`() {
        assertEquals(WidgetColorUtils.COLOR_CALENDAR, WidgetColorUtils.getColorForWidgetType(WidgetType.CALENDAR))
        assertEquals(WidgetColorUtils.COLOR_SINGLE_SELECT, WidgetColorUtils.getColorForWidgetType(WidgetType.SINGLE_SELECT))
        assertEquals(WidgetColorUtils.COLOR_MULTI_SELECT, WidgetColorUtils.getColorForWidgetType(WidgetType.MULTI_SELECT))
        assertEquals(WidgetColorUtils.COLOR_TEXT_BOX, WidgetColorUtils.getColorForWidgetType(WidgetType.TEXT_BOX))
        assertEquals(WidgetColorUtils.COLOR_QUANTITY, WidgetColorUtils.getColorForWidgetType(WidgetType.QUANTITY))
        assertEquals(WidgetColorUtils.COLOR_CUSTOMER, WidgetColorUtils.getColorForWidgetType(WidgetType.CUSTOMER))
    }

    @Test
    fun `quantity and customer colors are distinct from every other widget color`() {
        val otherColors = listOf(
            WidgetColorUtils.COLOR_CALENDAR,
            WidgetColorUtils.COLOR_SINGLE_SELECT,
            WidgetColorUtils.COLOR_MULTI_SELECT,
            WidgetColorUtils.COLOR_TEXT_BOX
        )

        otherColors.forEach { assertNotEquals(it, WidgetColorUtils.COLOR_QUANTITY) }
        otherColors.forEach { assertNotEquals(it, WidgetColorUtils.COLOR_CUSTOMER) }
        assertNotEquals(WidgetColorUtils.COLOR_QUANTITY, WidgetColorUtils.COLOR_CUSTOMER)
    }

    @Test
    fun `every widget type resolves to an icon`() {
        assertEquals(R.drawable.ic_calendar, WidgetColorUtils.getIconForWidgetType(WidgetType.CALENDAR))
        assertEquals(R.drawable.ic_check_box, WidgetColorUtils.getIconForWidgetType(WidgetType.SINGLE_SELECT))
        assertEquals(R.drawable.ic_label, WidgetColorUtils.getIconForWidgetType(WidgetType.MULTI_SELECT))
        assertEquals(R.drawable.ic_edit, WidgetColorUtils.getIconForWidgetType(WidgetType.TEXT_BOX))
        assertEquals(R.drawable.ic_add_circle, WidgetColorUtils.getIconForWidgetType(WidgetType.QUANTITY))
        assertEquals(R.drawable.ic_user_circle, WidgetColorUtils.getIconForWidgetType(WidgetType.CUSTOMER))
    }
}
