# OrderMate V2 Implementation Plan

## Overview
This document outlines the implementation plan for 7 parent tickets, each to be completed on a separate branch off `complete_v2_redesign_2`.

---

## Branch 1: Parent Ticket #7 - General/Global UI Refinements
**Branch Name:** `feature/7-global-ui-refinements`

### Sub-ticket #8: Make Clover Tags Human Readable
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Current State:**
- `CommonFunctions.kt` → `getThePaymentState()` returns raw API values like `LOCKED`, `PARTIALLY_PAID`

**Implementation:**
1. Create a mapping function in `CommonFunctions.kt`:
```kotlin
fun getHumanReadablePaymentState(apiValue: String): String {
    return when (apiValue.uppercase()) {
        "OPEN" -> "Open"
        "PAID" -> "Paid"
        "PARTIALLY_PAID" -> "Partially Paid"
        "PARTIALLY_REFUNDED" -> "Partially Refunded"
        "REFUNDED" -> "Refunded"
        "LOCKED" -> "Closed"
        else -> apiValue.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}
```
2. Update `getThePaymentState()` to return human-readable values
3. Update all adapters using payment state display:
   - `OrderCardRedesignAdapter.kt`
   - `OrderDetailFragment.kt`
   - `CalendarFragment.kt` (event preview)

**Files to Modify:**
- `app/src/main/java/com/orderMate/utils/CommonFunctions.kt`
- `app/src/main/java/com/orderMate/adapters/OrderCardRedesignAdapter.kt`

---

### Sub-ticket #9: Remove Gradient from Background, Keep Flat Color
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Current State:**
- `ProfileSettingsFragment.kt` → `applyGradientToAppBackground()` creates a `GradientDrawable` with `TL_BR` orientation
- Uses `lightenColor()` to create gradient effect

**Implementation:**
1. Modify `applyGradientToAppBackground()` to use solid color instead:
```kotlin
private fun applyFlatColorToAppBackground(hexColor: String) {
    val baseColor = Color.parseColor(hexColor)
    val colorDrawable = ColorDrawable(baseColor)
    
    activity?.let { act ->
        act.window?.decorView?.background = colorDrawable
        act.findViewById<View>(R.id.rootLayout)?.background = colorDrawable
    }
}
```
2. Rename method to `applyFlatColorToAppBackground()` or keep name but change behavior
3. Update `applyThemeColor()` similarly if it creates gradients
4. Keep color picker functionality intact

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/ProfileSettingsFragment.kt`

---

### Sub-ticket #10: Fix Toggle Switch Shape - Circle Not Oval
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Current State:**
- `res/drawable/bg_toggle_thumb.xml` defines 20x20dp oval shape
- Issue: Switch thumbs may appear oval due to scaling or container constraints

**Implementation:**
1. Verify `bg_toggle_thumb.xml` has equal width/height (currently 20x20dp ✓)
2. Check SwitchCompat widget's `thumbWidth` and `thumbHeight` attributes
3. Ensure parent container doesn't distort the thumb
4. If needed, add explicit width/height constraints in XML layouts:
   - `fragment_settings_redesign.xml`
   - `item_widget_editor.xml`
   - `fragment_custom_fields.xml`
5. Consider using `app:switchMinWidth` attribute

**Files to Modify:**
- `app/src/main/res/drawable/bg_toggle_thumb.xml` (verify)
- `app/src/main/res/values/style.xml` (add SwitchCompat style)
- Various layout XMLs using SwitchCompat

---

## Branch 2: Parent Ticket #11 - Main Header Refinements
**Branch Name:** `feature/11-header-refinements`

### Sub-ticket #12: Separate Order Date and Due Date Pills
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Update header layout to include two separate date pills
2. Each pill should be independently clickable and editable
3. Order Date: When order was created (read-only or editable)
4. Due Date: Expected delivery/pickup date (editable via date picker)

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_list_redesign.xml`
- `app/src/main/java/com/orderMate/fragment/OrderListRedesignFragment.kt`
- `app/src/main/res/layout/fragment_calendar_redesign.xml`
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`

---

### Sub-ticket #13: Update Calendar Modal Colors
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Left panel (month/year nav): Use user's theme color from `ProfileSettingsManager`
2. Right panel (date grid): Use filter modal highlight color (gold/orange `#FF9F43`)
3. Update `CustomDatePickerFragment.kt` to apply theme colors
4. Reference `FilterDialogFragment` for consistent accent color

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CustomDatePickerFragment.kt`
- `app/src/main/res/values/colors.xml` (if new colors needed)
- Date picker theme/style

---

### Sub-ticket #14: Expand Calendar Icon Click Area
**Type:** BUG FIX  
**Estimated Effort:** Small

**Implementation:**
1. Expand touch target padding on calendar icon (minimum 48dp touch target)
2. Add padding/margin to increase clickable area without changing icon size
3. Use `android:padding` or wrap in touchable container

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_list_redesign.xml` (calendarIcon)
- Add background with padding or increase touch target

