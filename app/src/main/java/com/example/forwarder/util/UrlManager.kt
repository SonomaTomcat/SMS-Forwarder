package com.example.forwarder.util

import android.content.Context
import android.net.Uri
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import android.app.AlertDialog
import com.example.forwarder.R

class UrlManager(
    private val context: Context,
    private val getString: (Int, Array<Any>) -> String
) {
    // Returns the default port for HTTP or HTTPS
    fun getDefaultPort(isHttps: Boolean): Int = if (isHttps) 443 else 80

    // Checks if the host is a valid IP address
    fun isIpValid(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return try {
            val asciiHost = IDN.toASCII(host)
            InetAddress.getByName(asciiHost)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Checks if the host is a valid domain name (RFC 1035)
    fun isDomainValid(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return try {
            val asciiHost = IDN.toASCII(host)
            val domainRegex = Regex("^(?=.{1,253})(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}")
            asciiHost.matches(domainRegex)
        } catch (e: Exception) {
            false
        }
    }

    fun isHostValid(host: String?): Boolean {
        return isIpValid(host) || isDomainValid(host)
    }

    // Checks if the port string is a valid port number (0-65535)
    fun isPortValid(portStr: String?): Boolean {
        val portNum = portStr?.trim()?.toIntOrNull()
        return portNum != null && portNum in 0..65535
    }

    fun isPathValid(path: String?): Boolean {
        if (path.isNullOrBlank()) return true // Empty path is considered valid
        return try {
            URI(null, null, path, null).path == path
        } catch (e: Exception) {
            false
        }
    }

    fun isQueryValid(query: String?): Boolean {
        if (query.isNullOrBlank()) return true // Empty query is considered valid
        return try {
            URI(null, null, "/", query).query == query
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parses a URL string into its components: scheme, host, port, path, and query.
     * @param url The URL string to parse.
     * @return A map containing the parsed components, or null if the URL is invalid.
     */
    fun parseUrl(url: String): Map<String, String?>? {
        return try {
            val uri = Uri.parse(url)
            mapOf(
                "scheme" to uri.scheme,
                "host" to uri.host,
                "port" to uri.port.takeIf { it != -1 }?.toString(),
                "path" to uri.path,
                "query" to uri.query
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the given URL is valid.
     * @param url The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    fun isUrlValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme
            val host = uri.host
            !scheme.isNullOrBlank() && (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shows a warning dialog when the URL scheme is HTTP.
     * @param onConfirm Callback when user confirms using HTTP.
     * @param onCancel Optional callback when user cancels.
     */
    fun showHttpSchemeWarning(onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        AlertDialog.Builder(context)
            .setTitle(R.string.warning_title)
            .setMessage(R.string.http_warning_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel?.invoke() }
            .show()
    }
}
