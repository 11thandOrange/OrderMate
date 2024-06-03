package com.order.orderappclover.networkManager


import com.order.orderappclover.utils.Constants
import com.order.orderappclover.utils.MyApp
import com.order.orderappclover.utils.PreferenceManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = "${Constants.bearer} ${getTokenFromStorage()}"
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader(Constants.authorization, token)
            .build()
        return chain.proceed(newRequest)
    }

    private fun getTokenFromStorage(): String {
        val sharedPreferences =
            PreferenceManager.getInstance(MyApp.getContext())
        return sharedPreferences.getString(Constants.accessToken)
    }
}


object RetrofitInstanceWithAuth {

    private var interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val authInterceptor = AuthInterceptor()
    private fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.TIME_OUT_DURATION, TimeUnit.MINUTES)
            .writeTimeout(Constants.TIME_OUT_DURATION, TimeUnit.MINUTES)
            .readTimeout(Constants.TIME_OUT_DURATION, TimeUnit.MINUTES)
            .addInterceptor(authInterceptor)
            .addInterceptor(interceptor)
            .build()
    }


    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(getClient())
        .build()

    fun getApiService(): ApiCall {
        return retrofit.create(ApiCall::class.java)
    }
}
