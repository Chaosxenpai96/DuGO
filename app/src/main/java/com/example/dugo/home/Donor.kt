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
    val lng: Double,
    /** True for the signed-in donor's own pin. */
    val isSelf: Boolean = false,
    /** Firebase uid of a real account. Empty for the sample donors. */
    val uid: String = ""
)

data class Area(val name: String, val lat: Double, val lng: Double)

// Default map centre, used until the device's position is known. Cebu City.
const val DEFAULT_LAT = 10.3167
const val DEFAULT_LNG = 123.8854

/**
 * The major areas around Cebu island. Used to place the sample donors and to
 * centre the map when an area filter is picked.
 */
val CEBU_REGIONS = listOf(
    Area("Cebu City", 10.3167, 123.8854),
    Area("Lapu-Lapu City", 10.3106, 123.9854),
    Area("Mandaue", 10.2353, 123.9422),
    Area("Talisay", 10.3075, 123.8475),
    Area("Naga & Daanbantayan", 10.3333, 123.7167),
    Area("Bogo & north", 10.8075, 124.0056),
    Area("Danao & Carmen", 10.2486, 123.8842),
    Area("Argao & Dumanjug", 10.3167, 123.6167),
    Area("Sogod & Getafe", 10.7864, 123.3764),
    Area("Barili", 10.4147, 123.5014),
    Area("Moalboal", 9.9400, 123.3683),
    Area("Toledo City", 10.1833, 123.4667),
    Area("Carcar", 10.7333, 123.8500),
    Area("Liloan", 10.1314, 123.7358)
)

/** Places a sample donor a short distance from the middle of their area. */
private fun donor(
    id: Int,
    name: String,
    bloodType: String,
    available: Boolean,
    area: Area,
    phone: String,
    dLat: Double,
    dLng: Double
) = Donor(id, name, bloodType, available, area.name, phone, area.lat + dLat, area.lng + dLng)

/**
 * Sample donors, spread across the major areas of Cebu island so the map and
 * the "find nearest donor" search can be tested from different locations.
 * Positions are approximate. These are not real people.
 */
