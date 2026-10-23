package com.example.dugo.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.example.dugo.R
import com.example.dugo.auth.AuthRepository
import com.example.dugo.location.currentLocation
import com.example.dugo.location.hasLocationPermission
import com.example.dugo.login.BLOOD_TYPES
import com.example.dugo.login.Brand
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlin.math.acos
import kotlin.math.ln
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val Green = Color(0xFF2E9E6B)
private const val ALL_TYPES = "All blood types"
private const val ALL_AREAS = "All areas"

private const val TOWN_ZOOM = 12f
private const val AREA_ZOOM = 14f

/** How far the donor search reaches. The map and the settings screen share this. */
const val MAX_RADIUS_KM = 100

/** ID of the signed-in user's own pin, which the sample donors (1-5) never use. */
private const val SELF_DONOR_ID = -1

private fun zoomForRadius(radiusKm: Double): Float =
    (14.6 - ln(radiusKm) / ln(2.0) * 0.95).toFloat()
enum class HomeTab(val label: String, val icon: ImageVector) {
    Map("Map", Icons.Outlined.Place),
    Requests("Requests", Icons.Outlined.DateRange),
    Messages("Messages", Icons.Outlined.MailOutline),
    Profile("Profile", Icons.Outlined.Person)
}

@Composable
fun HomeScreenMap(
    roleLabel: String = "Blood Donor",
    donors: List<Donor> = sampleDonors,
    selectedTab: HomeTab = HomeTab.Map,
    onTabSelected: (HomeTab) -> Unit = {},
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    onMessageDonor: (Donor) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var bloodFilter by remember { mutableStateOf(ALL_TYPES) }
    var areaFilter by remember { mutableStateOf(ALL_AREAS) }
    var dismissed by remember { mutableStateOf(setOf<Int>()) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var radiusKm by remember {
        val saved = AppSettings.prefs(context)
            .getFloat(AppSettings.DEFAULT_RADIUS_KM, AppSettings.DEFAULT_RADIUS_KM_VALUE)
        mutableStateOf(saved.coerceIn(1f, MAX_RADIUS_KM.toFloat()))
    }
    var filtersMinimized by remember { mutableStateOf(false) }
    var sheetMinimized by remember { mutableStateOf(false) }
    var usingMyLocation by remember { mutableStateOf(false) }

    // Real donors who have signed up and saved their position
    var liveDonors by remember { mutableStateOf<List<Donor>>(emptyList()) }

    DisposableEffect(Unit) {
        val registration = DonorRepository.listenDonors(
            onChange = { liveDonors = it },
            onError = { /* the sample donors stay on the map */ }
        )
        onDispose { registration.remove() }
    }

    // The signed-in user's pin. Its position is the device's GPS fix; the saved
    // profile position is used until the first fix arrives.
    val profile = UserSession.profile
    var myGps by remember {
        mutableStateOf(
            profile.latitude?.let { lat ->
                profile.longitude?.let { lng -> lat to lng }
            }
        )
    }
    val isDonor = profile.role == "Blood Donor"

    // Moves the user's own pin and remembers the position for next time
    fun useOwnPosition(gps: Pair<Double, Double>) {
        myGps = gps
        AuthRepository.saveLocation(gps.first, gps.second)
    }

    // Map camera starts on the user's last known position, or the default centre
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(DEFAULT_LAT, DEFAULT_LNG), TOWN_ZOOM)
    }

    // Location permission
    fun locationGranted(): Boolean = hasLocationPermission(context)

    var granted by remember { mutableStateOf(locationGranted()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    val askLocation = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var origin by remember { mutableStateOf(DEFAULT_LAT to DEFAULT_LNG) }

    // Ask for location permission the first time the map opens
    LaunchedEffect(Unit) {
        if (!granted) askLocation()
    }

    // When area filter changes, shift origin to that area's centre
    LaunchedEffect(areaFilter) {
        val area = CEBU_REGIONS.firstOrNull { it.name == areaFilter }
        if (area != null) {
            origin = area.lat to area.lng
            usingMyLocation = false
        }
    }

    // Center the map on the user's current location once permission is granted
    LaunchedEffect(granted) {
        if (granted) {
            val gps = currentLocation(context) ?: return@LaunchedEffect
            origin = gps
            usingMyLocation = true
            useOwnPosition(gps)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(gps.first, gps.second), TOWN_ZOOM)
            )
        }
    }

    // "Use my location" button: fetch a fresh fix and fly the camera there
    val focusMyLocation: () -> Unit = {
        if (!granted) {
            askLocation()
        } else {
            scope.launch {
                val gps = currentLocation(context)
                if (gps != null) {
                    origin = gps
                    usingMyLocation = true
                    useOwnPosition(gps)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(gps.first, gps.second), TOWN_ZOOM)
                    )
                } else {
                    Toast.makeText(context, "Couldn't get your location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // The signed-in donor's pin, shown alongside the sample donors
    val selfPin = myGps?.takeIf { isDonor }?.let { (lat, lng) ->
        Donor(
            id = SELF_DONOR_ID,
            name = profile.fullName,
            bloodType = profile.bloodType,
            availableToday = profile.availableToday,
            area = profile.location.substringBefore(',').trim(),
            phone = profile.contactNumber,
            lat = lat,
            lng = lng,
            isSelf = true,
            region = regionForLocation(profile.location)
        )
    }
    val allDonors = remember(donors, liveDonors, selfPin) {
        val myUid = AuthRepository.currentUser?.uid
        // The signed-in donor has their own pin, so drop their other copy
        val list = donors + liveDonors.filter { it.uid != myUid }
        if (selfPin != null) list + selfPin else list
    }

    val visible = remember(allDonors, query, bloodFilter, areaFilter, dismissed, radiusKm, origin) {
        val type = if (bloodFilter == ALL_TYPES) null else bloodFilter
        findNearestDonors(allDonors, type, origin.first, origin.second, radiusKm.toDouble())
            .map { it.donor }
            .filter { it.id !in dismissed }
            // The profile's location text may not match a barangay name, so the
            // donor's own pin is never hidden by the area filter
            .filter { areaFilter == ALL_AREAS || it.region == areaFilter || it.isSelf }
            .filter {
                query.isBlank() ||
                        it.area.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true)
            }
    }
    val selected = visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull()
    val areaName = when {
        areaFilter != ALL_AREAS -> areaFilter
        usingMyLocation -> "your area"
        else -> "Cebu"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        HomeHeader(roleLabel, usingMyLocation, onNotifications, onProfile)

        MinimizeRow(
            title = "Filter & location",
            minimized = filtersMinimized,
            onToggle = { filtersMinimized = !filtersMinimized },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!filtersMinimized) Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search area or donor name", color = Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Gray) },
                trailingIcon = {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { focusMyLocation() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = "Use my location", tint = Brand)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7F8FA),
                    unfocusedContainerColor = Color(0xFFF7F8FA),
                    focusedBorderColor = Brand,
                    unfocusedBorderColor = Line
                )
            )

            FilterDropdown(
                options = listOf(ALL_TYPES) + BLOOD_TYPES,
                selected = bloodFilter,
                onSelected = { bloodFilter = it; showAll = false }
            )
            FilterDropdown(
                options = listOf(ALL_AREAS) + CEBU_REGIONS.map { it.name },
                selected = areaFilter,
                leadingIcon = Icons.Outlined.LocationOn,
                onSelected = { choice ->
                    areaFilter = choice
                    showAll = false
                    // Fly the map to the chosen area
                    val area = CEBU_REGIONS.firstOrNull { it.name == choice }
                    val update = if (area != null) {
                        CameraUpdateFactory.newLatLngZoom(LatLng(area.lat, area.lng), AREA_ZOOM)
                    } else {
                        CameraUpdateFactory.newLatLngZoom(LatLng(DEFAULT_LAT, DEFAULT_LNG), TOWN_ZOOM)
                    }
                    scope.launch { cameraPositionState.animate(update) }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Radius", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${radiusKm.toInt()} km", color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = radiusKm,
                onValueChange = { radiusKm = it; showAll = false },
                onValueChangeFinished = {
                    // Zoom the map so the whole radius is visible
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(origin.first, origin.second),
                                zoomForRadius(radiusKm.toDouble())
                            )
                        )
                    }
                },
                valueRange = 1f..MAX_RADIUS_KM.toFloat(),
                steps = 19, // 1, 2, 3 ... 100 km
                colors = SliderDefaults.colors(
                    thumbColor = Brand,
                    activeTrackColor = Brand,
                    inactiveTrackColor = Line
                )
            )

            Button(
                onClick = {
                    val nearest = visible.firstOrNull()
                    when {
                        bloodFilter == ALL_TYPES ->
                            Toast.makeText(context, "Pick a blood type first", Toast.LENGTH_SHORT).show()
                        nearest == null ->
                            Toast.makeText(
                                context,
                                "No $bloodFilter donor within ${radiusKm.toInt()} km",
                                Toast.LENGTH_SHORT
                            ).show()
                        else -> {
                            selectedId = nearest.id
                            showAll = false
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(nearest.lat, nearest.lng), AREA_ZOOM)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE1D2B), contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Find nearest ${if (bloodFilter == ALL_TYPES) "donor" else "$bloodFilter donor"}",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }

            if (!granted) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF0F0))
                        .clickable { askLocation() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Gray))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Location permission blocked • tap to try again",
                        color = Brand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Map + bottom sheet
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val sheetMax = maxHeight * 0.62f

            DonorMap(
                donors = visible,
                selectedId = selected?.id,
                onSelect = { donor ->
                    selectedId = donor.id
                    showAll = false
                    sheetMinimized = false
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLng(LatLng(donor.lat, donor.lng))
                        )
                    }
                },
                cameraPositionState = cameraPositionState,
                myLocationEnabled = granted,
                originLat = origin.first,
                originLng = origin.second,
                radiusKm = radiusKm.toDouble(),
                modifier = Modifier.fillMaxSize()
            )

            // Zoom controls
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "+",
                        modifier = Modifier
                            .clickable { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }
                            .padding(vertical = 8.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink
                    )
                    HorizontalDivider(Modifier.width(24.dp), color = Line)
                    Text(
                        "−",
                        modifier = Modifier
                            .clickable { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } }
                            .padding(vertical = 8.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink
                    )
                }
            }

            // Nearby donors sheet
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = sheetMax),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Nearby donors", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(
                                "${visible.size} available in $areaName",
                                fontSize = 12.sp, color = Gray
                            )
                        }
                        if (!sheetMinimized) {
                            Text(
                                if (showAll) "Show less" else "View all",
                                color = Brand,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showAll = !showAll }
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Icon(
                            if (sheetMinimized) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (sheetMinimized) "Expand donor info" else "Minimize donor info",
                            tint = Ink,
                            modifier = Modifier.size(24.dp).clickable { sheetMinimized = !sheetMinimized }
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    if (sheetMinimized) {
                        // collapsed: header only
                    } else if (visible.isEmpty()) {
                        Text(
                            "No donors match your filters.",
                            color = Gray, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        val shown = if (showAll) visible else listOfNotNull(selected)
                        shown.forEach { donor ->
                            DonorCard(
                                donor = donor,
                                distanceKm = donor.distanceKmFrom(origin.first, origin.second),
                                onDismiss = { dismissed = dismissed + donor.id },
                                onContact = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, "tel:${donor.phone}".toUri())
                                        )
                                    }.onFailure {
                                        Toast.makeText(context, "No dialer found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMessage = {
                                    // Sample donors aren't real accounts, so there is
                                    // nobody to message yet
                                    if (donor.uid.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "${donor.name} is a test donor. Sign in with a real account to chat.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        onMessageDonor(donor)
                                    }
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Line)
        AppBottomNav(selectedTab, onTabSelected)
    }
}

@Composable
private fun MinimizeRow(
    title: String,
    minimized: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Icon(
            if (minimized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            contentDescription = if (minimized) "Expand filters" else "Minimize filters",
            tint = Ink
        )
    }
}

@Composable
private fun HomeHeader(
    roleLabel: String,
    usingMyLocation: Boolean,
    onNotifications: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Needs res/drawable/dugo_logo.png
        Image(
            painter = painterResource(R.drawable.dugo_logo),
            contentDescription = "DuGO",
            modifier = Modifier.height(34.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(roleLabel, color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(if (usingMyLocation) "Near your location" else "Cebu, Philippines", color = Gray, fontSize = 12.sp)
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Ink)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Brand)
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F6)).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Gray)
        }
    }
}

