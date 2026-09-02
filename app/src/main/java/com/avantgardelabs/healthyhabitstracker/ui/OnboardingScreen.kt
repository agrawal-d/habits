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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.zIndex
import com.avantgardelabs.healthyhabitstracker.receiver.HabitReminderReceiver
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
    var pageIndex by remember { mutableIntStateOf(0) }

    // Page 1 data: questions list
    val questions = remember { mutableStateListOf<Question>() }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingQuestion by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    // Page 2 data: reminder time
    var reminderHour by remember { mutableIntStateOf(21) } // 9 PM
    var reminderMinute by remember { mutableIntStateOf(0) }

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
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
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
        bottomBar = {
            when (pageIndex) {
                1 -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 4.dp
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
                2 -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pageIndex = 1 },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    "BACK",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
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
                                    .height(48.dp)
                            ) {
                                Text(
                                    "FINISH",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        },
        containerColor = Color(0xFFECEFF1)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            text = "Get daily reminders",
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

                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF388E3C),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
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

                1 -> {
                    // Page 1: Setup Habits (Matches EditQuestionsActivity UI)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
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
                                            "Add your habits below and press Next once done.",
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
                                        .zIndex(if (isDraggingThis) 10f else 0f)
                                        .graphicsLayer {
                                            this.translationY = translationY
                                            this.shadowElevation = if (isDraggingThis) 8.dp.toPx() else 0f
                                        }
                                        .pointerInput(question.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedIndex = questions.indexOfFirst { it.id == question.id }
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y

                                                    val dragged = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                                    val threshold = with(density) { 40.dp.toPx() }
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
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .pointerInput(question.id) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedIndex = questions.indexOfFirst { it.id == question.id }
                                                            dragOffset = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffset += dragAmount.y

                                                            val dragged = draggedIndex ?: return@detectDragGestures
                                                            val threshold = with(density) { 40.dp.toPx() }
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
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = IconMapper.getIconByName(question.icon),
                                            contentDescription = null,
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(22.dp)
                                        )
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
                }

                2 -> {
                    // Page 2: Daily Reminder Setup
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
                                    text = "Set a time to be reminded daily.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        color = Color(0xFF546E7A),
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                val displayTime = String.format(
                                    java.util.Locale.ENGLISH,
                                    "%02d:%02d %s",
                                    if (reminderHour % 12 == 0) 12 else reminderHour % 12,
                                    reminderMinute,
                                    if (reminderHour >= 12) "PM" else "AM"
                                )
                                Text(
                                    text = displayTime,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 36.sp,
                                        color = Color(0xFF0D47A1)
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
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

                                Spacer(modifier = Modifier.height(10.dp))

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
                                        .height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TEST REMINDER",
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
        }
    }
}
