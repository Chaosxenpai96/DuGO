package com.example.dugo.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** True when the app may read the device's position. */
fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return runCatching {
        lm.getProviders(true)
            .mapNotNull { lm.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }.getOrNull()
}

/**
 * Requests a fresh GPS fix, falling back to the last known location. Returns
 * latitude to longitude, or null when the position can't be determined.
 */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val token = CancellationTokenSource()
            Tasks.await(client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token))
        }.getOrNull()?.let { it.latitude to it.longitude } ?: lastKnownLocation(context)
    }

/**
 * Turns coordinates into a place name like "Lahug, Cebu City, Cebu".
 *
 * Android's geocoder needs a backend service for this, so it can fail or come
 * back empty. Returns null in that case, and the caller falls back to a
 * description built from the coordinates themselves.
 */
@SuppressLint("MissingPermission")
suspend fun describeLocation(context: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(lat, lng, 1)
            val address = results?.firstOrNull() ?: return@runCatching null
            // The most specific part first: barangay, then city, then province.
            val parts = listOfNotNull(
                address.subLocality?.takeIf { it.isNotBlank() },
                address.locality?.takeIf { it.isNotBlank() },
                address.subAdminArea?.takeIf { it.isNotBlank() }
            ).distinct()
            if (parts.isEmpty()) null else parts.joinToString(", ")
        }.getOrNull()
    }