@Composable
private fun FilterDropdown(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    leadingIcon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, Line, shape)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(selected, Modifier.weight(1f), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Ink)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

/** Real Google Map centred on the user's position, with a blood-type pin for each donor. */
@Composable
private fun DonorMap(
    donors: List<Donor>,
    selectedId: Int?,
    onSelect: (Donor) -> Unit,
    cameraPositionState: CameraPositionState,
    myLocationEnabled: Boolean,
    originLat: Double,
    originLng: Double,
    radiusKm: Double,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        ),
        // Keeps the Google logo and map center clear of the bottom sheet
        contentPadding = PaddingValues(bottom = 220.dp)
    ) {
        Circle(
            center = LatLng(originLat, originLng),
            radius = radiusKm * 1000.0,
            strokeColor = Brand,
            strokeWidth = 3f,
            fillColor = Brand.copy(alpha = 0.1f)
        )

        donors.forEach { donor ->
            key(donor.id) {
                val isSelected = donor.id == selectedId
                val icon = remember(donor.bloodType, isSelected) {
                    pinIcon(donor.bloodType, isSelected, density)
                }
                Marker(
                    state = rememberMarkerState(
                        key = donor.id.toString(),
                        position = LatLng(donor.lat, donor.lng)
                    ),
                    icon = icon,
                    title = donor.name,
                    zIndex = if (isSelected) 1f else 0f,
                    onClick = {
                        onSelect(donor)
                        true
                    }
                )
            }
        }
    }
}

