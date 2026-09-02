package com.avantgardelabs.healthyhabitstracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.R
import com.avantgardelabs.healthyhabitstracker.data.AnswerType
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.LogEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class DaySummary(
    val date: LocalDate,
    val dateStr: String,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val log: LogEntry?
)

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
            (raw / 5.0).roundToInt() * 5
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
                                text = "Record your habits",
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
                                    text = "Start",
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                        text = "You recorded today's habits",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = Color(0xFF263238)
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${todayLog.getScaledScore()}%",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = Color(0xFF0D47A1)
                                        )
                                    )
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
