/**
 * @file SettingsActivity.kt
 * @brief Activity for displaying and managing settings.
 */
package com.example.forwarder.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.forwarder.R
import com.example.forwarder.presentation.model.ApiManagerFragment
import com.example.forwarder.domain.SettingsType

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        // Set up the custom Toolbar as the ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set the title based on the settings type
        val typeName = intent.getStringExtra(ARG_SETTINGS_TYPE)
        val type = typeName?.let { SettingsType.valueOf(it) }
        val titleRes = when (type) {
            SettingsType.API -> R.string.api_manager
            SettingsType.INTERFACE -> R.string.settings
            else -> R.string.settings
        }
        supportActionBar?.title = getString(titleRes)

        // Load the appropriate fragment based on the settings type
        if (savedInstanceState == null) {
            val typeName = intent.getStringExtra(ARG_SETTINGS_TYPE)
            val type = typeName?.let { SettingsType.valueOf(it) }
            val fragment = when (type) {
                SettingsType.INTERFACE -> SettingsFragment()
                SettingsType.API -> ApiManagerFragment()
                else -> null
            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.settings_container, it)
                    .commit()
            }
        }
    }

    // Handle the back button in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Handle the menu item clicks
    companion object {
        const val ARG_SETTINGS_TYPE = "settings_type"

        fun start(context: Context, type: SettingsType) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.putExtra(ARG_SETTINGS_TYPE, type.name)
            context.startActivity(intent)
        }
    }
}

