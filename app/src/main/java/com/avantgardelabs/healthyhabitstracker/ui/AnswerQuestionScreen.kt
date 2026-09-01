package com.avantgardelabs.healthyhabitstracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.data.AnswerType
import com.avantgardelabs.healthyhabitstracker.data.Question
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AnswerQuestionScreen(
    dateStr: String,
    questions: List<Question>,
    existingAnswers: Map<String, AnswerType>?,
    existingNote: String?,
    onSave: (answers: Map<String, AnswerType>, note: String) -> Unit,
    onCancel: () -> Unit
) {
    val answers = remember {
        mutableStateMapOf<String, AnswerType>().apply {
            existingAnswers?.let { putAll(it) }
        }
    }
    var note by remember { mutableStateOf(existingNote ?: "") }

    val formattedDate = remember(dateStr) {
        try {
            val date = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMMM d"))
                else -> date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    val isAllAnswered = questions.all { answers.containsKey(it.id) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    val answeredCount = questions.count { answers.containsKey(it.id) }
                    Text(
                        text = "$answeredCount of ${questions.size} answered",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )
                }

                Button(
                    onClick = {
                        if (isAllAnswered) {
                            onSave(answers.toMap(), note)
                        }
                    },
                    enabled = isAllAnswered,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Save",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Unified Questions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        questions.forEachIndexed { index, question ->
                            val selectedAnswer = answers[question.id]

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = IconMapper.getIconByName(question.icon),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = question.text,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                SegmentedAnswerRow(
                                    selectedAnswer = selectedAnswer,
                                    onSelect = { answers[question.id] = it }
                                )
                            }

                            if (index < questions.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Notes",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SegmentedAnswerRow(
    selectedAnswer: AnswerType?,
    onSelect: (AnswerType) -> Unit
) {
    val options = listOf(
        Triple(AnswerType.YES, "Yes", Color(0xFF2E7D32)),
        Triple(AnswerType.PARTIAL, "Partially", Color(0xFFE65100)),
        Triple(AnswerType.NO, "No", Color(0xFF455A64))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(20.dp)
            )
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { idx, (type, label, accentColor) ->
            val isSelected = selectedAnswer == type

            val (bgModifier, textColor) = if (isSelected) {
                val containerColor = when (type) {
                    AnswerType.YES -> Color(0xFFE8F5E9)
                    AnswerType.PARTIAL -> Color(0xFFFFF3E0)
                    AnswerType.NO -> Color(0xFFECEFF1)
                }
                Modifier
                    .background(containerColor, RoundedCornerShape(18.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp)) to accentColor
            } else {
                Modifier to Color.Gray
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(bgModifier)
                    .clickable { onSelect(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = textColor
                    )
                )
            }

            if (idx < options.size - 1 && !isSelected && selectedAnswer != options[idx + 1].first) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                )
            }
        }
    }
}
