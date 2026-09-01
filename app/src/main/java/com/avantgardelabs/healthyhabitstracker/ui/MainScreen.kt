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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.BuildConfig
import com.avantgardelabs.healthyhabitstracker.EditQuestionsActivity
import com.avantgardelabs.healthyhabitstracker.R
import com.avantgardelabs.healthyhabitstracker.data.AnswerType
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.LogEntry
import com.avantgardelabs.healthyhabitstracker.data.ReminderScheduler
import com.avantgardelabs.healthyhabitstracker.data.ReminderSettings
import com.avantgardelabs.healthyhabitstracker.receiver.HabitReminderReceiver
import java.time.LocalDate
import java.time.YearMonth
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

fun getSentimentIconAndColor(score: Int?): Pair<ImageVector, Color> {
    return when {
        score == null -> Icons.Default.SentimentNeutral to Color.Gray
        score >= 70 -> Icons.Default.SentimentSatisfied to Color(0xFF4CAF50)
        score >= 40 -> Icons.Default.SentimentNeutral to Color(0xFFFF9800)
        else -> Icons.Default.SentimentDissatisfied to Color(0xFFF44336)
    }
}

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
    HOME, LOG, SETTINGS, REMINDER
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

fun formatDisplayDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH))
    val day = date.dayOfMonth
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    val dayOrdinal = "$day$suffix"
    val isCurrentMonth = date.year == today.year && date.month == today.month
    return if (isCurrentMonth) {
        "$dayOfWeek $dayOrdinal"
    } else {
        val shortMonth = date.format(DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH))
        "$dayOfWeek $dayOrdinal $shortMonth"
    }
}

// 2. Recent Day Item with expandable note on click
@Composable
fun RecentDayItem(
    day: DaySummary,
    onAnswerDate: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val score = day.log?.getScaledScore()
    val (indicatorIcon, indicatorColor) = getSentimentIconAndColor(score)

    val extendedDateStr = remember(day.date) {
        formatDisplayDate(day.date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (day.log != null) {
                    expanded = !expanded
                } else {
                    onAnswerDate(day.dateStr)
                }
            },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Face with extended date ("Monday 31")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = indicatorIcon,
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = extendedDateStr,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF263238)
                        )
                    )
                }

                // Right: Colored bg box with % score in white (bg blue)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(68.dp)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (score != null) "$score%" else "—",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            if (expanded && day.log != null) {
                HorizontalDivider(color = Color(0xFFECEFF1))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    day.log.questions.forEachIndexed { qIdx, question ->
                        if (qIdx > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        val answer = day.log.answers[question.id] ?: AnswerType.NO
                        val ansText = when (answer) {
                            AnswerType.YES -> "Yes"
                            AnswerType.PARTIAL -> "Partially"
                            AnswerType.NO -> "No"
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
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = question.text,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF263238)
                                    )
                                )
                            }
                            Text(
                                text = ansText,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFF546E7A)
                                )
                            )
                        }
                    }

                    if (day.log.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = day.log.note,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color(0xFF37474F),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { onAnswerDate(day.dateStr) },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF1976D2)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
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
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val todayLog = dataManager.habitData.logs.firstOrNull { it.date == todayStr }
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 1: TODAY'S STATUS / REQUEST
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left prominent full-height icon container with padding
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(84.dp)
                            .background(Color(0xFFFFF9C4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = "Today Habit Star",
                            modifier = Modifier
                                .size(52.dp)
                                .padding(4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (todayLog == null) {
                            Text(
                                text = "TODAY'S CHECK-IN",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1976D2)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "You haven't logged your habits for today yet.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color(0xFF263238),
                                    lineHeight = 18.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onAnswerDate(todayStr) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CHECK IN TODAY",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TODAY",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1976D2)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${todayLog.getScaledScore()}%",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp,
                                            color = Color(0xFF0D47A1)
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = { onAnswerDate(todayStr) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit today's log",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: LAST 7 DAYS HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAST 7 DAYS",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A)
                    )
                )
                if (avgScore != null) {
                    Text(
                        text = "Avg $avgScore%",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2)
                        )
                    )
                }
            }
        }

        // Recent days
        val recentDays = last7Days.filter { !it.isToday }
        if (loggedCount == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No entries yet",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your last 7 days will appear here",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        )
                    }
                }
            }
        } else {
            items(recentDays) { day ->
                RecentDayItem(
                    day = day,
                    onAnswerDate = onAnswerDate
                )
            }
        }

        item {
            Button(
                onClick = onNavigateToLogs,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "History",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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

    val groupedLogs = remember(logs) {
        logs.groupBy { entry ->
            try {
                YearMonth.from(LocalDate.parse(entry.date))
            } catch (e: Exception) {
                YearMonth.now()
            }
        }.toSortedMap(compareByDescending { it })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Prominent History Overview Card with full-height route icon on left
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left prominent full-height icon container with padding
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(84.dp)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.list),
                        contentDescription = "History",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "${logs.size} total entries",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF263238)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.list),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No history recorded yet",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
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
                groupedLogs.forEach { (yearMonth, monthEntries) ->
                    val monthTitle = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    val scores = monthEntries.map { it.getScaledScore() }
                    val raw = scores.sum().toDouble() / scores.size
                    val monthAvg = (Math.round(raw / 5.0) * 5).toInt()

                    item(key = "header_${yearMonth}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthTitle,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF263238)
                                )
                            )
                            Text(
                                text = "Avg $monthAvg%",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1976D2)
                                )
                            )
                        }
                    }

                    items(monthEntries, key = { it.date }) { entry ->
                        LogHistoryItem(
                            entry = entry,
                            onEdit = { onEditEntry(entry.date) },
                            onDelete = { onDeleteEntry(entry.date) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
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

    val parsedDate = remember(entry.date) {
        try { LocalDate.parse(entry.date) } catch (e: Exception) { null }
    }
    val extendedDateStr = remember(entry.date) {
        parsedDate?.let { formatDisplayDate(it) } ?: entry.date
    }

    val score = entry.getScaledScore()
    val (indicatorIcon, indicatorColor) = getSentimentIconAndColor(score)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Face with extended date ("Monday 31")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = indicatorIcon,
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = extendedDateStr,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF263238)
                        )
                    )
                }

                // Right: Colored bg box with % score in white (bg blue)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(68.dp)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$score%",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(color = Color(0xFFECEFF1))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    entry.questions.forEachIndexed { qIdx, question ->
                        if (qIdx > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        val answer = entry.answers[question.id] ?: AnswerType.NO
                        val ansText = when (answer) {
                            AnswerType.YES -> "Yes"
                            AnswerType.PARTIAL -> "Partially"
                            AnswerType.NO -> "No"
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
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = question.text,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF263238)
                                    )
                                )
                            }
                            Text(
                                text = ansText,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFF546E7A)
                                )
                            )
                        }
                    }

                    if (entry.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.note,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color(0xFF37474F),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF1976D2)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFF44336)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                Uri.parse("https://www.flaticon.com/packs/essential-collection")
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

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

