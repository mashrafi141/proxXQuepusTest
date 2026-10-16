package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FloatingBottomBar
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val rootEntity by viewModel.rootEntity.collectAsState()
    val isLockEnabled by viewModel.securityManager.isLockEnabled.collectAsState(initial = false)
    val hasPinSet by viewModel.securityManager.hasPinSet().collectAsState(initial = false)

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = CyanPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            FloatingBottomBar(
                currentScreen = Screen.Settings,
                onNavigate = { viewModel.setScreen(it) }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Storage Section
            SettingsSectionHeader(title = "STORAGE CONFIGURATION", icon = Icons.Default.Storage)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Root Folder", color = TextSecondary, fontSize = 12.sp)
                Text(
                    text = rootEntity?.rootName ?: "None selected",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            rootEntity?.let {
                                viewModel.startBackgroundScan(Uri.parse(it.treeUriString))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rebuild Index", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.repository.thumbnailEngine.clearCache()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cached, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Cache", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (rootEntity != null) {
                    TextButton(
                        onClick = { showDisconnectDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Disconnect Root", color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            }

            // Scrap Profiles entry point
            SettingsSectionHeader(title = "SCRAP PROFILES", icon = Icons.Default.Link)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurfaceVariant).border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Manage creator profile links", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Add one or multiple public profile URLs and manage saved profile cards.", color = TextSecondary, fontSize = 12.sp)
                Button(onClick = { viewModel.setScreen(Screen.ScrapProfiles()) }, colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage / Add Profiles", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Security Section
            SettingsSectionHeader(title = "SECURITY & PRIVACY", icon = Icons.Default.Security)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("App PIN Lock", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Require PIN to open app", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { enable ->
                            if (enable && !hasPinSet) {
                                showPinDialog = true
                            } else {
                                coroutineScope.launch {
                                    viewModel.securityManager.setLockEnabled(enable)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPinDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (hasPinSet) "Change PIN" else "Set PIN", color = CyanPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }

                if (isLockEnabled) {
                    Button(
                        onClick = { viewModel.lockApp() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x337C4DFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lock Screen Now", color = VioletSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // About Section
            SettingsSectionHeader(title = "APPLICATION INFO", icon = Icons.Default.Info)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("ProxXQuepus", color = CyanPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Version 2.0.0 (Native Release)", color = TextPrimary, fontSize = 13.sp)
                Text(
                    "Zero-modification local media browser. Never renames, moves or modifies your original media files.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Set / Change PIN Dialog
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    enteredPin = ""
                    pinError = null
                },
                title = { Text("Set 4-Digit PIN", color = TextPrimary) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    enteredPin = it
                                    pinError = null
                                }
                            },
                            label = { Text("Enter PIN") },
                            isError = pinError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(pinError!!, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredPin.length >= 4) {
                                coroutineScope.launch {
                                    viewModel.securityManager.setPin(enteredPin)
                                    showPinDialog = false
                                    enteredPin = ""
                                }
                            } else {
                                pinError = "PIN must be at least 4 digits"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Save PIN", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinDialog = false
                        enteredPin = ""
                    }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }

        // Disconnect Root Dialog
        if (showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showDisconnectDialog = false },
                title = { Text("Disconnect Root Folder", color = TextPrimary) },
                text = { Text("This will clear the indexed database and disconnect the current root. Your actual media files remain untouched.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDisconnectDialog = false
                            coroutineScope.launch {
                                viewModel.repository.clearRoot()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Disconnect", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisconnectDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
