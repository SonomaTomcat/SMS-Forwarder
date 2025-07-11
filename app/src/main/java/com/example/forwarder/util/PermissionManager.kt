package com.example.forwarder.util

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.provider.Settings
    import androidx.activity.result.ActivityResultCaller
    import androidx.activity.result.ActivityResultLauncher
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.ContextCompat
    import com.example.forwarder.R

    class PermissionManager(
        private val activity: AppCompatActivity
    ) {
        private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
        private var onGranted: (() -> Unit)? = null
        private var onDenied: (() -> Unit)? = null

        private val requiredPermissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        fun initPermissionLauncher(
            caller: ActivityResultCaller,
            onGranted: () -> Unit,
            onDenied: () -> Unit
        ) {
            this.onGranted = onGranted
            this.onDenied = onDenied
            permissionLauncher = caller.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.all { it.value }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }

        fun requestPermissions() {
            permissionLauncher.launch(requiredPermissions)
        }

        fun hasRequiredPermissions(): Boolean {
            return requiredPermissions.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun showPermissionDeniedDialog() {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.permissions_required))
                .setMessage(activity.getString(R.string.sms_permissions_needed))
                .setPositiveButton(activity.getString(R.string.settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", activity.packageName, null)
                    activity.startActivity(intent)
                }
                .setNegativeButton(activity.getString(android.R.string.cancel), null)
                .show()
        }
    }