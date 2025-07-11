package com.example.forwarder.domain.repository

import com.example.forwarder.domain.model.Sms

interface SmsRepository {
    fun readSmsMessages(): List<Sms>
}