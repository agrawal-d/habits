package com.avantgardelabs.healthyhabitstracker.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.avantgardelabs.healthyhabitstracker.data.Question
import java.util.UUID
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

@Composable
fun OnboardingScreen(
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onFinished: (List<Question>, hour: Int, minute: Int) -> Unit,
    onRestoreBackup: (String) -> Boolean
) {
    var pageIndex by remember { mutableStateOf(0) }
    
    // Page 1 data: empty questions list (do not pre-fill)
    val questions = remember { mutableStateListOf<Question>() }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var newQuestionText by remember { mutableStateOf("") }
    var newQuestionIcon by remember { mutableStateOf("star") }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Page 2 data: reminder time
    var reminderHour by remember { mutableStateOf(21) } // 9 PM
    var reminderMinute by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    if (editingQuestionIndex != null && editingQuestionIndex!! in questions.indices) {
        val q = questions[editingQuestionIndex!!]
        EditQuestionScreen(
            question = q,
            onSave = { updated ->
                questions[editingQuestionIndex!!] = updated
                editingQuestionIndex = null
            },
            onCancel = {
                editingQuestionIndex = null
            }
        )
        return
    }

    // Launcher for file import (restore backup) during onboarding
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val success = onRestoreBackup(json)
                    if (success) {
                        Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "ERROR: Invalid JSON structure", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when (pageIndex) {
                0 -> {
                    // Page 0: Intro
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "HEALTHY HABITS TRACKER",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Get reminders to track habits daily",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { pageIndex = 1 },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Get Started", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Restore Backup", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                    }
                }
                1 -> {
                    // Page 1: Questions setup (empty by default, separate boxes)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Define your daily habits",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Separate input boxes (2 levels)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Level 1: Icon Selection
                                Text(
                                    text = "Select Habit Icon",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconMapper.availableIcons.forEach { pair ->
                                        val name = pair.first
                                        val vector = pair.second
                                        val isSelected = newQuestionIcon == name
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { newQuestionIcon = name },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = vector,
                                                contentDescription = name,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Level 2: Text description
                                Text(
                                    text = "Habit Description",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (newQuestionText.isEmpty()) {
                                        Text(
                                            text = "e.g. Ate healthy breakfast today",
                                            style = TextStyle(color = Color.Gray, fontSize = 13.sp)
                                        )
                                    }
                                    BasicTextField(
                                        value = newQuestionText,
                                        onValueChange = { newQuestionText = it },
                                        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val text = newQuestionText.trim()
                                        if (text.isNotEmpty()) {
                                            questions.add(Question(text = text, icon = newQuestionIcon))
                                            newQuestionText = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    modifier = Modifier.align(Alignment.End).height(36.dp)
                                ) {
                                    Text("Add Habit", fontFamily = FontFamily.SansSerif, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Added list
                        Text(
                            text = "Habits Checklist (${questions.size})",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (questions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Please add at least one habit question to proceed.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                itemsIndexed(questions) { idx, q ->
                                    val isDraggingThis = draggedIndex == idx
                                    val translationY = if (isDraggingThis) dragOffset else 0f

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                this.translationY = translationY
                                                this.shadowElevation = if (isDraggingThis) 8.dp.toPx() else 0f
                                            }
                                            .pointerInput(idx) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedIndex = idx
                                                        dragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount.y

                                                        val dragged = draggedIndex
                                                        if (dragged != null) {
                                                            val threshold = with(density) { 56.dp.toPx() }
                                                            val targetIndex = if (dragOffset > threshold && dragged < questions.size - 1) {
                                                                dragged + 1
                                                            } else if (dragOffset < -threshold && dragged > 0) {
                                                                dragged - 1
                                                            } else {
                                                                null
                                                            }
                                                            if (targetIndex != null) {
                                                                val temp = questions[dragged]
                                                                questions[dragged] = questions[targetIndex]
                                                                questions[targetIndex] = temp
                                                                draggedIndex = targetIndex
                                                                dragOffset = 0f
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        draggedIndex = null
                                                        dragOffset = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = null
                                                        dragOffset = 0f
                                                    }
                                                )
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = IconMapper.getIconByName(q.icon),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = q.text,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { editingQuestionIndex = idx },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Question",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { questions.removeAt(idx) },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Question",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { pageIndex = 0 },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Back", fontFamily = FontFamily.SansSerif)
                        }

                        Button(
                            onClick = { pageIndex = 2 },
                            enabled = questions.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Next", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> {
                    // Page 2: Notification setup and time
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Enable Reminder",
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
                            text = "Choose your check-in time and enable reminders.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Reminder Time Box
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val amPm = if (reminderHour >= 12) "PM" else "AM"
                                val displayHour = when {
                                    reminderHour == 0 -> 12
                                    reminderHour > 12 -> reminderHour - 12
                                    else -> reminderHour
                                }
                                val displayMin = String.format("%02d", reminderMinute)

                                Text(
                                    text = "Scheduled Time: $displayHour:$displayMin $amPm",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                reminderHour = h
                                                reminderMinute = m
                                            },
                                            reminderHour,
                                            reminderMinute,
                                            false
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("Select Time", fontFamily = FontFamily.SansSerif, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { pageIndex = 1 },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Back", fontFamily = FontFamily.SansSerif)
                        }

                        Button(
                            onClick = {
                                if (!notificationsEnabled) {
                                    onRequestNotificationPermission()
                                }
                                onFinished(questions.toList(), reminderHour, reminderMinute)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Finish", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
