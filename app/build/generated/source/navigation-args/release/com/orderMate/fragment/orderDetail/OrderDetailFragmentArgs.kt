package com.orderMate.fragment.orderDetail

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import com.clover.sdk.v3.order.Order
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import kotlin.Suppress
import kotlin.jvm.JvmStatic

public data class OrderDetailFragmentArgs(
  public val orderData: Order,
) : NavArgs {
  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toBundle(): Bundle {
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

  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    if (Parcelable::class.java.isAssignableFrom(Order::class.java)) {
      result.set("orderData", this.orderData as Parcelable)
    } else if (Serializable::class.java.isAssignableFrom(Order::class.java)) {
      result.set("orderData", this.orderData as Serializable)
    } else {
      throw UnsupportedOperationException(Order::class.java.name +
          " must implement Parcelable or Serializable or must be an Enum.")
    }
    return result
  }

  public companion object {
    @JvmStatic
    @Suppress("DEPRECATION")
    public fun fromBundle(bundle: Bundle): OrderDetailFragmentArgs {
      bundle.setClassLoader(OrderDetailFragmentArgs::class.java.classLoader)
      val __orderData : Order?
      if (bundle.containsKey("orderData")) {
        if (Parcelable::class.java.isAssignableFrom(Order::class.java) ||
            Serializable::class.java.isAssignableFrom(Order::class.java)) {
          __orderData = bundle.get("orderData") as Order?
        } else {
          throw UnsupportedOperationException(Order::class.java.name +
              " must implement Parcelable or Serializable or must be an Enum.")
        }
        if (__orderData == null) {
          throw IllegalArgumentException("Argument \"orderData\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"orderData\" is missing and does not have an android:defaultValue")
      }
      return OrderDetailFragmentArgs(__orderData)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): OrderDetailFragmentArgs {
      val __orderData : Order?
      if (savedStateHandle.contains("orderData")) {
        if (Parcelable::class.java.isAssignableFrom(Order::class.java) ||
            Serializable::class.java.isAssignableFrom(Order::class.java)) {
          __orderData = savedStateHandle.get<Order?>("orderData")
        } else {
          throw UnsupportedOperationException(Order::class.java.name +
              " must implement Parcelable or Serializable or must be an Enum.")
        }
        if (__orderData == null) {
          throw IllegalArgumentException("Argument \"orderData\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"orderData\" is missing and does not have an android:defaultValue")
      }
      return OrderDetailFragmentArgs(__orderData)
    }
  }
}