// 5. Full Page Daily Reminder Screen
@Composable
fun ReminderSettingsScreen(
    dataManager: DataManager,
    notificationsEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val reminder = dataManager.habitData.reminder
    var isEnabled by remember(reminder) { mutableStateOf(reminder.enabled) }

    val amPm = if (reminder.hour >= 12) "PM" else "AM"
    val displayHour = when {
        reminder.hour == 0 -> 12
        reminder.hour > 12 -> reminder.hour - 12
        else -> reminder.hour
    }
    val displayMinute = String.format(Locale.getDefault(), "%02d", reminder.minute)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Hero Card featuring alarm.png
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left prominent full-height icon container with padding
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(84.dp)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.alarm),
                            contentDescription = "Daily Reminder Alarm",
                            modifier = Modifier
                                .size(56.dp)
                                .padding(4.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Daily Habit Reminder",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF263238)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Receive daily notifications to record your habits and maintain momentum.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color(0xFF546E7A),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Enable/Disable row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Reminder",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF263238)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) "Notifications arrive daily" else "Reminders are disabled",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
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
                                    if (checked) "Daily reminder enabled" else "Daily reminder disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFFECEFF1)
                    )

                    // Scheduled time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder Time",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF263238)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$displayHour:$displayMinute $amPm",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = if (isEnabled) Color(0xFF1976D2) else Color.Gray
                                )
                            )
                        }

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
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CHANGE TIME",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            HabitReminderReceiver.showNotification(context)
                            Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF57C00),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEST REMINDER",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (!notificationsEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color(0xFFECEFF1)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                .clickable { onOpenSettings() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notifications Disabled",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Notifications are blocked in system settings. Tap here to enable permissions.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB71C1C)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