---

### Sub-ticket #15: Add Sync Button with Syncing State
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Add sync button icon in header top-right area
2. On click: Show "Syncing..." indicator in header
3. Call Clover API to refresh orders
4. Hide indicator when sync completes
5. Verify `syncingContainer` exists and wire up behavior

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_list_redesign.xml`
- `app/src/main/java/com/orderMate/fragment/OrderListRedesignFragment.kt` (`syncOrders()`)

---

## Branch 3: Parent Ticket #16 - List Page UI Refinements
**Branch Name:** `feature/16-list-page-refinements`

### Sub-ticket #17: Fix Order Pill Colors - Darker BG, Lighter Text
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Update pill background colors to be darker/more opaque
2. Update text colors to be lighter for contrast
3. Modify `colors.xml` or pill styling in adapter

**Files to Modify:**
- `app/src/main/res/values/colors.xml`
- `app/src/main/java/com/orderMate/adapters/OrderCardRedesignAdapter.kt`
- `app/src/main/res/layout/item_order_card_redesign.xml`

---

### Sub-ticket #18: Add Order Description to List Row Cards
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Extract order description from order notes/Firebase data
2. Add TextView to card layout positioned as last element
3. Truncate with ellipsis if too long (single line, `maxLines="1"`, `ellipsize="end"`)
4. Update adapter to bind description data

**Files to Modify:**
- `app/src/main/res/layout/item_order_card_redesign.xml`
- `app/src/main/java/com/orderMate/adapters/OrderCardRedesignAdapter.kt`

---

### Sub-ticket #19: Add Custom Order Tags to List Row Cards
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Fetch custom tags from OrderMate order notes (Firebase)
2. Render alongside Clover system tags (payment status, order status)
3. Use consistent pill/badge styling
4. May need to limit number of tags shown to prevent overflow

**Files to Modify:**
- `app/src/main/res/layout/item_order_card_redesign.xml`
- `app/src/main/java/com/orderMate/adapters/OrderCardRedesignAdapter.kt`

---

## Branch 4: Parent Ticket #20 - Calendar Page Refinements & Bug Fixes
**Branch Name:** `feature/20-calendar-refinements`

### Sub-ticket #21: Auto-Scroll to First Event on Day Selection
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. In day selection handler, find first event for selected day
2. Scroll RecyclerView/ScrollView to position the event in view
3. No scroll if no events exist for that day

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`

---

### Sub-ticket #22: BUG - Event Disappears After Back Navigation
**Type:** BUG FIX  
**Estimated Effort:** Medium

**Implementation:**
1. Debug state restoration in `observeSharedState()`
2. Ensure day view state persists through navigation
3. May need to save/restore selected date and events in ViewModel
4. Check fragment lifecycle handling

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`
- `app/src/main/java/com/orderMate/viewmodel/SharedFilterViewModel.kt` (if needed)

---

### Sub-ticket #23: BUG - Calendar Arrows Should Increment Based on View
**Type:** BUG FIX  
**Estimated Effort:** Small

**Implementation:**
1. Check current view mode before incrementing:
   - Day view: +/- 1 day
   - Week view: +/- 1 week
   - Month view: +/- 1 month
2. Update `navigateMonth()` to respect `currentViewMode`

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`