/** Draws a red teardrop pin with the blood type on it. */
private fun pinIcon(label: String, selected: Boolean, density: Float): BitmapDescriptor {
    val sizePx = ((if (selected) 46 else 38) * density).toInt()
    val tipPx = (sizePx * 0.28f).toInt()
    val bitmap = createBitmap(sizePx, sizePx + tipPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val r = sizePx / 2f
    val border = 2f * density

    fun drawDrop(radius: Float, tipY: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(r, r, radius, paint)
        val d = tipY - r
        val phi = acos((radius / d).toDouble())
        val dx = (radius * sin(phi)).toFloat()
        val dy = (radius * cos(phi)).toFloat()
        val path = Path().apply {
            moveTo(r, tipY)
            lineTo(r - dx, r + dy)
            lineTo(r + dx, r + dy)
            close()
        }
        canvas.drawPath(path, paint)
    }

    val tipY = (sizePx + tipPx).toFloat()
    drawDrop(r, tipY, android.graphics.Color.WHITE)
    drawDrop(
        r - border,
        tipY - border * 1.5f,
        (if (selected) "#F0202F" else "#C4182A").toColorInt()
    )

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (if (selected) 13f else 11f) * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(label, r, r - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun DonorCard(
    donor: Donor,
    distanceKm: Double,
    onDismiss: () -> Unit,
    onContact: () -> Unit,
    onMessage: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Line, shape)
            .background(Color.White, shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFEEF0F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Gray, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (donor.isSelf) "${donor.name} (You)" else donor.name,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .rotate(45f)
                            .background(
                                Brand,
                                RoundedCornerShape(
                                    topStartPercent = 50, topEndPercent = 50,
                                    bottomEndPercent = 0, bottomStartPercent = 50
                                )
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(donor.bloodType, color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "  •  ${"%.1f".format(distanceKm)} km away",
                        color = Gray, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(if (donor.availableToday) Green else Gray)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (donor.availableToday) "Available today" else "Available on request",
                        color = if (donor.availableToday) Green else Gray,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
                if (donor.area.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        donor.area,
                        color = Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Dismiss",
                tint = Gray,
                modifier = Modifier.size(20.dp).clickable(onClick = onDismiss)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onContact,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE1D2B), contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Contact donor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFEFEF))
                    .clickable(onClick = onMessage),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Email, contentDescription = "Message", tint = Brand)
            }
        }
    }
}