package com.lucky.aikeyboard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val key = sharedPref.getString("api_key", null)
        if (key?.isNotEmpty() == true) return key
        // Else from local.properties
        return try {
            context.javaClass.classLoader?.getResourceAsStream("local.properties")?.bufferedReader()?.useLines { lines ->
                lines.find { it.startsWith("gemini.api.key=") }?.substringAfter("=") ?: throw Exception("API key not found")
            } ?: throw Exception("local.properties not found")
        } catch (e: Exception) {
            throw Exception("Gemini API key not configured")
        }
    }

    suspend fun generateReply(prompt: String, tone: String, context: String?): String {
        return withContext(Dispatchers.IO) {
            val apiKey = getApiKey()
            val systemPrompt = "You are a helpful AI assistant. Respond in a $tone tone. Keep responses concise."
            val fullPrompt = if (context != null) {
                "Context: $context\nUser: $prompt\nAssistant:"
            } else {
                prompt
            }
            val json = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n$fullPrompt")
                            })
                        })
                    })
                })
            }.toString()
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("API error: ${response.message}")
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }
}
