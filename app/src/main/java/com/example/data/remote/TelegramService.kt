package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class TelegramSendMessageRequest(
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "text") val text: String,
    @Json(name = "parse_mode") val parseMode: String = "HTML"
)

data class TelegramResponse(
    val ok: Boolean,
    val description: String? = null
)

class TelegramService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(TelegramSendMessageRequest::class.java)
    private val responseAdapter = moshi.adapter(TelegramResponse::class.java)

    suspend fun sendMessage(botToken: String, chatId: String, text: String, parseMode: String = "HTML"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanToken = botToken.trim()
                val cleanChatId = chatId.trim()
                if (cleanToken.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Bot Token ထည့်သွင်းထားခြင်း မရှိပါ"))
                }
                if (cleanChatId.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Chat ID / Channel ID ထည့်သွင်းထားခြင်း မရှိပါ"))
                }

                val url = "https://api.telegram.org/bot$cleanToken/sendMessage"
                val bodyData = TelegramSendMessageRequest(
                    chatId = cleanChatId,
                    text = text,
                    parseMode = parseMode
                )
                val jsonBody = jsonAdapter.toJson(bodyData)
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    Result.success("Telegram သို့ စာရင်း အောင်မြင်စွာ ပို့ပြီးပါပြီ ✅")
                } else {
                    val parsed = runCatching { responseAdapter.fromJson(responseBodyStr) }.getOrNull()
                    val errorDetail = parsed?.description ?: "Code ${response.code}: $responseBodyStr"
                    Result.failure(Exception("Telegram Error: $errorDetail"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
