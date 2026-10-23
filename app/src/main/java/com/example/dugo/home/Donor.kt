package com.example.dugo.home

import android.location.Location

data class Donor(
    val id: Int,
    val name: String,
    val bloodType: String,
    val availableToday: Boolean,
    /** Where the donor is, e.g. "Lahug, Cebu City". */
    val area: String,
    val phone: String,
    val lat: Double,
    val lng: Double,
    /** True for the signed-in donor's own pin. */
    val isSelf: Boolean = false,
    /** Firebase uid of a real account. Empty for the sample donors. */
    val uid: String = "",
    /** The town the donor is in. Used by the area filter. */
    val region: String = ""
)

data class Area(val name: String, val lat: Double, val lng: Double)

// Default map centre, used until the device's position is known. Cebu City.
const val DEFAULT_LAT = 10.3230
const val DEFAULT_LNG = 123.9060

/** A person in the sample data. */
private data class SamplePerson(val name: String, val bloodType: String, val available: Boolean)

/**
 * A town around Cebu island, with the barangays the sample donors are placed
 * in. The centre is the real one, so a pin inside the town lands on land and
 * the area filter matches what the map shows.
 */
private data class SampleTown(
    val name: String,
    val lat: Double,
    val lng: Double,
    val barangays: List<String>,
    val people: List<SamplePerson>
)

/**
 * The towns the app knows about. One entry per municipality, so a donor is
 * never labelled with two places at once.
 */
