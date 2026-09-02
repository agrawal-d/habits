package com.avantgardelabs.healthyhabitstracker.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.BuildConfig
import com.avantgardelabs.healthyhabitstracker.EditQuestionsActivity
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import java.time.format.DateTimeFormatter

// 4. Settings Screen implementation
@Composable
fun SettingItemRow(
    title: String,
    state: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (!state.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// 4. Settings Screen implementation
@Composable
fun SettingsScreen(
    dataManager: DataManager,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current

    // Dialog visibility states
    var showBackupDialog by remember { mutableStateOf(false) }
    var resetConfirmShow by remember { mutableStateOf(false) }

    // Reminder States
    val reminder = dataManager.habitData.reminder
    val isEnabled = reminder.enabled

    // Launcher for file import
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val success = dataManager.importData(json)
                    if (success) {
                        showBackupDialog = false
                        onNavigateHome()
                        Toast.makeText(context, "Backup imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "ERROR: Invalid JSON structure", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Launcher for file export (backup)
    val fileCreatorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val data = dataManager.exportData()
                    outputStream.write(data.toByteArray())
                }
                showBackupDialog = false
                Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 2. Backup & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            shape = RoundedCornerShape(4.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = "Backup & Restore",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Export your habit data and logs to a JSON file or restore from an existing backup.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = Color(0xFF546E7A),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = {
                            val timeStamp = java.time.LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            fileCreatorLauncher.launch("healthy_habits_backup_$timeStamp.json")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("EXPORT BACKUP", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("RESTORE BACKUP", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("CLOSE", color = Color(0xFF1976D2), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        )
    }

    // 4. Reset Confirmation Dialog
    if (resetConfirmShow) {
        AlertDialog(
            onDismissRequest = { resetConfirmShow = false },
            shape = RoundedCornerShape(4.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Text(
                    "Reset App Data?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Text(
                    "This will completely erase all your logs, habits configuration, and settings. This action cannot be undone.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dataManager.clearAllData()
                        resetConfirmShow = false
                        Toast.makeText(context, "App cleared successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("RESET ALL DATA", color = Color(0xFFD32F2F), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmShow = false }) {
                    Text("CANCEL", color = Color(0xFF1976D2), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        )
    }

    // Main Settings List Layout
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Main Settings Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Habit Questions
                    SettingItemRow(
                        title = "Habit Questions",
                        state = "${dataManager.habitData.questions.size} total",
                        onClick = {
                            val intent = Intent(context, EditQuestionsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )

                    // 2. Daily Reminder
                    val amPm = if (reminder.hour >= 12) "PM" else "AM"
                    val displayHour = when {
                        reminder.hour == 0 -> 12
                        reminder.hour > 12 -> reminder.hour - 12
                        else -> reminder.hour
                    }
                    val displayMinute = String.format("%02d", reminder.minute)
                    val reminderTimeStr = "$displayHour:$displayMinute $amPm"
                    SettingItemRow(
                        title = "Daily Reminder",
                        state = if (isEnabled) "$reminderTimeStr • Enabled" else "$reminderTimeStr • Disabled",
                        onClick = onOpenReminderSettings
                    )

                    // 3. Backup & Restore
                    SettingItemRow(
                        title = "Backup & Restore",
                        onClick = { showBackupDialog = true }
                    )

                    // 4. Reset App Data
                    SettingItemRow(
                        title = "Reset App Data",
                        onClick = { resetConfirmShow = true },
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Info & Links Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. License
                    SettingItemRow(
                        title = "License",
                        onClick = {
                            val intent = Intent(context, com.avantgardelabs.healthyhabitstracker.LicenseActivity::class.java)
                            context.startActivity(intent)
                        }
                    )

                    // 2. Report a Bug
                    SettingItemRow(
                        title = "Report a Bug",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/agrawal-d/habits/issues".toUri())
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open issues page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // 3. Rate App
                    SettingItemRow(
                        title = "Rate App",
                        onClick = {
                            val activity = RatingHelper.findActivity(context)
                            if (activity != null) {
                                RatingHelper.launchReviewFlow(activity, fallbackToStoreOnFailure = true)
                            } else {
                                RatingHelper.openStorePage(context)
                            }
                        }
                    )

                    // 4. Source Code
                    SettingItemRow(
                        title = "Source Code",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/agrawal-d/habits".toUri())
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open GitHub page", Toast.LENGTH_SHORT).show()
                            }
                        },
                        showDivider = false
                    )
                }
            }
        }

        // Footer Credit
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Healthy Habits Tracker • Divyanshu Agrawal",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Icons by flaticon.com",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.flaticon.com/packs/essential-collection".toUri()
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        }
                    }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BuildConfig.GIT_COMMIT} • ${BuildConfig.BUILD_DATE}",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = Color.Gray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