val sampleDonors: List<Donor> = buildList {
    var id = 1
    fun addOne(
        area: Area,
        name: String,
        bloodType: String,
        available: Boolean,
        phone: String,
        dLat: Double,
        dLng: Double
    ) {
        add(donor(id++, name, bloodType, available, area, phone, dLat, dLng))
    }

    val cebuCity = CEBU_REGIONS[0]
    addOne(cebuCity, "Mia Santos", "O+", true, "09171234567", 0.012, -0.008)
    addOne(cebuCity, "Liza Gomez", "O-", true, "09211234567", -0.015, 0.010)
    addOne(cebuCity, "Carlo Reyes", "A+", true, "09181234567", 0.006, 0.018)
    addOne(cebuCity, "Ana Lim", "AB+", false, "09191234567", -0.008, -0.020)
    addOne(cebuCity, "Paolo Cruz", "B-", true, "09201234567", 0.020, 0.004)
    addOne(cebuCity, "Grace Uy", "B+", true, "09211234567", -0.004, 0.026)

    val lapuLapu = CEBU_REGIONS[1]
    addOne(lapuLapu, "Rico Tano", "A-", true, "09221234567", 0.009, -0.006)
    addOne(lapuLapu, "Tina Abella", "O+", false, "09231234567", -0.011, 0.008)
    addOne(lapuLapu, "Jomar Villareal", "AB-", true, "09241234567", 0.014, 0.011)
    addOne(lapuLapu, "Nina Alonzo", "A+", true, "09251234567", -0.007, -0.014)
    addOne(lapuLapu, "Ricky Laurel", "O-", true, "09261234567", 0.004, 0.019)

    val mandaue = CEBU_REGIONS[2]
    addOne(mandaue, "Jude Caceres", "B+", true, "09271234567", 0.007, 0.006)
    addOne(mandaue, "Kaye Manalo", "O+", true, "09281234567", -0.009, -0.007)
    addOne(mandaue, "Marlon Dy", "A+", false, "09291234567", 0.012, -0.012)
    addOne(mandaue, "Tessa Lim", "AB+", true, "09301234567", -0.005, 0.015)
    addOne(mandaue, "Yves Fajardo", "O-", true, "09311234567", 0.015, 0.002)

    val talisay = CEBU_REGIONS[3]
    addOne(talisay, "Bela Singson", "B-", true, "09321234567", 0.008, -0.009)
    addOne(talisay, "Omar Tiu", "A+", true, "09331234567", -0.010, 0.007)
    addOne(talisay, "Weng Badillo", "O+", false, "09341234567", 0.013, 0.010)
    addOne(talisay, "Ivan Quinto", "AB-", true, "09351234567", -0.006, -0.013)
    addOne(talisay, "Kat Abado", "B+", true, "09361234567", 0.016, 0.005)
    val naga = CEBU_REGIONS[4]
    addOne(naga, "Lorna Pangilinan", "O+", true, "09371234567", 0.009, 0.008)
    addOne(naga, "Danilo Escamilla", "A-", true, "09381234567", -0.012, -0.006)
    addOne(naga, "Mae Tobias", "O-", false, "09391234567", 0.007, -0.014)
    addOne(naga, "Rene Abarca", "B+", true, "09401234567", 0.014, 0.003)
    addOne(naga, "Nilo Villaflor", "AB+", true, "09411234567", -0.008, 0.016)

    val bogo = CEBU_REGIONS[5]
    addOne(bogo, "Ester Muring", "O+", true, "09421234567", 0.010, -0.007)
    addOne(bogo, "Rolly Tabotabo", "A+", false, "09431234567", -0.009, 0.009)
    addOne(bogo, "Carmelita Dayap", "B-", true, "09441234567", 0.006, 0.017)
    addOne(bogo, "Ariel Labot", "O-", true, "09451234567", -0.013, -0.011)
    addOne(bogo, "Cherry Tiu", "AB+", true, "09461234567", 0.013, 0.006)

    val danao = CEBU_REGIONS[6]
    addOne(danao, "Manolo Yniguez", "O+", true, "09471234567", 0.008, 0.007)
    addOne(danao, "Fe Dela Cruz", "A-", true, "09481234567", -0.011, -0.008)
    addOne(danao, "Dan Carlos", "B+", false, "09491234567", 0.012, 0.012)
    addOne(danao, "Leni Tiongson", "O-", true, "09501234567", -0.007, 0.018)
    addOne(danao, "Pepito Almeda", "AB-", true, "09511234567", 0.015, -0.004)

    val argao = CEBU_REGIONS[7]
    addOne(argao, "Boy Tabinas", "O+", true, "09521234567", 0.009, 0.006)
    addOne(argao, "Marites Araneta", "A+", true, "09531234567", -0.010, -0.009)
    addOne(argao, "Jun Abaya", "B-", false, "09541234567", 0.011, 0.014)
    addOne(argao, "Ning Calagui", "O-", true, "09551234567", -0.006, 0.016)
    addOne(argao, "Weng Yap", "AB+", true, "09561234567", 0.014, -0.006)

    val sogod = CEBU_REGIONS[8]
    addOne(sogod, "Eddie Cunanan", "O+", true, "09571234567", 0.010, -0.008)
    addOne(sogod, "Carmela Solis", "A-", true, "09581234567", -0.011, 0.007)
    addOne(sogod, "Rudy Bacsain", "B+", true, "09591234567", 0.007, 0.015)
    addOne(sogod, "Liza Mondragon", "O-", false, "09601234567", -0.008, -0.013)
    addOne(sogod, "Bert Siguion", "AB-", true, "09611234567", 0.013, 0.004)

    val barili = CEBU_REGIONS[9]
    addOne(barili, "Amie Villar", "O+", true, "09621234567", 0.008, 0.005)
    addOne(barili, "Dong Daguio", "A+", true, "09631234567", -0.009, -0.010)
    addOne(barili, "Kat Magsaysay", "B-", true, "09641234567", 0.012, 0.011)
    addOne(barili, "Marlon Caamal", "O-", false, "09651234567", -0.007, 0.017)
    addOne(barili, "Tina Dico", "AB+", true, "09661234567", 0.015, -0.005)

    val moalboal = CEBU_REGIONS[10]
    addOne(moalboal, "Yawl Patching", "O+", true, "09671234567", 0.009, 0.006)
    addOne(moalboal, "Rina Logroho", "A+", true, "09681234567", -0.010, -0.007)
    addOne(moalboal, "Gino Lusad", "B+", false, "09691234567", 0.011, 0.013)
    addOne(moalboal, "Nena Aseo", "O-", true, "09701234567", -0.006, 0.015)
    addOne(moalboal, "Bert Bacsain", "AB-", true, "09711234567", 0.014, -0.004)

    val toledo = CEBU_REGIONS[11]
    addOne(toledo, "Dan Toledo", "O+", true, "09721234567", 0.008, 0.005)
    addOne(toledo, "Mae Poblacion", "A-", true, "09731234567", -0.009, -0.008)
    addOne(toledo, "Carlo Cadenas", "B+", true, "09741234567", 0.010, 0.012)
    addOne(toledo, "Lourdes Malvar", "O-", false, "09751234567", -0.007, 0.016)
    addOne(toledo, "Jun Valledor", "AB+", true, "09761234567", 0.013, -0.005)

    val carcar = CEBU_REGIONS[12]
    addOne(carcar, "Tess Boljoon", "O+", true, "09771234567", 0.009, 0.006)
    addOne(carcar, "Manolo Arboleda", "A+", false, "09781234567", -0.010, -0.007)
    addOne(carcar, "Nena Ourd", "B-", true, "09791234567", 0.011, 0.013)
    addOne(carcar, "Rene Dimaculangan", "O-", true, "09801234567", -0.007, 0.015)
    addOne(carcar, "Boy Bien", "AB-", true, "09811234567", 0.014, -0.004)

    val liloan = CEBU_REGIONS[13]
    addOne(liloan, "Kaye Suyo", "O+", true, "09821234567", 0.008, 0.005)
    addOne(liloan, "Alden Alcaraz", "A-", true, "09831234567", -0.009, -0.008)
    addOne(liloan, "Mae Rama", "B+", true, "09841234567", 0.010, 0.012)
    addOne(liloan, "Tino Yap", "O-", false, "09851234567", -0.007, 0.016)
    addOne(liloan, "Rhea Cabase", "AB+", true, "09861234567", 0.013, -0.005)
}

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
