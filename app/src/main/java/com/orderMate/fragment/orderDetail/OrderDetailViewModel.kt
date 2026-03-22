package com.orderMate.fragment.orderDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orderMate.modals.ShareMessageJson
import com.orderMate.modals.ShareSmsModal
import com.orderMate.repository.CloverRepository
import com.orderMate.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.UnknownHostException

class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CloverRepository = CloverRepository.getInstance(application)


    private val _successResponse: MutableLiveData<Pair<String , Boolean>> = MutableLiveData()
    val successResponse: LiveData<Pair<String , Boolean>> = _successResponse



    fun shareEmail(data: ShareMessageJson) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.sendEmail(data)
                handleResponse(response , Constants.emailAddress)
            }
            catch (e : UnknownHostException){
                _successResponse.postValue(Pair(Constants.noInternet,false))
            }
            catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun handleResponse(response: Response<Any> , whichType : String) {
        _successResponse.postValue(Pair( whichType ,response.isSuccessful))
    }

    fun shareSms(data: ShareSmsModal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.sendSms(data)
                handleResponse(response , Constants.phoneNumber)
            }
            catch (e : UnknownHostException){
                _successResponse.postValue(Pair(Constants.noInternet,false))
            }
            catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}