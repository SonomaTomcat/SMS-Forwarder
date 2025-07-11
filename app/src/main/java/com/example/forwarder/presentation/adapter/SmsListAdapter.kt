package com.example.forwarder.presentation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forwarder.R
import com.example.forwarder.domain.model.Sms
import com.example.forwarder.util.DateFormatter

class SmsListAdapter(
    private val sms: MutableList<Sms>,
    private val context: Context
) : RecyclerView.Adapter<SmsListAdapter.SmsViewHolder>() {

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val senderTextView: TextView = view.findViewById(R.id.senderTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val sms = sms[position]
        holder.senderTextView.text = sms.sender
        holder.contentTextView.text = sms.content
        val dateFormat = preferences.getString("time_format", "yyyy-MM-dd HH:mm:ss z") ?: "yyyy-MM-dd HH:mm:ss z"
        holder.dateTextView.text = DateFormatter.getFormattedTime(sms.timestamp, dateFormat)
    }

    override fun getItemCount() = sms.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateMessages(newSms: List<Sms>) {
        sms.clear()
        sms.addAll(newSms)
        notifyDataSetChanged()
    }
}