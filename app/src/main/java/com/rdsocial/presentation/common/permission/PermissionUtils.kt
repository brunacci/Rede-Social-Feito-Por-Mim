package com.rdsocial.presentation.common.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun galleryPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun cameraPermission(): String = Manifest.permission.CAMERA

    fun locationPermissions(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun resolveStatus(
        activity: Activity?,
        context: Context,
        permission: String,
        hasRequestedBefore: Boolean,
    ): PermissionStatus {
        if (isPermissionGranted(context, permission)) return PermissionStatus.Granted
        if (!hasRequestedBefore) return PermissionStatus.Denied
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false
        return if (shouldShowRationale) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
    }

    fun resolveLocationStatus(
        activity: Activity?,
        context: Context,
        hasRequestedBefore: Boolean,
    ): PermissionStatus {
        val hasFine = isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (hasFine || hasCoarse) return PermissionStatus.Granted
        if (!hasRequestedBefore) return PermissionStatus.Denied
        val shouldShowFine = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } ?: false
        val shouldShowCoarse = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: false
        return if (shouldShowFine || shouldShowCoarse) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isLocationEnabled
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        context.startActivity(intent)
    }
}
