package com.example.forwarder.domain.model

data class Sms(
    val id: Long,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val forwarded: Boolean = false
)

