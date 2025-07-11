/**
 * @file PermissionCheckActivity.kt
 * @brief Activity for checking and requesting SMS permissions.
 */
package com.example.forwarder.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.forwarder.R
import com.example.forwarder.util.PermissionManager

class PermissionCheckActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private var isUIInitialized = false

    // This activity is used to check and request SMS permissions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)
        permissionManager.initPermissionLauncher(
            this,
            onGranted = { redirect() },
            onDenied = { permissionManager.showPermissionDeniedDialog() }
        )
        setContentView(R.layout.activity_permission_check)
        setupRequestPermissionsButton()
        isUIInitialized = true
        redirectIfNeeded()
    }

    // This method is called when the activity is resumed
    public override fun onResume() {
        super.onResume()
        redirectIfNeeded()
    }

    private fun redirectIfNeeded() {
        if (permissionManager.hasRequiredPermissions()) {
            redirect()
        }
        // Fix:
        // No longer reinitializing UI or calling setContentView
    }

    private fun setupRequestPermissionsButton() {
        val requestPermissionsButton: Button = findViewById(R.id.btnRequestPermissions)
        requestPermissionsButton.setOnClickListener {
            permissionManager.requestPermissions()
        }
    }

    private fun redirect() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}

