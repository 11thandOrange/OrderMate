package com.orderMate.fragment.orderHistory

import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.clover.sdk.v3.order.Order
import com.orderMate.R
import java.io.Serializable
import java.lang.UnsupportedOperationException
import kotlin.Int
import kotlin.Suppress

public class OrderHistoryFragmentDirections private constructor() {
  private data class ActionOrderHistoryFragmentToOrderDetailFragment(
    public val orderData: Order,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_orderHistoryFragment_to_orderDetailFragment

    public override val arguments: Bundle
      @Suppress("CAST_NEVER_SUCCEEDS")
      get() {
        val result = Bundle()
        if (Parcelable::class.java.isAssignableFrom(Order::class.java)) {
          result.putParcelable("orderData", this.orderData as Parcelable)
        } else if (Serializable::class.java.isAssignableFrom(Order::class.java)) {
          result.putSerializable("orderData", this.orderData as Serializable)
        } else {
          throw UnsupportedOperationException(Order::class.java.name +
              " must implement Parcelable or Serializable or must be an Enum.")
        }
        return result
      }
  }

  public companion object {
    public fun actionOrderHistoryFragmentToCustomFieldsFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_orderHistoryFragment_to_customFieldsFragment)

    public fun actionOrderHistoryFragmentToOrderDetailFragment(orderData: Order): NavDirections =
        ActionOrderHistoryFragmentToOrderDetailFragment(orderData)
  }
}
