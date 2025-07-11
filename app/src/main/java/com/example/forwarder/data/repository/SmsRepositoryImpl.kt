package com.example.forwarder.data

import android.content.Context
import android.provider.Telephony
import com.example.forwarder.domain.model.Sms
import com.example.forwarder.domain.repository.SmsRepository

class SmsRepositoryImpl(private val context: Context) : SmsRepository {
    override fun readSmsMessages(): List<Sms> {
        val sms = mutableListOf<Sms>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            null, null, Telephony.Sms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
            val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                sms.add(
                    Sms(
                        if (idIdx >= 0) cursor.getLong(idIdx) else 0,
                        if (addrIdx >= 0) cursor.getString(addrIdx) ?: "Unknown" else "Unknown",
                        if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "No content" else "No content",
                        if (dateIdx >= 0) cursor.getLong(dateIdx) else 0
                    )
                )
            }
        }
        return sms
    }
}