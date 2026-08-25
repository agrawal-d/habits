package com.avantgardelabs.healthyhabitstracker.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.EditQuestionsActivity
import com.avantgardelabs.healthyhabitstracker.data.AnswerType
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.LogEntry
import com.avantgardelabs.healthyhabitstracker.data.ReminderScheduler
import com.avantgardelabs.healthyhabitstracker.data.ReminderSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun StatusIcon(
    pct: Double,
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    val icon = when {
        pct >= 100.0 -> Icons.Default.EmojiEvents
        pct >= 50.0 -> Icons.Default.SentimentNeutral
        else -> Icons.Default.SentimentDissatisfied
    }
    val defaultColor = when {
        pct >= 100.0 -> Color(0xFF2E7D32) // Green
        pct >= 50.0 -> Color(0xFFF57F17)  // Yellow/Orange
        else -> Color(0xFF5D4037)         // Brown
    }
    Icon(
        imageVector = icon,
        contentDescription = "Status",
        tint = tintColor ?: defaultColor,
        modifier = modifier
    )
}

enum class Tab {
    HOME, LOG, SETTINGS
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
        currentTab = Tab.HOME
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
            title = {
                Text(
                    "Delete Entry?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete the log for ${deleteConfirmDate}?",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp
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
                    Text("Delete", color = Color.Gray, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmDate = null }) {
                    Text("Cancel", color = Color.Gray, fontFamily = FontFamily.SansSerif)
                }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Main layout
    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItemWithIcon(
                        label = "Home",
                        icon = Icons.Default.Home,
                        isSelected = currentTab == Tab.HOME,
                        onClick = { currentTab = Tab.HOME }
                    )
                    TabItemWithIcon(
                        label = "History",
                        icon = Icons.Default.List,
                        isSelected = currentTab == Tab.LOG,
                        onClick = { currentTab = Tab.LOG }
                    )
                    TabItemWithIcon(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = currentTab == Tab.SETTINGS,
                        onClick = { currentTab = Tab.SETTINGS }
                    )
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
                            onAnswerDate = { dateStr -> editingDate = dateStr }
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
                        onOpenSettings = { openAppSettings(context) }
                    )
                }
            }
        }
    }
}

@Composable
fun TabItemWithIcon(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            )
        )
    }
}

