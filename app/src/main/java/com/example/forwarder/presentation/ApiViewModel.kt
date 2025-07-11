package com.example.forwarder.presentation

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.core.net.toUri
import com.example.forwarder.R
import com.example.forwarder.domain.model.Api
import com.example.forwarder.domain.model.QueryParam
import com.example.forwarder.domain.model.UrlConfig
import com.example.forwarder.domain.model.TlsConfig
import com.example.forwarder.domain.model.VerificationMethod
import com.example.forwarder.util.UrlManager

class ApiViewModel : ViewModel() {
    // Form fields as LiveData for data binding
    val remark = MutableLiveData<String>()
    val method = MutableLiveData<String>("POST")
    val baseUrl = MutableLiveData<String>("")
    val queryList = MutableLiveData<MutableList<QueryParam>>(mutableListOf(QueryParam("", "")))
    val resolve = MutableLiveData<String>()

    // TLS config fields
    val selfSignedCert = MutableLiveData<Boolean>(false)
    val verificationMethod = MutableLiveData<VerificationMethod?>(VerificationMethod.NONE)

    val credential = MutableLiveData<String>()

    val baseUrlError = MutableLiveData<String?>()
    val queryError = MutableLiveData<String?>()
    val remarkError = MutableLiveData<String?>()

    // visibility for TLS input
    val tlsVisibility = MutableLiveData(View.VISIBLE)

    // injected dependencies
    lateinit var repository: com.example.forwarder.domain.repository.ApiRepository
    lateinit var getStringRes: (Int) -> String
    var apiIndex: Int = -1
    var onSaved: (() -> Unit)? = null
    var urlManager: UrlManager? = null

    fun onSaveClick() {
        // validate before saving
        if (validateInput(urlManager!!, getStringRes)) {
            // save with sanitation handled in repository
            repository.saveApi(toApi(), apiIndex.takeIf { it >= 0 })
            onSaved?.invoke()
        }
    }

    fun validateInput(urlManager: UrlManager, getString: (Int) -> String): Boolean {
        var valid = true
        val url = baseUrl.value.orEmpty()
        if (url.isBlank() || !urlManager.isUrlValid(url)) {
            baseUrlError.value = getString(R.string.input_invalid)
            valid = false
        } else baseUrlError.value = null
        // TODO: validate query list
        queryError.value = null
        return valid
    }

    fun addQueryParam() {
        val list = queryList.value ?: mutableListOf()
        list.add(QueryParam("", ""))
        queryList.value = list
    }

    fun removeQueryParam(index: Int) {
        val list = queryList.value ?: return
        if (index in list.indices && list.size > 1) {
            list.removeAt(index)
            queryList.value = list
        }
    }

    fun setApi(newApi: Api) {
        // initialize form fields
        remark.value = newApi.remark
        method.value = newApi.url.method
        baseUrl.value = newApi.url.baseUrl
        // Always show at least one empty row in UI, but do not save it if not filled
        val queries = newApi.url.queries?.toMutableList() ?: mutableListOf()
        if (queries.isEmpty()) queries.add(QueryParam("", ""))
        queryList.value = queries
        resolve.value = newApi.resolve.orEmpty()

        // initialize TLS method and credential
        selfSignedCert.value = newApi.tls?.selfSignedCert ?: false
        verificationMethod.value = newApi.tls?.verificationMethod ?: VerificationMethod.SHA256
        credential.value = newApi.tls?.credential.orEmpty()

        // TLS cert section visibility based on baseUrl prefix
        tlsVisibility.value = if (newApi.url.baseUrl.lowercase().startsWith("https://")) View.VISIBLE else View.GONE
    }

    fun toApi(): Api {
        val urlConfig = UrlConfig(
            method = method.value ?: "POST",
            baseUrl = baseUrl.value.orEmpty(),
            queries = queryList.value,
        )
        // TLS config only if baseUrl is https
        val tlsConfig = if (baseUrl.value.orEmpty().lowercase().startsWith("https://")) {
            TlsConfig(
                selfSignedCert = selfSignedCert.value == true,
                verificationMethod = verificationMethod.value,
                credential = credential.value.orEmpty().takeIf { it.isNotEmpty() }
            )
        } else null
        return Api(
            remark = remark.value.orEmpty(),
            url = urlConfig,
            resolve = resolve.value.orEmpty().takeIf { it.isNotBlank() },
            tls = tlsConfig
        )
    }

    fun setTlsMethod(method: VerificationMethod) {
        verificationMethod.value = method
    }
}
