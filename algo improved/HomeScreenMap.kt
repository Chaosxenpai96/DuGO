package com.example.dugo.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.example.dugo.R
import com.example.dugo.login.BLOOD_TYPES
import com.example.dugo.login.Brand
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1B2230)
private val Gray = Color(0xFF8A8F99)
private val Line = Color(0xFFE7E9EE)
private val Green = Color(0xFF2E9E6B)
private const val ALL_TYPES = "All blood types"
private const val ALL_AREAS = "All Dalaguete"

private const val TOWN_ZOOM = 12f
private const val AREA_ZOOM = 14f

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
    onMessageDonor: (Donor) -> Unit = {},
    // [MAP] ADDED: blood type to show first (the seeker's needed type), null = no priority
    priorityBloodType: String? = null,
    // [MAP] ADDED: starting distance filter (from Profile > Location settings) + callback when changed
    searchRadiusKm: Int = DEFAULT_RADIUS_KM,
    onRadiusChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var bloodFilter by remember { mutableStateOf(ALL_TYPES) }
    var areaFilter by remember { mutableStateOf(ALL_AREAS) }
    var dismissed by remember { mutableStateOf(setOf<Int>()) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var showAll by remember { mutableStateOf(false) }
    // [MAP] ADDED: distance filter + the seeker's real position
    var radiusKm by remember(searchRadiusKm) { mutableStateOf(searchRadiusKm) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Map camera starts focused on Dalaguete
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(DALAGUETE_LAT, DALAGUETE_LNG), TOWN_ZOOM)
    }

    // Location permission
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasLocationPermission()) }
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

    // [MAP] ADDED: get the phone's position once permission is granted
    LaunchedEffect(granted) {
        if (granted) {
            fetchUserLocation(context) { loc -> userLocation = LatLng(loc.latitude, loc.longitude) }
        }
    }

    // [MAP] ADDED: distances are measured from the seeker; falls back to Dalaguete center
    val originLat = userLocation?.latitude ?: DALAGUETE_LAT
    val originLng = userLocation?.longitude ?: DALAGUETE_LNG

    // [MAP] CHANGED: filters + sorting
    val visible = remember(
        donors, query, bloodFilter, areaFilter, dismissed,
        radiusKm, originLat, originLng, priorityBloodType
    ) {
        donors
            .filter { it.id !in dismissed }
            // hide donors who turned off "Show me on the donor map" or are marked unavailable
            .filter { it.showOnMap && it.availableToDonate }
            // blood type filter = exact match only
            .filter { bloodFilter == ALL_TYPES || it.bloodType == bloodFilter }
            .filter { areaFilter == ALL_AREAS || it.area == areaFilter }
            .filter {
                query.isBlank() ||
                        it.area.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true)
            }
            // only donors inside the chosen kilometer radius
            .filter { it.distanceKmFrom(originLat, originLng) <= radiusKm }
            // 1st: donors with the blood type the seeker needs, 2nd: nearest first
            .sortedWith(
                compareBy<Donor> { if (priorityBloodType != null && it.bloodType == priorityBloodType) 0 else 1 }
                    .thenBy { it.distanceKmFrom(originLat, originLng) }
            )
    }
    val selected = visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull()
    val areaName = if (areaFilter == ALL_AREAS) "Dalaguete" else areaFilter

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        HomeHeader(roleLabel, onNotifications, onProfile)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Search location in Dalaguete", color = Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Gray) },
                trailingIcon = {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { askLocation() },
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

            // [MAP] CHANGED: blood type + distance side by side
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterDropdown(
                    options = listOf(ALL_TYPES) + BLOOD_TYPES,
                    selected = bloodFilter,
                    onSelected = { bloodFilter = it; showAll = false },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    options = RADIUS_OPTIONS_KM.map { "Within $it km" },
                    selected = "Within $radiusKm km",
                    onSelected = { choice ->
                        val km = choice.removePrefix("Within ").removeSuffix(" km").toIntOrNull() ?: radiusKm
                        radiusKm = km
                        showAll = false
                        onRadiusChange(km)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            FilterDropdown(
                options = listOf(ALL_AREAS) + DALAGUETE_AREAS.map { it.name },
                selected = areaFilter,
                leadingIcon = Icons.Outlined.LocationOn,
                onSelected = { choice ->
                    areaFilter = choice
                    showAll = false
                    // Fly the map to the chosen area
                    val area = DALAGUETE_AREAS.firstOrNull { it.name == choice }
                    val update = if (area != null) {
                        CameraUpdateFactory.newLatLngZoom(LatLng(area.lat, area.lng), AREA_ZOOM)
                    } else {
                        CameraUpdateFactory.newLatLngZoom(LatLng(DALAGUETE_LAT, DALAGUETE_LNG), TOWN_ZOOM)
                    }
                    scope.launch { cameraPositionState.animate(update) }
                }
            )

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
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLng(LatLng(donor.lat, donor.lng))
                        )
                    }
                },
                cameraPositionState = cameraPositionState,
                myLocationEnabled = granted,
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
                                // [MAP] CHANGED: shows the distance filter
                                "${visible.size} available within $radiusKm km of " +
                                        (if (userLocation != null) "you" else "$areaName center"),
                                fontSize = 12.sp, color = Gray
                            )
                        }
                        Text(
                            if (showAll) "Show less" else "View all",
                            color = Brand,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showAll = !showAll }
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    if (visible.isEmpty()) {
                        Text(
                            // [MAP] CHANGED
                            "No donors match your filters within $radiusKm km. Try a larger distance.",
                            color = Gray, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        val shown = if (showAll) visible else listOfNotNull(selected)
                        shown.forEach { donor ->
                            DonorCard(
                                donor = donor,
                                distanceKm = donor.distanceKmFrom(originLat, originLng),  // [MAP] CHANGED: from the seeker
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
                                onMessage = { onMessageDonor(donor) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Line)
        BottomNav(selectedTab, onTabSelected)
    }
}

// [PROFILE] CHANGED: "private" -> "internal" and added the `location` parameter (last, with a default)
// so the Profile screen can reuse this same header.
@Composable
internal fun HomeHeader(
    roleLabel: String,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    location: String = "Dalaguete, Cebu"
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
            Text(location, color = Gray, fontSize = 12.sp)  // [PROFILE] was hard-coded "Dalaguete, Cebu"
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
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier   // [MAP] ADDED so two dropdowns can sit side by side
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(modifier.fillMaxWidth()) {
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

/** Real Google Map focused on Dalaguete, with a blood-type pin for each donor. */
@Composable
private fun DonorMap(
    donors: List<Donor>,
    selectedId: Int?,
    onSelect: (Donor) -> Unit,
    cameraPositionState: CameraPositionState,
    myLocationEnabled: Boolean,
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
                Text(donor.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
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

// [PROFILE] CHANGED: "private" -> "internal" so the Profile screen can reuse the bottom tabs.
@Composable
internal fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (active) Brand else Gray)
                Text(
                    tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Brand else Gray
                )
            }
        }
    }
}