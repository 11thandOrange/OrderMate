package com.orderMate.modals

import com.orderMate.utils.ModalDialogCategories

data class CustomItemJson(
    var types: MutableList<ModalData>
)

data class ModalData(
    var name: String,
    var type : ModalDialogCategories,
    var hasDropDown : Boolean = true,
    var isActive: Boolean,
    var list: MutableList<String> = mutableListOf()
)
data class CustomItemJson1(
    var types: HashMap<Int , MutableList<ModalData>>
)

