package com.example.dugo.home

import android.location.Location

data class Donor(
    val id: Int,
    val name: String,
    val bloodType: String,
    val availableToday: Boolean,
    val area: String,
    val phone: String,
    val lat: Double,
    val lng: Double
)

data class Area(val name: String, val lat: Double, val lng: Double)

// Dalaguete town center (approx.)
const val DALAGUETE_LAT = 9.7612
const val DALAGUETE_LNG = 123.5349

// A few Dalaguete barangays (approximate centers) used by the area filter
val DALAGUETE_AREAS = listOf(
    Area("Poblacion", 9.7612, 123.5349),
    Area("Consolacion", 9.7489, 123.5157),
    Area("Coro", 9.7873, 123.5234),
    Area("Dumalan", 9.8213, 123.4828)
)

/** Sample data until a real backend is connected. Positions are approximate. */
val sampleDonors = listOf(
    Donor(1, "Mia Santos", "O+", true, "Poblacion", "09171234567", 9.7650, 123.5330),
    Donor(2, "Liza Gomez", "O-", true, "Poblacion", "09211234567", 9.7600, 123.5368),
    Donor(3, "Carlo Reyes", "A+", true, "Consolacion", "09181234567", 9.7489, 123.5157),
    Donor(4, "Ana Lim", "AB+", false, "Coro", "09191234567", 9.7873, 123.5234),
    Donor(5, "Paolo Cruz", "B-", true, "Dumalan", "09201234567", 9.8213, 123.4828)
)

/** Straight-line distance in km between two lat/lng points. */
fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, result)
    return result[0] / 1000.0
}

/** Straight-line distance in km from a point to the donor. */
fun Donor.distanceKmFrom(lat: Double, lng: Double): Double =
    distanceKm(lat, lng, this.lat, this.lng)

data class NearestResult(val donor: Donor, val distanceKm: Double)

/**
 * Returns donors sorted by distance from the given origin, optionally filtered
 * by blood type, within [radiusKm].
 */
fun findNearestDonors(
    donors: List<Donor>,
    bloodType: String?,
    originLat: Double,
    originLng: Double,
    radiusKm: Double
): List<NearestResult> =
    donors
        .filter { bloodType == null || it.bloodType == bloodType }
        .map { NearestResult(it, distanceKm(originLat, originLng, it.lat, it.lng)) }
        .filter { it.distanceKm <= radiusKm }
        .sortedBy { it.distanceKm }