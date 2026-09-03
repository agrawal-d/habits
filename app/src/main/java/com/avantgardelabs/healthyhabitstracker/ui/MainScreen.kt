package com.avantgardelabs.healthyhabitstracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.LogEntry

enum class Tab {
    HOME, LOG, SETTINGS, REMINDER
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
fun NotificationBlockerScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Required",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Notification Permission Required",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Healthy Habits Tracker relies on structured daily reminders so you never forget to log your habits. Enable notification permissions to continue.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A),
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "GRANT PERMISSION",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "OPEN APP SETTINGS",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dataManager: DataManager,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(Tab.HOME) }
    var editingDate by remember { mutableStateOf<String?>(null) }
    var deleteConfirmDate by remember { mutableStateOf<String?>(null) }

    // Intercept back actions
    BackHandler(enabled = editingDate != null) {
        editingDate = null
    }

    BackHandler(enabled = editingDate == null && currentTab != Tab.HOME) {
        currentTab = if (currentTab == Tab.REMINDER) Tab.SETTINGS else Tab.HOME
    }

    // If editingDate is set, show the questionnaire screen
    if (editingDate != null) {
        val targetDate = editingDate!!
        val existingLog = dataManager.habitData.logs.firstOrNull { it.date == targetDate }
        val questionsToLog = existingLog?.questions ?: dataManager.habitData.questions
        val existingAnswers = existingLog?.answers
        val existingNote = existingLog?.note

        AnswerQuestionScreen(
            dateStr = targetDate,
            questions = questionsToLog,
            existingAnswers = existingAnswers,
            existingNote = existingNote,
            onSave = { answers, note ->
                val newEntry = LogEntry(
                    date = targetDate,
                    questions = questionsToLog,
                    answers = answers,
                    note = note
                )
                dataManager.saveLogEntry(newEntry)
                editingDate = null
                Toast.makeText(context, "Log saved", Toast.LENGTH_SHORT).show()
            },
            onCancel = {
                editingDate = null
            }
        )
        return
    }

    // Delete confirmation dialog
    if (deleteConfirmDate != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmDate = null },
            shape = RoundedCornerShape(4.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Text(
                    "Delete Entry?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete the log for $deleteConfirmDate?",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmDate?.let { dataManager.deleteLogEntry(it) }
                        deleteConfirmDate = null
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("DELETE", color = Color(0xFFD32F2F), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmDate = null }) {
                    Text("CANCEL", color = Color(0xFF1976D2), fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Main layout
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentTab != Tab.HOME) {
                            IconButton(onClick = {
                                currentTab = if (currentTab == Tab.REMINDER) Tab.SETTINGS else Tab.HOME
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = when (currentTab) {
                                Tab.HOME -> "Habits"
                                Tab.LOG -> "History"
                                Tab.SETTINGS -> "Settings"
                                Tab.REMINDER -> "Daily Reminder"
                            },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        )
                    }

                    if (currentTab == Tab.HOME) {
                        IconButton(onClick = { currentTab = Tab.SETTINGS }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                Tab.HOME -> {
                    if (!notificationsEnabled) {
                        NotificationBlockerScreen(
                            onRequestPermission = onRequestNotificationPermission,
                            onOpenSettings = {
                                openAppSettings(context)
                            }
                        )
                    } else {
                        HomeScreen(
                            dataManager = dataManager,
                            onAnswerDate = { dateStr -> editingDate = dateStr },
                            onNavigateToLogs = { currentTab = Tab.LOG }
                        )
                    }
                }
                Tab.LOG -> {
                    LogScreen(
                        dataManager = dataManager,
                        onEditEntry = { dateStr -> editingDate = dateStr },
                        onDeleteEntry = { dateStr -> deleteConfirmDate = dateStr }
                    )
                }
                Tab.SETTINGS -> {
                    SettingsScreen(
                        dataManager = dataManager,
                        notificationsEnabled = notificationsEnabled,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSettings = { openAppSettings(context) },
                        onOpenReminderSettings = { currentTab = Tab.REMINDER },
                        onNavigateHome = { currentTab = Tab.HOME }
                    )
                }
                Tab.REMINDER -> {
                    ReminderSettingsScreen(
                        dataManager = dataManager,
                        notificationsEnabled = notificationsEnabled,
                        onOpenSettings = { openAppSettings(context) }
                    )
                }
            }
        }
    }
}
