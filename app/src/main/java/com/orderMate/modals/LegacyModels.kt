package com.orderMate.modals

import com.orderMate.utils.ModalDialogCategories

/**
 * Legacy v1 models for migration
 * These map to the old customData/{merchantId}/data structure
 */
data class LegacyCustomItemJson(
    var types: MutableList<LegacyModalData> = mutableListOf()
)

data class LegacyModalData(
    var name: String = "",
    var type: ModalDialogCategories = ModalDialogCategories.Description,
    var hasDropDown: Boolean = true,
    var isActive: Boolean = false,
    var list: MutableList<String> = mutableListOf()
)
