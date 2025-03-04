package com.orderMate.fragment.customFields

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.orderMate.R

public class CustomFieldsFragmentDirections private constructor() {
  public companion object {
    public fun actionCustomFieldsFragmentToOrderHistoryFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_customFieldsFragment_to_orderHistoryFragment)
  }
}
