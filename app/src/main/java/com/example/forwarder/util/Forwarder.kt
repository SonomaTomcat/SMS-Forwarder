package com.example.forwarder.util

import android.util.Log
import com.example.forwarder.domain.model.Api
import com.example.forwarder.domain.model.Sms
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import androidx.preference.PreferenceManager



object Forwarder {
    private val client = OkHttpClient()

    /**
     * Forward SMS to the specified API.
     * @param sms SMS object
     * @param api API configuration
     * @param context Android context
     * @return true if forwarding is successful
     */
    fun forwardSms(sms: Sms, api: Api, context: android.content.Context): Boolean {
        try {
            val url = buildUrl(api, sms, context)
            val json = buildJson(api, sms)
            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            Log.e("Forwarder", "Forward failed", e)
            return false
        }
    }

    // Build URL and replace query parameters
    private fun buildUrl(api: Api, sms: Sms, context: android.content.Context): String {
        val urlConfig = api.url
        val baseUrl = urlConfig.baseUrl
        val queryString = buildQueryString(api, sms, context)
        return if (!queryString.isNullOrBlank()) "$baseUrl?$queryString" else baseUrl
    }

    // Build query string, replacing placeholders like {sender}, {content}, etc.
    private fun buildQueryString(api: Api, sms: Sms, context: android.content.Context): String? {
        val queries = api.url.queries ?: return null
        return queries.joinToString("&") { param ->
            val key = param.key ?: ""
            val value = replacePlaceholders(param.value ?: "", sms, context)
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
    }

    // Build POST body, replacing placeholders in all values
    private fun buildJson(api: Api, sms: Sms): String {
        val json = JSONObject()
        // Put all fields from Sms as JSON, replacing placeholders if needed
        json.put("id", sms.id)
        json.put("sender", sms.sender)
        json.put("content", sms.content)
        json.put("timestamp", sms.timestamp)
        return json.toString()
    }

    // Replace placeholders like {sender}, {content}, {id}, {timestamp}
    private fun replacePlaceholders(text: String, sms: Sms, context: android.content.Context): String {
        return text
            .replace("{id}", sms.id.toString())
            .replace("{sender}", sms.sender ?: "")
            .replace("{content}", sms.content ?: "")
            .replace("{timestamp}", sms.timestamp.toString())
            .replace("{time}", DateFormatter.getFormattedTime(
                sms.timestamp,
                getUserTimeFormat(context)
            ))
    }

    private fun getUserTimeFormat(context: android.content.Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("time_format", "yyyy-MM-dd HH:mm:ss z") ?: "yyyy-MM-dd HH:mm:ss z"
    }
}
