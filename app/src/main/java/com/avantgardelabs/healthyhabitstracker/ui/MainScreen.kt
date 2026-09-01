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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.avantgardelabs.healthyhabitstracker.BuildConfig
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

data class DaySummary(
    val date: LocalDate,
    val dateStr: String,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val log: LogEntry?
)

@Composable
fun ScoreBadge(
    score: Int?,
    modifier: Modifier = Modifier
) {
    if (score == null) {
        Box(
            modifier = modifier
                .size(44.dp)
                .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "—",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            )
        }
    } else {
        val (bgColor, textColor) = when {
            score >= 80 -> Color(0xFF2E7D32).copy(alpha = 0.12f) to Color(0xFF2E7D32)
            score >= 50 -> Color(0xFFF57F17).copy(alpha = 0.12f) to Color(0xFFE65100)
            else -> Color(0xFF5D4037).copy(alpha = 0.12f) to Color(0xFF5D4037)
        }
        val borderColor = textColor.copy(alpha = 0.4f)

        Box(
            modifier = modifier
                .size(44.dp)
                .background(bgColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
            )
        }
    }
}

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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItemWithIcon(
                        label = "Home",
                        icon = Icons.Default.Home,
                        isSelected = currentTab == Tab.HOME,
                        onClick = { currentTab = Tab.HOME },
                        modifier = Modifier.weight(1f)
                    )
                    TabItemWithIcon(
                        label = "History",
                        icon = Icons.AutoMirrored.Filled.List,
                        isSelected = currentTab == Tab.LOG,
                        onClick = { currentTab = Tab.LOG },
                        modifier = Modifier.weight(1f)
                    )
                    TabItemWithIcon(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = currentTab == Tab.SETTINGS,
                        onClick = { currentTab = Tab.SETTINGS },
                        modifier = Modifier.weight(1f)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            )
        }
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
    onAnswerDate: (String) -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val today = LocalDate.now()
    val last7Days = remember(dataManager.habitData.logs) {
        (0..6).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val log = dataManager.habitData.logs.firstOrNull { it.date == dateStr }
            DaySummary(
                date = date,
                dateStr = dateStr,
                isToday = offset == 0,
                isYesterday = offset == 1,
                log = log
            )
        }
    }

    val loggedScores = remember(last7Days) {
        last7Days.mapNotNull { it.log?.getScaledScore() }
    }
    val avgScore = remember(loggedScores) {
        if (loggedScores.isNotEmpty()) {
            val raw = loggedScores.sum().toDouble() / loggedScores.size
            (Math.round(raw / 5.0) * 5).toInt()
        } else null
    }
    val loggedCount = loggedScores.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            // App header title
            Text(
                text = "Habits",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 7-Day Momentum Overview Card with dynamic score-based color & icon (no fluff text)
            val cardBgColor = when {
                avgScore == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                avgScore >= 80 -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                avgScore >= 50 -> Color(0xFFF57F17).copy(alpha = 0.12f)
                else -> Color(0xFF5D4037).copy(alpha = 0.10f)
            }
            val cardBorderColor = when {
                avgScore == null -> MaterialTheme.colorScheme.outlineVariant
                avgScore >= 80 -> Color(0xFF2E7D32).copy(alpha = 0.35f)
                avgScore >= 50 -> Color(0xFFF57F17).copy(alpha = 0.35f)
                else -> Color(0xFF5D4037).copy(alpha = 0.30f)
            }
            val statusIcon = when {
                avgScore == null -> Icons.Default.QueryBuilder
                avgScore >= 80 -> Icons.Default.EmojiEvents
                avgScore >= 50 -> Icons.AutoMirrored.Filled.TrendingUp
                else -> Icons.Default.FitnessCenter
            }
            val accentColor = when {
                avgScore == null -> Color.Gray
                avgScore >= 80 -> Color(0xFF2E7D32)
                avgScore >= 50 -> Color(0xFFE65100)
                else -> Color(0xFF5D4037)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "7-Day Average",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (avgScore != null) "$avgScore" else "—",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                )
                                if (avgScore != null) {
                                    Text(
                                        text = " / 100",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Gray
                                        ),
                                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Logged",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$loggedCount / 7",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "days",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Last 7 Days",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(last7Days) { day ->
            val dateLabel = when {
                day.isToday -> "Today"
                day.isYesterday -> "Yesterday"
                else -> day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAnswerDate(day.dateStr) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (day.isToday && day.log == null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (day.isToday && day.log == null) 1.5.dp else 1.dp,
                    color = if (day.isToday && day.log == null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dateLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (day.isToday && day.log == null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )

                        if (day.log != null) {
                            val earned = day.log.getAbsoluteScore()
                            val max = day.log.getMaxScore()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$earned / $max pts",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else if (!day.isToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Not recorded",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }

                    if (day.isToday && day.log == null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Add entry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        ScoreBadge(score = day.log?.getScaledScore())
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "History",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            val today = LocalDate.now()
            when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
            }
        } catch (e: Exception) {
            entry.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${entry.getAbsoluteScore()} / ${entry.getMaxScore()} pts",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                ScoreBadge(
                    score = entry.getScaledScore()
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

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
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    // Dialog visibility states
    var showReminderDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
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

    // 1. Daily Reminder Dialog
    if (showReminderDialog) {
        val amPm = if (reminder.hour >= 12) "PM" else "AM"
        val displayHour = when {
            reminder.hour == 0 -> 12
            reminder.hour > 12 -> reminder.hour - 12
            else -> reminder.hour
        }
        val displayMinute = String.format("%02d", reminder.minute)

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Text(
                    text = "Daily Reminder",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder Time",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$displayHour:$displayMinute $amPm",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        TextButton(
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
                            }
                        ) {
                            Text("Change", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarm Enabled",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) "Notifications will arrive daily" else "Alarms are turned off",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                isEnabled = checked
                                dataManager.updateReminder(ReminderSettings(reminder.hour, reminder.minute, checked))
                                ReminderScheduler.scheduleReminder(context, reminder.hour, reminder.minute, checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Reminder enabled" else "Reminder disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    if (!notificationsEnabled) {
                        Text(
                            text = "Notifications are disabled in Android settings. Tap to fix.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.clickable { onOpenSettings() }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Done", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Theme Dialog
    if (showThemeDialog) {
        val themes = listOf(
            Triple("green", "Green", Color(0xFF1B5E20)),
            Triple("orange", "Orange", Color(0xFFFF6600)),
            Triple("slate", "Slate", Color(0xFF455A64)),
            Triple("blue", "Blue", Color(0xFF1565C0))
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Theme Color",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (themeName, label, colorVal) ->
                        val isSelected = dataManager.habitData.theme == themeName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dataManager.updateTheme(themeName)
                                    showThemeDialog = false
                                    Toast.makeText(context, "Theme set to $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(colorVal, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    dataManager.updateTheme(themeName)
                                    showThemeDialog = false
                                    Toast.makeText(context, "Theme set to $label", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", fontFamily = FontFamily.SansSerif)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Backup & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    text = "Backup & Restore",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = {
                            showBackupDialog = false
                            val timestamp = System.currentTimeMillis()
                            val fileName = "habit_tracker_backup_$timestamp.json"
                            fileCreatorLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Create Backup", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Restore Backup", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close", fontFamily = FontFamily.SansSerif)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 4. Reset Confirmation Dialog
    if (resetConfirmShow) {
        AlertDialog(
            onDismissRequest = { resetConfirmShow = false },
            title = {
                Text(
                    "Reset App Data?",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "This will completely erase all your logs, habits configuration, and settings. This action cannot be undone.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
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
                    Text("YES, RESET EVERYTHING", color = Color.Red, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmShow = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.SansSerif)
                }
            },
            shape = RoundedCornerShape(16.dp)
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Habit Questions
                    SettingItemRow(
                        title = "Habit Questions",
                        state = "${dataManager.habitData.questions.size} active habits",
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
                        onClick = { showReminderDialog = true }
                    )

                    // 3. Theme
                    val themeLabel = when (dataManager.habitData.theme) {
                        "orange" -> "Orange"
                        "slate" -> "Slate"
                        "blue" -> "Blue"
                        else -> "Green"
                    }
                    SettingItemRow(
                        title = "Theme",
                        state = themeLabel,
                        onClick = { showThemeDialog = true }
                    )

                    // 4. Backup & Restore
                    SettingItemRow(
                        title = "Backup & Restore",
                        onClick = { showBackupDialog = true }
                    )

                    // 5. License
                    SettingItemRow(
                        title = "License",
                        onClick = {
                            val intent = Intent(context, com.avantgardelabs.healthyhabitstracker.LicenseActivity::class.java)
                            context.startActivity(intent)
                        }
                    )

                    // 6. Report a Bug
                    SettingItemRow(
                        title = "Report a Bug",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agrawal-d/habits/issues"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open issues page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // 7. Reset App Data
                    SettingItemRow(
                        title = "Reset App Data",
                        onClick = { resetConfirmShow = true },
                        showDivider = true
                    )

                    // 8. Source Code
                    SettingItemRow(
                        title = "Source Code",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agrawal-d/habits"))
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

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
