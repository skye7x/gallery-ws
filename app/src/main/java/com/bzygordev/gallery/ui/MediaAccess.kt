package com.bzygordev.gallery.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

object MediaPermissions {
    fun required(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        Build.VERSION.SDK_INT >= 29 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /** True when the app can read at least some of the library (on Android 14+ "selected photos" counts). */
    fun hasAccess(context: Context): Boolean {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= 34 ->
                granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                    granted(Manifest.permission.READ_MEDIA_VIDEO) ||
                    granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 ->
                granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                    granted(Manifest.permission.READ_MEDIA_VIDEO)
            else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shows [content] once the app may read the device's photos; otherwise asks for access. */
@Composable
fun MediaAccessGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(MediaPermissions.hasAccess(context)) }
    var needsSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAccess = MediaPermissions.hasAccess(context)
        if (!hasAccess) {
            val activity = context.findActivity()
            needsSettings = activity != null && MediaPermissions.required().none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        }
    }

    // Pick up a permission granted from system settings while the app was in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = MediaPermissions.hasAccess(context)
    }

    LaunchedEffect(Unit) {
        if (!hasAccess) launcher.launch(MediaPermissions.required())
    }

    if (hasAccess) {
        content()
    } else {
        PermissionPrompt(
            needsSettings = needsSettings,
            onAllow = { launcher.launch(MediaPermissions.required()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        )
    }
}

@Composable
private fun PermissionPrompt(
    needsSettings: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isLight = !isSystemInDarkTheme()
    val textColor = if (isLight) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLight) Color.White else Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Access your photos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Gallery needs permission to show the photos and videos stored on this device and on your SD card.",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = if (needsSettings) onOpenSettings else onAllow) {
            Text(if (needsSettings) "Open Settings" else "Allow access")
        }
    }
}
