package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RelayMessageRequest(
    val phoneNumber: String,
    val messageBody: String,
    val templateType: String,
    val phoneNumberId: String? = null,
    val senderId: String? = null,
    val idempotencyKey: String
)

@JsonClass(generateAdapter = true)
data class RelayMessageResponse(
    val success: Boolean,
    val providerMessageId: String? = null,
    val message: String? = null
)

interface MessagingRelayService {
    @POST("api/v1/messaging/whatsapp/send")
    suspend fun sendWhatsAppCloudMessage(
        @Header("Authorization") bearerToken: String,
        @Body request: RelayMessageRequest
    ): Response<RelayMessageResponse>

    @POST("api/v1/messaging/sms/send")
    suspend fun sendSmsGatewayMessage(
        @Header("Authorization") bearerToken: String,
        @Body request: RelayMessageRequest
    ): Response<RelayMessageResponse>
}

object MessagingRelayClient {
    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    fun create(baseUrl: String): MessagingRelayService? {
        val normalized = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://10.") && !normalized.startsWith("http://127.")) {
            return null
        }
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MessagingRelayService::class.java)
    }
}
