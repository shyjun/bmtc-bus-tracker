package com.bmtc.bustracker.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bmtc.bustracker.R
import com.bmtc.bustracker.data.remote.VehicleData
import com.bmtc.bustracker.data.repository.TrackingStatus
import com.bmtc.bustracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackerScreen(
    viewModel: BusTrackerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BusTrackerViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BackgroundGray,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        viewModel.dismissSuggestions()
                    }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AppHeader(
                    onSettingsClick = { viewModel.openSettings() }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    BusNumberCard(
                        busInput = viewModel.busInput,
                        onBusNumberChange = { viewModel.onBusInputChange(it) },
                        isLoading = state.isLoading,
                        isSearching = viewModel.isSearching,
                        searchResults = viewModel.searchResults,
                        showSuggestions = viewModel.showSuggestions,
                        searchError = viewModel.searchError,
                        onTrackClick = { viewModel.onTrackClicked() },
                        onSuggestionSelected = { viewModel.onSuggestionSelected(it) },
                        onDismissSuggestions = { viewModel.dismissSuggestions() }
                    )

                    if (state.error != null) {
                        ErrorCard(error = state.error!!)
                    }

                    if (state.busNumber.isNotBlank() && state.vehicleId > 0) {
                        TrackingStatusCard(
                            status = state.trackingStatus,
                            lastRefreshOn = state.locationDetails?.lastRefreshOn,
                            onRefreshClick = { viewModel.onRefreshClicked() },
                            isLoading = state.isLoading
                        )

                        BusPositionCard(
                            previousStop = state.locationDetails?.previousStop,
                            nextStop = state.locationDetails?.nextStop
                        )

                        SettingsCard(
                            monitoringEnabled = viewModel.monitoringEnabled,
                            notificationsEnabled = viewModel.notificationsEnabled,
                            monitoringInterval = viewModel.monitoringInterval,
                            offlineNotificationInterval = viewModel.offlineNotificationInterval,
                            onMonitoringToggled = { viewModel.onMonitoringToggled(it) },
                            onNotificationsToggled = { viewModel.onNotificationsToggled(it) }
                        )
                    }

                    Footer()

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BmtcBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        if (viewModel.showSettingsDialog) {
            SettingsDialog(
                monitoringSecs = viewModel.dialogMonitoringSecs,
                offlineMins = viewModel.dialogOfflineMins,
                onMonitoringSecsDecrement = { viewModel.onMonitoringSecsDecrement() },
                onMonitoringSecsIncrement = { viewModel.onMonitoringSecsIncrement() },
                onOfflineMinsDecrement = { viewModel.onOfflineMinsDecrement() },
                onOfflineMinsIncrement = { viewModel.onOfflineMinsIncrement() },
                onDismiss = { viewModel.onSettingsDismiss() },
                onConfirm = { viewModel.onSettingsConfirm() }
            )
        }
    }
}

