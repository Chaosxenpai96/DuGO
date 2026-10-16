// ─────────────────────────────────────────────────────────────────────────────
// FILE TO CREATE : UserLocation.kt
// PUT IT IN      : app/src/main/java/com/example/dugo/home/
// WHAT IT DOES   : Gets the phone's current position (no extra libraries) so the
//                  donor list can be measured from where the seeker really is.
//                  Needs the location permission (already requested in HomeScreenMap).
// ─────────────────────────────────────────────────────────────────────────────
package com.example.dugo.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * Calls [onLocation] with the last known position right away (if any),
 * then again with a fresh one. Does nothing if permission/location is off.
 */
@SuppressLint("MissingPermission")
fun fetchUserLocation(context: Context, onLocation: (Location) -> Unit) {
    val hasPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

    try {
        // 1) Quick: most recent position the phone already knows
        val last = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null) onLocation(last)

        // 2) Accurate: ask for a fresh position
        if (!LocationManagerCompat.isLocationEnabled(manager)) return
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return
        }
        LocationManagerCompat.getCurrentLocation(
            manager, provider, null, ContextCompat.getMainExecutor(context)
        ) { location ->
            if (location != null) onLocation(location)
        }
    } catch (e: SecurityException) {
        // Permission was revoked while running; keep using the fallback (Dalaguete center)
    }
}
