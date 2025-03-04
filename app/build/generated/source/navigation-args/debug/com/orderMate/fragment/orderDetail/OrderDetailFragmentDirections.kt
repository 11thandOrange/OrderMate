package com.orderMate.fragment.orderDetail

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

public class OrderDetailFragmentDirections private constructor() {
  private data class ActionOrderDetailFragmentSelf(
    public val orderData: Order,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_orderDetailFragment_self

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
    public fun actionOrderDetailFragmentToOrderHistoryFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_orderDetailFragment_to_orderHistoryFragment)

    public fun actionOrderDetailFragmentSelf(orderData: Order): NavDirections =
        ActionOrderDetailFragmentSelf(orderData)
  }
}
