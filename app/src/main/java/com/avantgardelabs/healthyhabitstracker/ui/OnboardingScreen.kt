package com.avantgardelabs.healthyhabitstracker.ui

import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.R
import com.avantgardelabs.healthyhabitstracker.data.Question

@Composable
fun OnboardingScreen(
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onFinished: (List<Question>, hour: Int, minute: Int) -> Unit,
    onRestoreBackup: (String) -> Boolean
) {
    var pageIndex by remember { mutableStateOf(0) }

    // Page 1 data: questions list
    val questions = remember { mutableStateListOf<Question>() }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingQuestion by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Page 2 data: reminder time
    var reminderHour by remember { mutableStateOf(21) } // 9 PM
    var reminderMinute by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = pageIndex > 0) {
        pageIndex--
    }

    if (isAddingQuestion) {
        EditQuestionScreen(
            question = Question(text = "", icon = "star"),
            title = "Add Habit Question",
            onSave = { newQuestion ->
                questions.add(newQuestion)
                isAddingQuestion = false
            },
            onCancel = {
                isAddingQuestion = false
            }
        )
        return
    }

    if (editingQuestionIndex != null && editingQuestionIndex!! in questions.indices) {
        val q = questions[editingQuestionIndex!!]
        EditQuestionScreen(
            question = q,
            title = "Edit Habit Question",
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pageIndex > 0) {
                        IconButton(onClick = { pageIndex-- }) {
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
                        text = when (pageIndex) {
                            0 -> "Healthy Habits Tracker"
                            1 -> "Setup Habits"
                            else -> "Daily Reminder"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFECEFF1)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            when (pageIndex) {
                0 -> {
                    // Page 0: Classic Material 1.0 Welcome
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                // Left vertical blue stripe
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF2196F3))
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.star),
                                        contentDescription = "Welcome Star",
                                        modifier = Modifier.size(80.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "TRACK YOUR DAILY HABITS",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1976D2)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Build lasting routines with daily check-ins, straightforward scoring, and streak tracking.",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 14.sp,
                                            color = Color(0xFF546E7A),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 20.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    Button(
                                        onClick = { pageIndex = 1 },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = "GET STARTED",
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF1976D2)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RESTORE BACKUP",
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Page 1: Setup Habits (Matches EditQuestionsActivity UI)
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Active Questions (${questions.size})",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF263238)
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            if (questions.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No habit questions configured yet.\nTap \"ADD HABIT\" below to create your first habit.",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 14.sp,
                                                    color = Color.Gray,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 20.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(questions, key = { _, q -> q.id }) { idx, question ->
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
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(
                                                        Color(0xFFE3F2FD),
                                                        RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = IconMapper.getIconByName(question.icon),
                                                    contentDescription = null,
                                                    tint = Color(0xFF1976D2),
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
                                                    color = Color(0xFF263238)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = { editingQuestionIndex = idx },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Question",
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    questions.removeAt(idx)
                                                },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Question",
                                                    tint = Color(0xFFF44336),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Bottom action buttons: ADD HABIT and NEXT
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { isAddingQuestion = true },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ADD HABIT",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = { pageIndex = 2 },
                                    enabled = questions.isNotEmpty(),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "NEXT",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Page 2: Daily Reminder Setup
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
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
                                    Image(
                                        painter = painterResource(id = R.drawable.alarm),
                                        contentDescription = "Daily Reminder Alarm",
                                        modifier = Modifier.size(80.dp)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = "Daily Check-in Reminder",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF263238)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Choose the time you want to be reminded each day to check in on your habits.",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = Color(0xFF546E7A),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 19.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Reminder Time Box
                                    val amPm = if (reminderHour >= 12) "PM" else "AM"
                                    val displayHour = when {
                                        reminderHour == 0 -> 12
                                        reminderHour > 12 -> reminderHour - 12
                                        else -> reminderHour
                                    }
                                    val displayMin = String.format("%02d", reminderMinute)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                        border = BorderStroke(1.dp, Color(0xFFCFD8DC))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Scheduled Time",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF546E7A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$displayHour:$displayMin $amPm",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 28.sp,
                                                color = Color(0xFF0D47A1)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = {
                                                    TimePickerDialog(
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
                                                shape = RoundedCornerShape(4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.White
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
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
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom action buttons
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { pageIndex = 1 },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("BACK", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (!notificationsEnabled) {
                                            onRequestNotificationPermission()
                                        }
                                        onFinished(questions.toList(), reminderHour, reminderMinute)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("FINISH", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