private val SAMPLE_TOWNS = listOf(
    SampleTown(
        "Cebu City", 10.3230, 123.9060,
        listOf("Poblacion", "Lahug", "Mabolo", "Tisa", "Talamban", "Santo Niño"),
        listOf(
            SamplePerson("Mia Santos", "O+", true),
            SamplePerson("Liza Gomez", "O-", true),
            SamplePerson("Carlo Reyes", "A+", true),
            SamplePerson("Ana Lim", "A-", false),
            SamplePerson("Paolo Cruz", "B+", true),
            SamplePerson("Grace Uy", "B-", true)
        )
    ),
    SampleTown(
        "Lapu-Lapu City", 10.2633, 123.9533,
        listOf("Poblacion", "Mactan", "Pusok", "Canlubang", "Looc"),
        listOf(
            SamplePerson("Rico Tano", "A-", true),
            SamplePerson("Tina Abella", "B+", false),
            SamplePerson("Jomar Villareal", "B-", true),
            SamplePerson("Nina Alonzo", "AB+", true),
            SamplePerson("Ricky Laurel", "AB-", true)
        )
    ),
    SampleTown(
        "Mandaue", 10.3154, 124.0028,
        listOf("Poblacion", "Subdivision", "Udo", "Tabok", "Pak-na-Luma"),
        listOf(
            SamplePerson("Jude Caceres", "AB+", true),
            SamplePerson("Kaye Manalo", "AB-", true),
            SamplePerson("Marlon Dy", "O+", false),
            SamplePerson("Tessa Lim", "O-", true),
            SamplePerson("Yves Fajardo", "A+", true)
        )
    ),
    SampleTown(
        "Talisay", 10.2459, 123.8252,
        listOf("Poblacion", "Tabunan", "Lutopan", "Cawongan", "Candabong"),
        listOf(
            SamplePerson("Bela Singson", "O-", true),
            SamplePerson("Omar Tiu", "A+", false),
            SamplePerson("Weng Badillo", "A-", true),
            SamplePerson("Ivan Quinto", "B+", true),
            SamplePerson("Kat Abado", "B-", true)
        )
    ),
    SampleTown(
        "Naga", 10.0855, 123.7266,
        listOf("Poblacion", "Tago", "Pasil", "San Vicente", "Malagtas"),
        listOf(
            SamplePerson("Lorna Pangilinan", "B+", true),
            SamplePerson("Danilo Escamilla", "B-", false),
            SamplePerson("Mae Tobias", "AB+", true),
            SamplePerson("Rene Abarca", "AB-", true),
            SamplePerson("Nilo Villaflor", "O+", true)
        )
    ),
    SampleTown(
        "Daanbantayan", 11.2258, 123.8992,
        listOf("Poblacion", "Cantiguib", "Tabok", "Tortal", "Busay"),
        listOf(
            SamplePerson("Ester Muring", "AB-", true),
            SamplePerson("Rolly Tabotabo", "O+", false),
            SamplePerson("Carmelita Dayap", "O-", true),
            SamplePerson("Ariel Labot", "A+", true),
            SamplePerson("Cherry Tiu", "A-", true)
        )
    ),
    SampleTown(
        "Bogo", 11.0042, 124.0035,
        listOf("Poblacion", "Cogon", "Napo", "Tabok", "Guinsay"),
        listOf(
            SamplePerson("Manolo Yniguez", "A+", true),
            SamplePerson("Fe Dela Cruz", "A-", false),
            SamplePerson("Dan Carlos", "B+", true),
            SamplePerson("Leni Tiongson", "B-", true),
            SamplePerson("Pepito Almeda", "AB+", true)
        )
    ),
    SampleTown(
        "Danao", 10.5468, 123.9841,
        listOf("Poblacion", "Tabok", "Alang", "Tapon", "Canturing"),
        listOf(
            SamplePerson("Boy Tabinas", "B-", true),
            SamplePerson("Marites Araneta", "AB+", false),
            SamplePerson("Jun Abaya", "AB-", true),
            SamplePerson("Ning Calagui", "O+", true),
            SamplePerson("Weng Yap", "O-", true)
        )
    ),
    SampleTown(
        "Carmen", 10.5672, 123.9975,
        listOf("Poblacion", "Alegria", "Tabog", "Tubog", "Taming"),
        listOf(
            SamplePerson("Eddie Cunanan", "O+", true),
            SamplePerson("Carmela Solis", "O-", false),
            SamplePerson("Rudy Bacsain", "A+", true),
            SamplePerson("Liza Mondragon", "A-", true),
            SamplePerson("Bert Siguion", "B+", true)
        )
    ),
    SampleTown(
        "Argao", 9.9127, 123.4775,
        listOf("Poblacion", "Bago", "Bahay", "Tabok", "Mesa"),
        listOf(
            SamplePerson("Amie Villar", "A-", true),
            SamplePerson("Dong Daguio", "B+", false),
            SamplePerson("Kat Magsaysay", "B-", true),
            SamplePerson("Marlon Caamal", "AB+", true),
            SamplePerson("Tina Dico", "AB-", true)
        )
    ),
    SampleTown(
        "Dumanjug", 10.0262, 123.5244,
        listOf("Poblacion", "Alejos", "Tabok", "Mambajog", "Suba"),
        listOf(
            SamplePerson("Yawl Patching", "AB+", true),
            SamplePerson("Rina Logroho", "AB-", false),
            SamplePerson("Gino Lusad", "O+", true),
            SamplePerson("Nena Aseo", "O-", true),
            SamplePerson("Bert Bacsain", "A+", true)
        )
    ),
    SampleTown(
        "Sogod", 10.0395, 123.6440,
        listOf("Poblacion", "Calanggaman", "Pound", "Tabok", "Wer"),
        listOf(
            SamplePerson("Dan Toledo", "O-", true),
            SamplePerson("Mae Poblacion", "A+", false),
            SamplePerson("Carlo Cadenas", "A-", true),
            SamplePerson("Lourdes Malvar", "B+", true),
            SamplePerson("Jun Valledor", "B-", true)
        )
    ),
    SampleTown(
        "Dalaguete", 9.9780, 123.5840,
        listOf("Poblacion", "Tayud", "Tabok", "Calanipawan", "Magsaysay"),
        listOf(
            SamplePerson("Tess Boljoon", "B+", true),
            SamplePerson("Manolo Arboleda", "B-", false),
            SamplePerson("Nena Ourd", "AB+", true),
            SamplePerson("Rene Dimaculangan", "AB-", true),
            SamplePerson("Boy Bien", "O+", true)
        )
    ),
    SampleTown(
        "Barili", 10.1118, 123.6050,
        listOf("Poblacion", "Batuan", "Buan", "Tangnan", "Vito"),
        listOf(
            SamplePerson("Kaye Suyo", "AB-", true),
            SamplePerson("Alden Alcaraz", "O+", false),
            SamplePerson("Mae Rama", "O-", true),
            SamplePerson("Tino Yap", "A+", true),
            SamplePerson("Rhea Cabase", "A-", true)
        )
    ),
    SampleTown(
        "Moalboal", 9.9570, 123.4000,
        listOf("Poblacion", "Basakdoku", "Bug-os", "Sambag", "Tangka"),
        listOf(
            SamplePerson("Anya Gretchin", "A+", true),
            SamplePerson("Nikko Dela Cruz", "A-", false),
            SamplePerson("Trisha Lim", "B+", true),
            SamplePerson("Benigno Soro", "B-", true),
            SamplePerson("Mylene Apostol", "AB+", true)
        )
    ),
    SampleTown(
        "Toledo City", 10.3724, 123.6338,
        listOf("Poblacion", "San Roque", "Tabok", "Ilihan", "Tomoc"),
        listOf(
            SamplePerson("Cathy Padasoy", "B-", true),
            SamplePerson("Dodjie Almeda", "AB+", false),
            SamplePerson("Ricky Toledo", "AB-", true),
            SamplePerson("Eden Poblacion", "O+", true),
            SamplePerson("Warren Siller", "O-", true)
        )
    ),
    SampleTown(
        "Carcar", 10.1028, 123.6495,
        listOf("Poblacion", "Baragola", "Busilis", "Maya", "Miniloc"),
        listOf(
            SamplePerson("Romel Imperial", "O+", true),
            SamplePerson("Iris Abalos", "O-", false),
            SamplePerson("Nico Caquil", "A+", true),
            SamplePerson("Shane Claudio", "A-", true),
            SamplePerson("Melfa Tiong", "B+", true)
        )
    ),
    SampleTown(
        "Liloan", 10.3000, 123.9830,
        listOf("Poblacion", "Cabase", "Candijay", "Luy-a", "Malinao"),
        listOf(
            SamplePerson("Alma Cancino", "A-", true),
            SamplePerson("Roger Bayog", "B+", false),
            SamplePerson("Joyce Dacoy", "B-", true),
            SamplePerson("Nico Rama", "AB+", true),
            SamplePerson("Fritzie Uy", "AB-", true)
        )
    )
)

