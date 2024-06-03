package com.order.orderappclover.fragment.orderHistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.order.orderappclover.modals.ErrorResponse
import com.order.orderappclover.modals.orderHistory.AccessTokenRequest
import com.order.orderappclover.modals.orderHistory.AccessTokenResponse
import com.order.orderappclover.modals.orderHistory.GetAllEmployeeListResponse
import com.order.orderappclover.modals.orderHistory.GetAllEmployeeRequest
import com.order.orderappclover.modals.orderHistory.GetAllOrderRequest
import com.order.orderappclover.modals.orderHistory.GetOrderResponse
import com.order.orderappclover.repository.CloverRepository
import com.order.orderappclover.utils.OrderApiTypes
import com.order.orderappclover.utils.makePair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderHistoryViewModel(application: Application) : AndroidViewModel(application) {


    companion object {
        private val repository: CloverRepository = CloverRepository.getInstance()
    }


    private val accessTokenResponse: MutableLiveData<AccessTokenResponse> = MutableLiveData()
    private val errorResponse: MutableLiveData<ErrorResponse> = MutableLiveData()
    private val getAllOrderResponse: MutableLiveData<GetOrderResponse> = MutableLiveData()
    private val getAllEmployeeList: MutableLiveData<GetAllEmployeeListResponse> = MutableLiveData()

    /*
    * this is the connector live data which connect all the mutable live data used here to the
    * one observer livedata which will update the ui.
    * */
    private val connectorLiveData: MediatorLiveData<Pair<OrderApiTypes, Any>> =
        MediatorLiveData()

    init {
        connectorLiveData.addSource(accessTokenResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.AccessTokenResponse))
        }
        connectorLiveData.addSource(errorResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.ErrorResponse))
        }
        connectorLiveData.addSource(getAllOrderResponse) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetAllOrders))
        }
        connectorLiveData.addSource(getAllEmployeeList) {
            connectorLiveData.postValue(makePair(it, OrderApiTypes.GetAllEmployeeResponse))
        }
    }

    // this is the observer live data which observe the data.
    val observerLiveData: LiveData<Pair<OrderApiTypes, Any>> get() = connectorLiveData


    fun getAccessToken(data: AccessTokenRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getAccessToken(data)
                if (response.isSuccessful) {
                    accessTokenResponse.postValue(response.body())
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

    fun getAllOrdersWithPagination(data: GetAllOrderRequest) {
        viewModelScope.launch {
            try {
                val response = repository.getAllOrdersWithPagination(data)
                if (response.isSuccessful) {
                    getAllOrderResponse.postValue(response.body())
                } else {
                    if(response.code() == 401)

                    errorResponse.postValue(
                        ErrorResponse(response.message()),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllEmployees(data: GetAllEmployeeRequest) {
        viewModelScope.launch {
            try {
                val response = repository.getEmployeeList(data)
                if (response.isSuccessful) {
                    getAllEmployeeList.postValue(response.body())
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