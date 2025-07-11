package com.example.forwarder.presentation.model

import androidx.lifecycle.ViewModel
import com.example.forwarder.domain.model.Sms
import com.example.forwarder.domain.repository.SmsRepository

class SmsViewModel(private val smsRepository: SmsRepository) : ViewModel() {
    fun getMessages(): List<Sms> = smsRepository.readSmsMessages()
}