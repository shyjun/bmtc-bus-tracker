package com.bmtc.bustracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bmtc.bustracker.ui.theme.*

/**
 * Root screen composable — single-screen BMTC Bus Tracker UI.
 * UI-only: no networking, no business logic, placeholder data only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackerScreen() {
    var busNumber by remember { mutableStateOf("KA57F4864") }
    var alertEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = BackgroundGray,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ─── 1. App Header ───────────────────────────────────────────
            AppHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ─── 2. Bus Number Card ───────────────────────────────────
                BusNumberCard(
                    busNumber = busNumber,
                    onBusNumberChange = { busNumber = it.uppercase() }
                )

                // ─── 3. Tracking Status Card ──────────────────────────────
                TrackingStatusCard()

                // ─── 4. Current Bus Position Card ────────────────────────
                BusPositionCard()

                // ─── 5. Alert Card ────────────────────────────────────────
                TrackingAlertCard(
                    alertEnabled = alertEnabled,
                    onAlertToggle = { alertEnabled = it }
                )

                // ─── 6. Bottom Info Card ──────────────────────────────────
                BottomInfoCard()

                // ─── 7. Footer ────────────────────────────────────────────
                Footer()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 1. APP HEADER
// ══════════════════════════════════════════════════════════════════════

@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BmtcBlueDark, BmtcBlue)
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bus icon circle
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

            Column {
                Text(
                    text = "BMTC Bus Tracker",
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
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 2. BUS NUMBER CARD
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusNumberCard(
    busNumber: String,
    onBusNumberChange: (String) -> Unit
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
                    text = "Enter Bus Number",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = BmtcBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = busNumber,
                    onValueChange = onBusNumberChange,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsBus,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (busNumber.isNotEmpty()) {
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
                    onClick = { /* UI only */ },
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
                        text = "TRACK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

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
                    text = "Enter bus number (e.g., KA57F4864)",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextHint)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// 3. TRACKING STATUS CARD
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TrackingStatusCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TrackingGreenContainer),
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
                // Green check circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TrackingGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Active",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Tracking Active",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TrackingGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "07-Jul-2026 10:50:45",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "(0 min ago)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            // Refresh button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { /* UI only */ },
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
                    text = "REFRESH",
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

// ══════════════════════════════════════════════════════════════════════
// 4. BUS POSITION CARD (Route Visualization)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun BusPositionCard() {
    TrackerCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Bus is currently between",
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
                // Left: vertical line + icons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    // Previous stop pin
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

                    // Dashed line segment top
                    DashedVerticalLine(
                        height = 28.dp,
                        color = PreviousStopGreen.copy(alpha = 0.5f)
                    )

                    // Bus icon
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

                    // Dashed line segment bottom
                    DashedVerticalLine(
                        height = 28.dp,
                        color = NextStopBlue.copy(alpha = 0.5f)
                    )

                    // Next stop pin
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

                // Right: stop labels
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Previous stop
                    StopLabel(
                        label = "PREVIOUS STOP",
                        labelColor = PreviousStopGreen,
                        stopName = "Bengaluru Dairy Circle",
                        direction = "(Towards Madivala)"
                    )

                    // Spacer to align with bus icon + dashed lines
                    Spacer(modifier = Modifier.height(72.dp))

                    // Next stop
                    StopLabel(
                        label = "NEXT STOP",
                        labelColor = NextStopBlue,
                        stopName = "Hosur Road Junction",
                        direction = "(Towards Madivala)"
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
        Text(
            text = stopName,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = direction,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary
            )
        )
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

// ══════════════════════════════════════════════════════════════════════
// 5. TRACKING ALERT CARD
// ══════════════════════════════════════════════════════════════════════

@Composable
fun TrackingAlertCard(
    alertEnabled: Boolean,
    onAlertToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BmtcBlueContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bell icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BmtcBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = BmtcBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Tracking Stop Alert",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = BmtcBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Notify me if tracking has not been updated for more than ",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                )
                Text(
                    text = "10 minutes.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BmtcBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Switch(
                checked = alertEnabled,
                onCheckedChange = onAlertToggle,
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

// ══════════════════════════════════════════════════════════════════════
// 6. BOTTOM INFO CARD (3 columns)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun BottomInfoCard() {
    TrackerCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoColumn(
                icon = Icons.Outlined.AccessTime,
                iconTint = TrackingGreen,
                title = "Last Update",
                value = "10:50:45",
                valueColor = TextPrimary
            )

            // Divider
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = DividerGray,
                thickness = 1.dp
            )

            InfoColumn(
                icon = Icons.Outlined.Sync,
                iconTint = BmtcBlue,
                title = "Status",
                value = "Running",
                valueColor = StatusRunningGreen
            )

            // Divider
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = DividerGray,
                thickness = 1.dp
            )

            InfoColumn(
                icon = Icons.Outlined.NotificationsActive,
                iconTint = AlertRed,
                title = "Alert",
                value = "Enabled",
                valueColor = AlertRed
            )
        }
    }
}

@Composable
fun InfoColumn(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                color = valueColor,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// 7. FOOTER
// ══════════════════════════════════════════════════════════════════════

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
            text = "Data provided by BMTC  •  Updates every 5 minutes",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextHint,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// SHARED: Tracker Card wrapper
// ══════════════════════════════════════════════════════════════════════

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

// ══════════════════════════════════════════════════════════════════════
// PREVIEW
// ══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
fun BusTrackerScreenPreview() {
    com.bmtc.bustracker.ui.theme.BmtcBusTrackerTheme {
        BusTrackerScreen()
    }
}
