package com.specialOrderNew.fragment.orderDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.specialOrderNew.modals.ErrorResponse
import com.specialOrderNew.modals.orderDetail.GetOrderLineItems
import com.specialOrderNew.modals.orderDetail.OrderDetailRequest
import com.specialOrderNew.modals.orderDetail.OrderDetailResponse
import com.specialOrderNew.modals.orderDetail.OrderDiscountRequest
import com.specialOrderNew.modals.orderDetail.OrderDiscountResponse
import com.specialOrderNew.modals.orderDetail.OrderPaymentRequest
import com.specialOrderNew.modals.orderDetail.OrderPaymentResponse
import com.specialOrderNew.modals.orderDetail.OrderRefundResponse
import com.specialOrderNew.repository.CloverRepository
import com.specialOrderNew.utils.OrderApiTypes
import com.specialOrderNew.utils.makePair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val repository: CloverRepository = CloverRepository.getInstance()
    }

    private val getOrderDetailResponse: MutableLiveData<OrderDetailResponse> = MutableLiveData()
    private val getOrderDiscountResponse: MutableLiveData<OrderDiscountResponse> = MutableLiveData()
    private val getOrderTransactionStatus: MutableLiveData<OrderPaymentResponse> = MutableLiveData()
    private val getOrderTransactionStatusForRefund: MutableLiveData<OrderRefundResponse> = MutableLiveData()
    private val getOrderLineItems: MutableLiveData<GetOrderLineItems> = MutableLiveData()
    private val errorResponse: MutableLiveData<ErrorResponse> = MutableLiveData()

    /*
    * this is the connector live data which connect all the mutable live data used here to the
    * one observer livedata which will update the ui.
    * */
    private val connectorLiveData: MediatorLiveData<Pair<OrderApiTypes, Any>> =
        MediatorLiveData()

    init {
        connectorLiveData.addSource(getOrderDetailResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetOrderDetailResponse))
        }
        connectorLiveData.addSource(errorResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.ErrorResponse))
        }

        connectorLiveData.addSource(getOrderDiscountResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetOrderDiscountResponse))
        }

        connectorLiveData.addSource(getOrderTransactionStatus) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetOrderPaymentStatus))
        }

        connectorLiveData.addSource(getOrderLineItems) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetLineItemOfAnOrder))
        }

        connectorLiveData.addSource(getOrderTransactionStatusForRefund) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetOrderRefund))
        }
    }

    // this is the observer live data which observe the data.
    val observerLiveData: LiveData<Pair<OrderApiTypes, Any>> get() = connectorLiveData

    fun getOrderDetailsWithLineItemFilter(data: OrderDetailRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getOrderDetails(data)
                if (response.isSuccessful) {
                    getOrderDetailResponse.postValue(response.body())
                } else {
                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getOrderDiscountData(data: OrderDiscountRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getOrderDiscount(data)
                if (response.isSuccessful) {
                    getOrderDiscountResponse.postValue(response.body())
                } else {
                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getOrderPaymentStatus(data: OrderPaymentRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getOrderPayment(data)
                if (response.isSuccessful) {
                        getOrderTransactionStatus.postValue(response.body())
                } else {
                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun getOrderPaymentStatusRefund(data: OrderPaymentRequest,) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getOrderPaymentRefund(data)
                if (response.isSuccessful) {
                        getOrderTransactionStatusForRefund.postValue(response.body())
                } else {
                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    fun getAllLineItems(data: OrderPaymentRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getAllLineItems(data)
                if (response.isSuccessful) {
                    getOrderLineItems.postValue(response.body())
                } else {
                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}