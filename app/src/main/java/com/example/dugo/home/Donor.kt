package com.example.dugo.home

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

// Farthest a donor can be and still show up in the distance search
const val MAX_RADIUS_KM = 20.0

// If the phone's GPS is farther than this from Dalaguete, searches start from the town center instead
const val GPS_NEAR_TOWN_KM = 30.0

/** Haversine: straight-line distance in km between two coordinates. */
fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).let { it * it }
    // coerceAtMost guards against tiny floating-point overshoot that would make asin return NaN
    return 2 * earthRadiusKm * asin(sqrt(a).coerceAtMost(1.0))
}

/** Straight-line distance in km from a point to the donor. */
fun Donor.distanceKmFrom(lat: Double, lng: Double): Double =
    distanceKm(lat, lng, this.lat, this.lng)

data class DonorMatch(val donor: Donor, val distanceKm: Double)

/**
 * Keeps donors with the same blood type (null = any type) that are within
 * radiusKm (capped at MAX_RADIUS_KM) of the origin, sorted nearest first.
 */
fun findNearestDonors(
    donors: List<Donor>,
    bloodType: String?,
    originLat: Double,
    originLng: Double,
    radiusKm: Double
): List<DonorMatch> {
    val limit = radiusKm.coerceAtMost(MAX_RADIUS_KM)
    return donors
        .asSequence()
        .filter { bloodType == null || it.bloodType == bloodType }
        .map { DonorMatch(it, distanceKm(originLat, originLng, it.lat, it.lng)) }
        .filter { it.distanceKm <= limit }
        .sortedBy { it.distanceKm }
        .toList()
}