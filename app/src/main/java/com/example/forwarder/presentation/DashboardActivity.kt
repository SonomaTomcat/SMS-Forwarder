package com.example.forwarder.presentation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forwarder.R
import com.example.forwarder.data.SmsRepositoryImpl
import com.example.forwarder.domain.SettingsType
import com.example.forwarder.presentation.adapter.SmsListAdapter
import com.example.forwarder.presentation.model.SmsViewModel
import com.example.forwarder.util.PermissionManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var smsListAdapter: SmsListAdapter
    private lateinit var permissionManager: PermissionManager

    // ViewModel initialization with a custom factory
    private val viewModel: SmsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val smsRepository = SmsRepositoryImpl(this@DashboardActivity)
                @Suppress("UNCHECKED_CAST")
                return SmsViewModel(smsRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * Fix:
         * 1. Check permissions before initializing the UI to prevent crashes if permissions are not granted.
         * 2. Redirect to PermissionCheckActivity if permissions are not granted.
         */
        permissionManager = PermissionManager(this)
        if (!permissionManager.hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionCheckActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_dashboard)

        // Set up the custom Toolbar as the ActionBar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        messageRecyclerView = findViewById(R.id.messageRecyclerView)

        smsListAdapter = SmsListAdapter(mutableListOf(), this) //< Pass context instead of settingsManager
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = smsListAdapter

        // Set click listener for the start button
        startButton.setOnClickListener {
            refreshSmsMessages()
        }

        loadSmsMessages()
    }

    // Inflate the menu with submenu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    // Handle submenu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_interface -> {
                SettingsActivity.start(this, SettingsType.INTERFACE)
                true
            }
            R.id.action_api -> {
                SettingsActivity.start(this, SettingsType.API)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Fix:
        // 2. Still check permissions in onResume to prevent runtime permission revocation from causing a crash.
        checkPermission()
        refreshSmsMessages()
    }

    // Check if required permissions are granted
    private fun checkPermission() {
        if (!permissionManager.hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionCheckActivity::class.java))
            finish()
        }
    }

    // Load SMS messages from the ViewModel
    private fun loadSmsMessages() {
        val messages = viewModel.getMessages()
        smsListAdapter.updateMessages(messages)
    }

    // Refresh the SMS messages and update the status text
    private fun refreshSmsMessages() {
        loadSmsMessages()
        statusTextView.text = getString(R.string.messages_refreshed)
    }
}

