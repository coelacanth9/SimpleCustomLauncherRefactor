package com.coelacanth9.simplecustomlauncher.platform

import androidx.compose.runtime.Composable

object PermissionManager {
    val CALENDAR_PERMISSIONS = arrayOf(android.Manifest.permission.READ_CALENDAR)

    fun checkPermissions(context: android.content.Context, permissions: Array<String>): Boolean =
        permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
}

@Composable
fun RequestPermissions(
    context: android.content.Context,
    permissions: Array<String>,
    onResult: (Boolean) -> Unit
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onResult(result.values.all { it })
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!PermissionManager.checkPermissions(context, permissions)) {
            launcher.launch(permissions)
        } else {
            onResult(true)
        }
    }
}