/**
 * The major areas around Cebu island. Used to place the sample donors and to
 * centre the map when an area filter is picked.
 */
val CEBU_REGIONS: List<Area> = SAMPLE_TOWNS.map { Area(it.name, it.lat, it.lng) }

/**
 * How far apart the donors inside one town sit, in degrees. Under a kilometre
 * each, so they stay inside the town instead of drifting into the sea.
 */
private val SPOT_OFFSETS = listOf(
    0.006 to 0.004,
    -0.005 to -0.006,
    0.004 to -0.007,
    -0.007 to 0.004,
    0.005 to 0.006,
    -0.003 to 0.007
)

/** A made-up contact number for a sample donor. */
private fun samplePhone(id: Int): String = "09" + (170000000 + id * 1111).toString()

/**
 * Sample donors, spread across the towns of Cebu island so the map and the
 * "find nearest donor" search can be tested from different locations.
 * Positions are approximate. These are not real people.
 */
val sampleDonors: List<Donor> = buildList {
    var id = 1
    SAMPLE_TOWNS.forEach { town ->
        town.people.forEachIndexed { index, person ->
            val (dLat, dLng) = SPOT_OFFSETS[index % SPOT_OFFSETS.size]
            add(
                Donor(
                    id = id,
                    name = person.name,
                    bloodType = person.bloodType,
                    availableToday = person.available,
                    area = "${town.barangays[index]}, ${town.name}",
                    phone = samplePhone(id),
                    lat = town.lat + dLat,
                    lng = town.lng + dLng,
                    region = town.name
                )
            )
            id++
        }
    }
}

/**
 * The town a location string belongs to, matched against the known areas.
 * "Brgy. Lahug, Cebu City" gives "Cebu City"; a place the app doesn't know
 * gives whatever was written after the first comma.
 */
fun regionForLocation(location: String): String {
    val text = location.trim()
    if (text.isEmpty()) return ""
    CEBU_REGIONS.firstOrNull { text.contains(it.name, ignoreCase = true) }?.let { return it.name }
    return text.substringAfter(',', "").trim().ifEmpty { text }
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
