package com.orderMate.modals

import com.orderMate.utils.ModalDialogCategories

/**
 * @deprecated Use [WidgetConfig] instead.
 * V1 schema model - being replaced by V2 WidgetConfig/PopupSettings schema.
 */
@Deprecated("Use WidgetConfig instead", ReplaceWith("WidgetConfig"))
data class CustomItemJson(
    var types: MutableList<ModalData>
)

/**
 * @deprecated Use [WidgetConfig] and [WidgetOption] instead.
 * V1 schema model for individual modal fields.
 */
@Deprecated("Use WidgetConfig and WidgetOption instead")
data class ModalData(
    var name: String,
    var type : ModalDialogCategories,
    var hasDropDown : Boolean = true,
    var isActive: Boolean,
    var list: MutableList<String> = mutableListOf()
)

/**
 * @deprecated Not used. See [CustomItemJson].
 */
@Deprecated("Not used")
data class CustomItemJson1(
    var types: HashMap<Int , MutableList<ModalData>>
)

