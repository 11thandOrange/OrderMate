package com.orderMate.networkManager

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Clover REST API (v3) client for order history beyond what's cached locally on-device (#138).
 *
 * Clover's on-device OrderConnector (used everywhere else in this app via
 * MyApp.getAllOrders()) only serves a retention-windowed local cache, not the merchant's full
 * order history - see MyApp.getAllOrders()'s doc for details. This REST client supplements
 * that with Clover's cloud Orders API (https://docs.clover.com/dev/docs/orders-faqs), which
 * supports pagination independent of the local device cache.
 *
 * The response is returned as a raw [ResponseBody] rather than deserialized into
 * com.clover.sdk.v3.order.Order directly: Order is a JSONObject-backed SDK model (constructed
 * via `Order(JSONObject)`), not a Gson-reflectable POJO, so Retrofit's Gson converter can't
 * populate it correctly. Callers (see CloverRepository.loadOlderOrders) parse the JSON
 * manually and construct Order instances the same way the SDK itself does.
 */
interface CloverOrdersApi {
    @GET("v3/merchants/{merchantId}/orders")
    suspend fun getOrders(
        @Path("merchantId") merchantId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Header("Authorization") authorization: String
    ): Response<ResponseBody>
}

/**
 * Builds a [CloverOrdersApi] client for a given base URL.
 *
 * Unlike [RetrofitInstanceWithAuth] (which targets a single fixed third-party host with a
 * static key), Clover's REST base URL and bearer token both come from
 * `com.clover.sdk.util.CloverAuth.authenticate()` per call - the base URL can differ by
 * merchant environment (production/sandbox/region) and the token can be refreshed, so a new
 * client is built per call rather than cached as a singleton.
 */
object CloverOrdersApiClient {

    fun create(baseUrl: String): CloverOrdersApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .build()
            .create(CloverOrdersApi::class.java)
    }
}
