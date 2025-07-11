/**
 * @file ApiUrlSettings.kt
 * @brief Data class for managing API URL settings.
 */
package com.example.forwarder.domain.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/** TLS verification methods */
enum class VerificationMethod {
    NONE,        // no verification
    SHA256,      // validate by certificate SHA-256 fingerprint
    CERTIFICATE  // validate by exact certificate string match

}

// TLS configuration container
@Parcelize
data class TlsConfig(
    var selfSignedCert: Boolean = false,        // enable self-signed cert handling
    var verificationMethod: VerificationMethod? = null,
    var credential: String? = null // certificate text or SHA-256 fingerprint
): Parcelable

// query parameter tuple
@Parcelize
data class QueryParam(
    var key: String? = null,
    var value: String? = null
): Parcelable

@Parcelize
data class UrlConfig(
    var method: String = "POST",
    var baseUrl: String = "",
    var queries: MutableList<QueryParam>? = null
) : Parcelable

@Parcelize
data class Api(
    val remark: String = "",
    val url: UrlConfig = UrlConfig(),
    val resolve: String? = null,
    val tls: TlsConfig? = null
) : Parcelable