@Composable
fun AppHeader(
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(BmtcBlueDark, BmtcBlue)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus,
                    contentDescription = "Bus",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "with Offline Notification",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF69F0AE))
                    )
                    Text(
                        text = "Live Tracking & Alerts",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.90f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusNumberCard(
    busInput: String,
    onBusNumberChange: (String) -> Unit,
    isLoading: Boolean,
    isSearching: Boolean,
    searchResults: List<VehicleData>,
    showSuggestions: Boolean,
    searchError: String?,
    onTrackClick: () -> Unit,
    onSuggestionSelected: (VehicleData) -> Unit,
    onDismissSuggestions: () -> Unit
) {
    TrackerCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BmtcBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.enter_bus_number),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = BmtcBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = busInput,
                        onValueChange = onBusNumberChange,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSearching) Icons.Filled.Sync else Icons.Outlined.DirectionsBus,
                                contentDescription = null,
                                tint = if (isSearching) BmtcBlue else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (busInput.isNotEmpty()) {
                                IconButton(onClick = { onBusNumberChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "KA57F4864",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextHint)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Text
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BmtcBlue,
                            unfocusedBorderColor = DividerGray,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = BmtcBlue,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Button(
                        onClick = onTrackClick,
                        enabled = !isLoading && busInput.isNotBlank(),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 88.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BmtcBlue,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.button_track),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSuggestions,
                    onDismissRequest = onDismissSuggestions,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .background(
                            color = CardSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    searchResults.forEach { vehicle ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = vehicle.vehicleRegNo,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            },
                            onClick = { onSuggestionSelected(vehicle) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DirectionsBus,
                                    contentDescription = null,
                                    tint = BmtcBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (searchError != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = searchError,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AlertRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.enter_bus_number_hint),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextHint)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorCard(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun TrackingStatusCard(
    status: TrackingStatus,
    lastRefreshOn: String?,
    onRefreshClick: () -> Unit,
    isLoading: Boolean
) {
    val cardBackground by animateColorAsState(
        targetValue = when (status) {
            TrackingStatus.ACTIVE -> TrackingGreenContainer
            TrackingStatus.OFFLINE -> Color(0xFFFFEBEE)
            TrackingStatus.NO_INTERNET -> Color(0xFFF5F5F5)
        },
        label = "status_card_bg"
    )

    val statusColor = when (status) {
        TrackingStatus.ACTIVE -> TrackingGreen
        TrackingStatus.OFFLINE -> AlertRed
        TrackingStatus.NO_INTERNET -> TextSecondary
    }

    val iconBg = when (status) {
        TrackingStatus.ACTIVE -> TrackingGreen
        TrackingStatus.OFFLINE -> AlertRed
        TrackingStatus.NO_INTERNET -> TextHint
    }

    val statusIcon = when (status) {
        TrackingStatus.ACTIVE -> Icons.Filled.Check
        TrackingStatus.OFFLINE -> Icons.Filled.Warning
        TrackingStatus.NO_INTERNET -> Icons.Filled.WifiOff
    }

    val statusText = when (status) {
        TrackingStatus.ACTIVE -> stringResource(R.string.tracking_active)
        TrackingStatus.OFFLINE -> stringResource(R.string.tracking_offline)
        TrackingStatus.NO_INTERNET -> stringResource(R.string.no_internet)
    }

    val formattedLocalDateStr = lastRefreshOn ?: "N/A"

    val timeAgoStr = remember(lastRefreshOn) {
        if (lastRefreshOn == null) return@remember ""
        try {
            val istZone = TimeZone.getTimeZone("Asia/Kolkata")
            val timeRegex = Regex("""(\d{1,2}):(\d{2}):(\d{2})""")
            val m = timeRegex.find(lastRefreshOn)
            if (m != null) {
                val hour = m.groupValues[1].toIntOrNull() ?: return@remember ""
                val min  = m.groupValues[2].toIntOrNull() ?: return@remember ""
                val sec  = m.groupValues[3].toIntOrNull() ?: return@remember ""
                val cal = java.util.Calendar.getInstance(istZone)
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, min)
                cal.set(java.util.Calendar.SECOND, sec)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                if (cal.timeInMillis > System.currentTimeMillis() + 60 * 60 * 1000L) {
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                }
                val diffMs = System.currentTimeMillis() - cal.timeInMillis
                val diffMins = diffMs / (1000 * 60)
                if (diffMins <= 0L) "(just now)" else "($diffMins min ago)"
            } else ""
        } catch (_: Exception) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AnimatedContent(
                        targetState = statusText,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "status_text"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Text(
                        text = stringResource(R.string.last_updated),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    AnimatedContent(
                        targetState = formattedLocalDateStr,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "local_date_str"
                    ) { targetDate ->
                        Text(
                            text = targetDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    AnimatedContent(
                        targetState = timeAgoStr,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "time_ago_str"
                    ) { targetAgo ->
                        if (targetAgo.isNotEmpty()) {
                            Text(
                                text = targetAgo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isLoading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = BmtcBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.button_refresh),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BmtcBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun BusPositionCard(
    previousStop: String?,
    nextStop: String?
) {
    TrackerCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.bus_is_between),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PreviousStopGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Previous stop",
                            tint = PreviousStopGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DashedVerticalLine(
                        height = 28.dp,
                        color = PreviousStopGreen.copy(alpha = 0.5f)
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BmtcBlue.copy(alpha = 0.10f))
                            .border(1.5.dp, BmtcBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsBus,
                            contentDescription = "Bus",
                            tint = BmtcBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DashedVerticalLine(
                        height = 28.dp,
                        color = NextStopBlue.copy(alpha = 0.5f)
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NextStopBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Next stop",
                            tint = NextStopBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    StopLabel(
                        label = stringResource(R.string.previous_stop),
                        labelColor = PreviousStopGreen,
                        stopName = previousStop ?: "---",
                        direction = ""
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    StopLabel(
                        label = stringResource(R.string.next_stop),
                        labelColor = NextStopBlue,
                        stopName = nextStop ?: "---",
                        direction = ""
                    )
                }
            }
        }
    }
}

@Composable
fun StopLabel(
    label: String,
    labelColor: Color,
    stopName: String,
    direction: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = labelColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontSize = 10.sp
            )
        )
        AnimatedContent(
            targetState = stopName,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "stop_name"
        ) { targetName ->
            Text(
                text = targetName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        if (direction.isNotEmpty()) {
            Text(
                text = direction,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                )
            )
        }
    }
}

@Composable
fun DashedVerticalLine(
    height: Dp,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .height(height)
    ) {
        val dashHeight = 6.dp.toPx()
        val gapHeight = 4.dp.toPx()
        var y = 0f
        while (y < this.size.height) {
            drawRect(
                color = color,
                topLeft = Offset(0f, y),
                size = Size(this.size.width, minOf(dashHeight, this.size.height - y))
            )
            y += dashHeight + gapHeight
        }
    }
}

@Composable
fun SettingsCard(
    monitoringEnabled: Boolean,
    notificationsEnabled: Boolean,
    monitoringInterval: Int,
    offlineNotificationInterval: Int,
    onMonitoringToggled: (Boolean) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit
) {
    val monitoringTint = if (monitoringEnabled) TrackingGreen else TextHint
    val monitoringBg = if (monitoringEnabled) TrackingGreen.copy(alpha = 0.12f) else DividerGray
    val notificationTint = if (notificationsEnabled) AlertRed else TextHint
    val notificationBg = if (notificationsEnabled) AlertRed.copy(alpha = 0.12f) else DividerGray

    TrackerCard {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(monitoringBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Monitoring",
                        tint = monitoringTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Monitoring",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (monitoringEnabled) stringResource(R.string.monitoring_enabled) else stringResource(R.string.monitoring_disabled),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (monitoringEnabled) TrackingGreen else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Checking every $monitoringInterval seconds",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Switch(
                    checked = monitoringEnabled,
                    onCheckedChange = onMonitoringToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BmtcBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DividerGray
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = DividerGray,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(notificationBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = notificationTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (notificationsEnabled) stringResource(R.string.notifications_enabled) else stringResource(R.string.notification_disabled),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (notificationsEnabled) TrackingGreen else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Alert after $offlineNotificationInterval minutes without updates",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsToggled,
                    enabled = monitoringEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BmtcBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DividerGray
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    monitoringSecs: Int,
    offlineMins: Int,
    onMonitoringSecsDecrement: () -> Unit,
    onMonitoringSecsIncrement: () -> Unit,
    onOfflineMinsDecrement: () -> Unit,
    onOfflineMinsIncrement: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "System Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Monitoring Interval (secs)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "How often the application polls the BMTC server for updated vehicle location.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    StepperControl(
                        value = monitoringSecs,
                        onDecrement = onMonitoringSecsDecrement,
                        onIncrement = onMonitoringSecsIncrement
                    )
                }

                HorizontalDivider(color = DividerGray)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Offline Notification Interval (mins)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Notify me if BMTC has not updated the vehicle location for this duration.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    StepperControl(
                        value = offlineMins,
                        onDecrement = onOfflineMinsDecrement,
                        onIncrement = onOfflineMinsIncrement
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun StepperControl(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onDecrement,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = BmtcBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Decrease",
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = 100.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        FilledIconButton(
            onClick = onIncrement,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = BmtcBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Increase",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun Footer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.data_source_footer),
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextHint,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrackerCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
fun BusTrackerScreenPreview() {
    BmtcBusTrackerTheme {
        BusTrackerScreen()
    }
}