---

### Sub-ticket #24: Darken Calendar Event Background Color
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Increase alpha/opacity of event background colors in `colors.xml`:
   - `event_pickup_bg`: `#334CAF50` → `#664CAF50` (or similar)
   - `event_delivery_bg`: `#332196F3` → `#662196F3`
   - `event_preorder_bg`: `#339C27B0` → `#669C27B0`

**Files to Modify:**
- `app/src/main/res/values/colors.xml`

---

### Sub-ticket #25: Add Grid Lines to Calendar View
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add border/divider lines to calendar day cells
2. Can use `android:divider` on GridLayout or add stroke to cell background
3. Update `item_calendar_day.xml` with border drawable

**Files to Modify:**
- `app/src/main/res/layout/fragment_calendar_redesign.xml`
- `app/src/main/res/layout/item_calendar_day.xml`
- Create `bg_calendar_cell_border.xml` if needed

---

### Sub-ticket #26: Multi-Event Day Click Opens Day View
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. In `onDayClick()`, count events for clicked day
2. If > 1 event: Switch to day view for that day
3. If == 1 event: Proceed to event preview (see #27)

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`

---

### Sub-ticket #27: Single Event Click Opens Event Preview Modal
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. In day view: Any event click → event preview modal
2. In week/month view with 1 event: Click → event preview modal
3. Show `EventPreviewDialog` with order data

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`
- `app/src/main/java/com/orderMate/fragment/EventPreviewDialog.kt`

---

### Sub-ticket #28: Event Preview Full Details Navigation
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Wire up "Full Details" button click in `EventPreviewDialog.kt`
2. Navigate to `OrderDetailFragment` with order ID
3. Dismiss dialog before navigation

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/EventPreviewDialog.kt`

---

### Sub-ticket #29: Event Preview Due Date Fallback Logic
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Priority: Order due date → Earliest item due date → "-"
2. Update `convertOrdersToEvents()` or event preview data binding
3. Handle null/missing dates gracefully

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/CalendarFragment.kt`
- `app/src/main/java/com/orderMate/fragment/EventPreviewDialog.kt`

---

### Sub-ticket #30: Render Custom Order Tags in Event Preview
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Replace hardcoded "pickup" text with actual custom tags from order notes
2. Fetch tags from Firebase/order notes
3. Render as pills under order number in event preview header

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/EventPreviewDialog.kt`
- `app/src/main/res/layout/dialog_event_preview.xml` (if needed)

---

## Branch 5: Parent Ticket #31 - Settings Page Refinements
**Branch Name:** `feature/31-settings-refinements`

### Sub-ticket #32: Verify Register Button Toggle (5A: General Tab)
**Type:** VERIFY EXISTING  
**Estimated Effort:** Small

**Implementation:**
1. Test toggle functionality manually
2. Verify Firebase persistence
3. Document findings or fix if broken

**Files to Verify:**
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt` (`setupGeneralPanel()`)

---

### Sub-ticket #33: Order Notes Edit Widget (5B: Pop Up Tab)
**Type:** VERIFY EXISTING  
**Estimated Effort:** Small

**Implementation:**
1. Verify widget editor with drag-and-drop
2. Test Firebase persistence of widget order
3. Document or fix issues

**Files to Verify:**
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt` (`setupPopUpPanel()`)

---

### Sub-ticket #34: Add Toggles for Order Notes and Item Notes (5B: Pop Up Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Add two toggle switches in Pop Up panel:
   - Enable Order Notes
   - Enable Item Notes
2. Persist to Firebase via `SettingsManager`
3. Reference toggles when showing popups in register overlay

**Files to Modify:**
- `app/src/main/res/layout/fragment_settings_redesign.xml`
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt`
- `app/src/main/java/com/orderMate/utils/SettingsManager.kt`

---

### Sub-ticket #35: Verify DB Handles Item Notes (5B: Pop Up Tab)
**Type:** VERIFY EXISTING  
**Estimated Effort:** Small

**Implementation:**
1. Verify Firebase schema supports both order-level and item-level notes
2. Check `WidgetManager` and `FirebaseConfigManager` implementation
3. Document schema or fix if needed

**Files to Verify:**
- `app/src/main/java/com/orderMate/utils/WidgetManager.kt`
- `app/src/main/java/com/orderMate/utils/FirebaseConfigManager.kt`
- `app/src/main/java/com/orderMate/modals/PopupSettings.kt`

---

### Sub-ticket #36: Add Widget Popup Styling (5B: Pop Up Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Apply filter modal styling to "Add Widget" popup:
   - Glassmorphism background
   - Consistent colors and spacing
2. Reference `FilterDialogFragment` for style

**Files to Modify:**
- Widget add dialog layout XML
- Dialog styling

---

### Sub-ticket #37: Verify Notification Templates Management (5C: Notification Tab)
**Type:** VERIFY EXISTING  
**Estimated Effort:** Small

**Implementation:**
1. Test CRUD operations for notification templates
2. Verify Firebase persistence
3. Document or fix issues

**Files to Verify:**
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt` (`setupNotificationPanel()`)

---

### Sub-ticket #38: Add Filter Modal Config Section (5D: Advanced Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Large

**Implementation:**
1. Add new section in Advanced tab for filter customization
2. Add toggles to enable/disable each filter section
3. Add drag-and-drop reordering
4. Persist to Firebase
5. Apply changes to filter modal immediately

**Files to Modify:**
- `app/src/main/res/layout/fragment_settings_redesign.xml`
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt` (`setupAdvancedPanel()`)
- `app/src/main/java/com/orderMate/utils/SettingsManager.kt`
- `app/src/main/java/com/orderMate/fragment/FilterDialogFragment.kt`

---

### Sub-ticket #39: Make Filter Pills Human Readable (5D: Advanced Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Reuse human-readable mapping from #8
2. Apply to filter pills in both List tab and Calendar tab
3. Consistent capitalization

**Files to Modify:**
- Filter/pill display logic (reuse #8 function)

---

### Sub-ticket #40: Validate Minutes Input (0-60) (5D: Advanced Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add input validation to minutes EditText fields
2. Restrict to 0-60 range
3. Show error or auto-correct invalid values

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt`

---

### Sub-ticket #41: Validate Hours Input (0-24) (5D: Advanced Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add input validation to hours EditText fields
2. Restrict to 0-24 range
3. Show error or auto-correct invalid values

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/SettingsFragment.kt`

---

### Sub-ticket #42: Implement Order Notes Trigger in Register (5D: Advanced Tab)
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. In register overlay: OrderMate button renders over Clover register
2. Click button → drawer opens
3. Add order notes section in drawer header
4. Click order notes → opens order notes popup (not item notes)
5. Order notes and item notes popups styled identically

**Files to Modify:**
- `app/src/main/java/com/orderMate/activities/OverlayActivity.kt`
- Related overlay layouts

---

## Branch 6: Parent Ticket #43 - Order Details Page Refinements
**Branch Name:** `feature/43-order-details-refinements`

### 6A: Header

#### Sub-ticket #44: BUG - Fix Delete Button Crash
**Type:** BUG FIX  
**Estimated Effort:** Small

**Implementation:**
1. Fix navigation after order deletion
2. Navigate back to order list instead of crashing/closing app
3. Check fragment stack and use proper navigation

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`

---

### 6B: Order Details Card

#### Sub-ticket #45: Fix Duplicate Labels
**Type:** BUG FIX  
**Estimated Effort:** Small

**Implementation:**
1. Remove duplicate status labels (e.g., "PAID" showing twice)
2. Check layout for duplicate views

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`

---

#### Sub-ticket #46: Add Order Notes Data to Details Card
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Add description row from order notes
2. Add custom order tags inline with existing pills
3. Fetch from Firebase

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`

---

#### Sub-ticket #47: Customer Button Padding and Border Radius
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add horizontal padding to customer button
2. Add border radius for rounded corners

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`

---

#### Sub-ticket #48: Customer Data Integration with Clover
**Type:** REFINEMENT  
**Estimated Effort:** Large

**Implementation:**
1. Implement bi-directional sync with Clover Customer API
2. Fetch existing customer data
3. Update customer data back to Clover

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`
- `app/src/main/java/com/orderMate/repository/CloverRepository.kt`

---

#### Sub-ticket #49: Order Tags Color Styling
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add appropriate colors for custom order tags
2. Update `colors.xml` with tag-specific colors

**Files to Modify:**
- `app/src/main/res/values/colors.xml`
- Tag rendering code

---

#### Sub-ticket #50: Customer Button Darker Background
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Match customer button background to item card background

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`

---

#### Sub-ticket #51: Reduce Card Header Spacing
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Reduce spacing between card header and first row
2. Match item card header spacing

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`

---

#### Sub-ticket #52: Customer Button Shows Truncated Name
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Display truncated 10 chars of full name (first + last)
2. Instead of just initials

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`

---

#### Sub-ticket #53: Dynamic Customer Name in Avatar Circle
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Replace hardcoded "ED" with actual customer name
2. Make circle pill-shaped to accommodate name
3. Show "?" if no customer on order

**Files to Modify:**
- `app/src/main/res/layout/dialog_customer.xml`
- `app/src/main/java/com/orderMate/fragment/orderDetail/CustomerDialog.kt`

---

### 6C: Order History Card

#### Sub-ticket #54: Add Sent Notifications to Order History
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Include sent notifications in history card
2. Fetch from notification history storage

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderDetailFragment.kt`

---

#### Sub-ticket #55: Create Reusable History Icons
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Create icons: `ic_history_sent`, `ic_history_read`, `ic_history_created`, `ic_history_modified`, `ic_history_payment`, `ic_history_refund`
2. Consistent style (outline, 24dp)
3. Store in `res/drawable/`

**Files to Create:**
- `app/src/main/res/drawable/ic_history_*.xml`

---

#### Sub-ticket #56: Reduce History Card Header Spacing
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Match item card header spacing

**Files to Modify:**
- Layout files

---

#### Sub-ticket #57: View All Button Darker Background
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Match item card background color

**Files to Modify:**
- `app/src/main/res/layout/fragment_order_detail.xml`

---

#### Sub-ticket #58: View All Popup Shows Full Messages
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Display complete notification messages in "View All" popup
2. Format for readability

**Files to Modify:**
- `app/src/main/res/layout/dialog_order_history.xml`
- `app/src/main/java/com/orderMate/fragment/orderDetail/OrderHistoryDialog.kt`

---

### 6D: Item Card

#### Sub-ticket #59: Dynamic Item Icons
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Remove hardcoded icons
2. Pull icons dynamically (from item data or category)

**Files to Modify:**
- `app/src/main/java/com/orderMate/adapters/ItemAdapter.kt`

---

#### Sub-ticket #60: Item Row Opens OrderMate Popup
**Type:** VERIFY EXISTING  
**Estimated Effort:** Small

**Implementation:**
1. Verify clicking item row opens `CustomModalDialog`
2. Test and document

**Files to Verify:**
- `onOrderItemClick()` implementation

---

#### Sub-ticket #61: Scrollable Item List with Indicator
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Add scroll capability to item list container
2. Add carrot/chevron indicator at bottom when more content available

**Files to Modify:**
- Item list layout
- Add scroll indicator drawable

---

### 6E: Send Notification Popup

#### Sub-ticket #62: Pull Templates from Settings Storage
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Load notification templates from Firebase (settings storage)
2. Display in template selector

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/SendNotificationDialog.kt`

---

#### Sub-ticket #63: Template Selection Auto-Fills Form
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. On template select: auto-fill text, subject, customer info

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/SendNotificationDialog.kt`

---

#### Sub-ticket #64: Use Template Subject for Email
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Use template's subject line field (not template name) as email subject
2. Add subject line field to template model if needed

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/SendNotificationDialog.kt`
- Template model

---

#### Sub-ticket #65: Sent Notifications in Order History
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Track all sent notifications
2. Display in order history section
3. Store in Firebase with order

**Files to Modify:**
- Notification sending logic
- History display

---

### 6F: Customer Details Popup

#### Sub-ticket #66: Auto-Fill Customer Data if Present
**Type:** REFINEMENT  
**Estimated Effort:** Small

**Implementation:**
1. Pre-populate input fields with existing customer data

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/CustomerDialog.kt`

---

#### Sub-ticket #67: Save New Customer to Clover
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Create new customer in Clover via API
2. Link to order

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/CustomerDialog.kt`
- Clover API integration

---

#### Sub-ticket #68: Customer Search from Clover
**Type:** REFINEMENT  
**Estimated Effort:** Medium

**Implementation:**
1. Query Clover customers API
2. Display search results
3. Allow selection

**Files to Modify:**
- `app/src/main/java/com/orderMate/fragment/orderDetail/CustomerSearchDialog.kt`

---

## Branch 7: Parent Ticket #69 - Stretch Goals / Future Enhancements
**Branch Name:** `feature/69-stretch-goals`

### Sub-ticket #70: Theme Selector with Preset Themes
**Type:** NEW FEATURE  
**Estimated Effort:** Medium

**Implementation:**
1. Add 4 preset themes including black & white
2. Create theme selector dialog
3. Apply selected theme to app

---

### Sub-ticket #71: Document Auto-Refresh Frequency
**Type:** DOCUMENTATION  
**Estimated Effort:** Small

**Implementation:**
1. Document how often orders refresh automatically
2. Add to README or in-app help

---

### Sub-ticket #72: Add Help Button to Header
**Type:** NEW FEATURE  
**Estimated Effort:** Small

**Implementation:**
1. Add help icon to header
2. Link to help content/documentation

---

### Sub-ticket #73: Calendar Widget Date/Time Storage
**Type:** NEW FEATURE  
**Estimated Effort:** Medium

**Implementation:**
1. Allow merchants to store preferred date/time for calendar widget
2. Persist in Firebase

---

### Sub-ticket #74: Default Template Setting
**Type:** NEW FEATURE  
**Estimated Effort:** Small

**Implementation:**
1. Add ability to set a default notification template
2. Auto-select when sending notifications

---

## Summary

| Branch | Parent Ticket | Sub-tickets | Estimated Effort |
|--------|---------------|-------------|------------------|
| `feature/7-global-ui-refinements` | #7 | #8, #9, #10 | Small |
| `feature/11-header-refinements` | #11 | #12, #13, #14, #15 | Medium |
| `feature/16-list-page-refinements` | #16 | #17, #18, #19 | Medium |
| `feature/20-calendar-refinements` | #20 | #21-#30 (10 tickets) | Medium-Large |
| `feature/31-settings-refinements` | #31 | #32-#42 (11 tickets) | Large |
| `feature/43-order-details-refinements` | #43 | #44-#68 (25 tickets) | Large |
| `feature/69-stretch-goals` | #69 | #70-#74 (5 tickets) | Medium |

**Total Sub-tickets:** 57

---

## Execution Order Recommendation

1. **Branch 1 (#7)**: Global UI changes that affect entire app
2. **Branch 2 (#11)**: Header changes needed before page-specific work
3. **Branch 3 (#16)**: List page refinements
4. **Branch 4 (#20)**: Calendar page refinements
5. **Branch 5 (#31)**: Settings refinements
6. **Branch 6 (#43)**: Order details refinements (largest scope)
7. **Branch 7 (#69)**: Stretch goals (lower priority)

Each branch should be merged to `complete_v2_redesign_2` after completion and testing before starting the next branch.