@Composable
fun NotificationBlockerScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Notification Permission Required",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Habit tracking relies on structured daily reminders. To use this app, enable notification permissions.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        "Grant Permission",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        "Open Settings",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// 2. Home Screen implementation
@Composable
fun HomeScreen(
    dataManager: DataManager,
    onAnswerDate: (String) -> Unit
) {
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val yesterday = today.minusDays(1)
    val yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val rollingDays = remember(dataManager.habitData.logs) {
        (0..6).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val log = dataManager.habitData.logs.firstOrNull { it.date == dateStr }
            val dayName = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.US)
            val dayNum = date.dayOfMonth.toString()
            Triple(dayName, dayNum, log)
        }.reversed()
    }

    val todayLog = dataManager.habitData.logs.firstOrNull { it.date == todayStr }
    val yesterdayLog = dataManager.habitData.logs.firstOrNull { it.date == yesterdayStr }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // App header title
        Text(
            text = "HABIT TRACKER",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Rolling 7-day card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Last 7 days",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rollingDays.forEach { (dayName, dayNum, log) ->
                    val scoreColor = when {
                        log == null -> Color(0xFFE0E0E0)
                        else -> {
                            val pct = log.getScorePercentage()
                            when {
                                pct >= 100.0 -> Color(0xFF2E7D32)
                                pct >= 50.0 -> Color(0xFFF57F17)
                                else -> Color(0xFF5D4037)
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dayName,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .background(scoreColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (log != null) {
                                StatusIcon(
                                    pct = log.getScorePercentage(),
                                    modifier = Modifier.size(16.dp),
                                    tintColor = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayNum,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's status box (above yesterday)
        Text(
            text = "Today's Status",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAnswerDate(todayStr) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todayLog != null) {
                        StatusIcon(
                            pct = todayLog.getScorePercentage(),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Pending",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Previous Day (Yesterday) score box (below today)
        Text(
            text = "Yesterday's Status",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAnswerDate(yesterdayStr) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = yesterday.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (yesterdayLog != null) {
                        StatusIcon(
                            pct = yesterdayLog.getScorePercentage(),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Missing",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// 3. History Screen implementation
@Composable
fun LogScreen(
    dataManager: DataManager,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val logs = dataManager.habitData.logs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "History",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history yet",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { entry ->
                    LogHistoryItem(
                        entry = entry,
                        onEdit = { onEditEntry(entry.date) },
                        onDelete = { onDeleteEntry(entry.date) }
                    )
                }
            }
        }
    }
}

@Composable
fun LogHistoryItem(
    entry: LogEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedDate = remember(entry.date) {
        try {
            val date = LocalDate.parse(entry.date)
            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        } catch (e: Exception) {
            entry.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                StatusIcon(
                    pct = entry.getScorePercentage(),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    entry.questions.forEach { question ->
                        val answer = entry.answers[question.id] ?: AnswerType.NO
                        val ansText = when (answer) {
                            AnswerType.YES -> "Yes"
                            AnswerType.PARTIAL -> "Partially"
                            AnswerType.NO -> "No"
                        }
                        val ansColor = when (answer) {
                            AnswerType.YES -> Color(0xFF2E7D32)
                            AnswerType.PARTIAL -> Color(0xFFF57F17)
                            AnswerType.NO -> Color(0xFF5D4037)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconByName(question.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = question.text,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            }
                            Text(
                                text = ansText,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ansColor
                                )
                            )
                        }
                    }

                    if (entry.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes: ${entry.note}",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. Settings Screen implementation
@Composable
fun SettingsScreen(
    dataManager: DataManager,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var resetConfirmShow by remember { mutableStateOf(false) }

    // Reminder States
    val reminder = dataManager.habitData.reminder
    var isEnabled by remember(reminder) { mutableStateOf(reminder.enabled) }

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
                Toast.makeText(context, "Backup saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Reset app confirmation dialog
    if (resetConfirmShow) {
        AlertDialog(
            onDismissRequest = { resetConfirmShow = false },
            title = {
                Text(
                    "Reset App Data?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    "This will completely erase all your logs, habits configuration, and settings. This action cannot be undone.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp
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
                    Text("YES, RESET EVERYTHING", color = Color.Red, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmShow = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.SansSerif)
                }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Section: Configure Active Habits / Questions (Separate Activity trigger)
        item {
            Text(
                text = "Habit Questions",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val intent = Intent(context, EditQuestionsActivity::class.java)
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Questions",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Section: Daily Reminder Settings (Cleaned, no colons/numbers)
        item {
            Text(
                text = "Daily Reminder",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val amPm = if (reminder.hour >= 12) "PM" else "AM"
                        val displayHour = when {
                            reminder.hour == 0 -> 12
                            reminder.hour > 12 -> reminder.hour - 12
                            else -> reminder.hour
                        }
                        val displayMinute = String.format("%02d", reminder.minute)

                        Text(
                            text = "Scheduled Time: $displayHour:$displayMinute $amPm",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Button(
                            onClick = {
                                val timePicker = TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        dataManager.updateReminder(ReminderSettings(h, m, isEnabled))
                                        ReminderScheduler.scheduleReminder(context, h, m, isEnabled)
                                        Toast.makeText(context, "Reminder time updated", Toast.LENGTH_SHORT).show()
                                    },
                                    reminder.hour,
                                    reminder.minute,
                                    false
                                )
                                timePicker.show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Set Time", fontFamily = FontFamily.SansSerif, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alarm Enabled",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp
                        )

                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                dataManager.updateReminder(ReminderSettings(reminder.hour, reminder.minute, checked))
                                ReminderScheduler.scheduleReminder(context, reminder.hour, reminder.minute, checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Reminder scheduled" else "Reminder disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    if (!notificationsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fix Notifications settings",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { onOpenSettings() }
                            )
                        }
                    }
                }
            }
        }

        // Section: Data Backup and Restore
        item {
            Text(
                text = "Backup & Restore",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val timestamp = System.currentTimeMillis()
                            val fileName = "habit_tracker_backup_$timestamp.json"
                            fileCreatorLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Backup", fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Backup", fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Theme Color Setup (Moved to the bottom, right above Reset button)
        item {
            Text(
                text = "Theme",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("green", "Green", Color(0xFF1B5E20)),
                        Triple("orange", "Orange", Color(0xFFFF6600)),
                        Triple("slate", "Slate", Color(0xFF455A64)),
                        Triple("blue", "Blue", Color(0xFF1565C0))
                    )

                    themes.forEach { (themeName, label, colorVal) ->
                        val isSelected = dataManager.habitData.theme == themeName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) colorVal else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .background(
                                    if (isSelected) colorVal.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    dataManager.updateTheme(themeName)
                                    Toast.makeText(context, "Theme changed to $label", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colorVal else Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Report a Bug Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agrawal-d/habits/issues"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open issues page", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report Bug", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Section: Reset App Data (Simple plain button directly in item, no outer card box or text)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { resetConfirmShow = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset App", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Designed by Credit
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Designed by Divyanshu Agrawal",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
