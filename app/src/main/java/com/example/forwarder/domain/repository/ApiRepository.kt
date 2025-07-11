package com.example.forwarder.domain.repository

import android.content.Context
import com.example.forwarder.domain.model.Api
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.forwarder.domain.model.VerificationMethod
import androidx.core.content.edit
import com.example.forwarder.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class ApiRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadApiList(): MutableList<Api> {
        val json = prefs.getString(KEY_API_LIST, null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Api>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun saveApiList(list: List<Api>) {
        prefs.edit { putString(KEY_API_LIST, gson.toJson(list)) }
    }

    // Serialize a single Api to JSON
    fun toJson(api: Api): String = gson.toJson(api)

    // Deserialize JSON to Api
    fun fromJson(json: String): Api = gson.fromJson(json, Api::class.java)

    /**
     * Returns a sanitized copy of the given Api according to rules:
     * - if HTTPS disabled, tlsConfig is null
     * - if skipCaVerify is false, clear verificationMethod and credential
     * - if verificationMethod is NONE or null, clear credential
     * - clear empty strings and empty lists in UrlConfig to null
     */
    fun sanitizeApi(api: Api): Api {
        val tls = api.tls
        val isHttps = api.url.baseUrl.lowercase().startsWith("https://")
        var sanitizedTls: com.example.forwarder.domain.model.TlsConfig? = tls
        if (!isHttps) {
            sanitizedTls = null
        } else if (tls != null) {
            // only if skipCaVerify allowed
            if (!tls.selfSignedCert) {
                sanitizedTls = sanitizedTls?.copy(verificationMethod = null, credential = null)
            }
            // if method none or null, clear credential
            if (sanitizedTls?.verificationMethod == null || sanitizedTls.verificationMethod == VerificationMethod.NONE) {
                sanitizedTls = sanitizedTls?.copy(credential = null)
            }
        }
        // return a new Api with sanitized tls
        return api.copy(tls = sanitizedTls)
    }

    fun saveApi(api: Api, index: Int? = null) {
        val sanitizedApi = sanitizeApi(api)
        val list = loadApiList()
        if (index != null && index in list.indices) list[index] = sanitizedApi else list.add(sanitizedApi)
        saveApiList(list)
    }

    /**
     * Test an example message using the given API config.
     * Pops up a dialog to fill sender and content, then sends.
     * Returns true if sent successfully, false otherwise.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testExampleMessage(
        context: android.content.Context,
        api: Api,
        onResult: (Boolean) -> Unit
    ) {
        val layout = android.view.LayoutInflater.from(context).inflate(com.example.forwarder.R.layout.dialog_test_example_message, null)
        val editSender = layout.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.forwarder.R.id.edit_sender)
        val editContent = layout.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.forwarder.R.id.edit_content)
        android.app.AlertDialog.Builder(context)
            .setTitle(R.string.test_example_message)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val sms = com.example.forwarder.domain.model.Sms(
                    id = 1L,
                    sender = editSender.text?.toString() ?: "",
                    content = editContent.text?.toString() ?: "",
                    timestamp = System.currentTimeMillis(),
                    forwarded = false
                )
                GlobalScope.launch(Dispatchers.IO) {
                    val result = try {
                        com.example.forwarder.util.Forwarder.forwardSms(sms, api, context)
                    } catch (e: Exception) {
                        false
                    }
                    android.os.Handler(context.mainLooper).post {
                        onResult(result)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val PREFS_NAME = "api_settings"
        private const val KEY_API_LIST = "api_list"

        /**
         * Return formatted URL with scheme and host only
         */
        fun getUrl(api: Api): String {
            return api.url.baseUrl
        }
    }
}
