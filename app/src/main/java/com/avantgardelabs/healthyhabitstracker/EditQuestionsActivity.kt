package com.avantgardelabs.healthyhabitstracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.avantgardelabs.healthyhabitstracker.ui.EditQuestionScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.Question
import com.avantgardelabs.healthyhabitstracker.ui.IconMapper
import com.avantgardelabs.healthyhabitstracker.ui.theme.HealthyHabitsTrackerTheme

class EditQuestionsActivity : ComponentActivity() {

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dataManager = DataManager(applicationContext)

        setContent {
            HealthyHabitsTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditQuestionsContent(
                        dataManager = dataManager,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun EditQuestionsContent(
    dataManager: DataManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var editingQuestion by remember { mutableStateOf<Question?>(null) }
    var isAddingQuestion by remember { mutableStateOf(false) }

    if (isAddingQuestion) {
        EditQuestionScreen(
            question = Question(text = "", icon = "star"),
            title = "Add Habit Question",
            onSave = { newQuestion ->
                dataManager.addQuestion(newQuestion)
                isAddingQuestion = false
                Toast.makeText(context, "Habit added", Toast.LENGTH_SHORT).show()
            },
            onCancel = {
                isAddingQuestion = false
            }
        )
        return
    }

    if (editingQuestion != null) {
        EditQuestionScreen(
            question = editingQuestion!!,
            title = "Edit Habit Question",
            onSave = { updated ->
                dataManager.updateQuestion(updated)
                editingQuestion = null
                Toast.makeText(context, "Habit updated", Toast.LENGTH_SHORT).show()
            },
            onCancel = {
                editingQuestion = null
            }
        )
        return
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val questions = dataManager.habitData.questions

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = Color(0xFFECEFF1),
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
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Habit Questions",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADD HABIT",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                "No habit questions configured yet.\nTap \"ADD HABIT\" below to create your first habit.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                        draggedIndex = dataManager.habitData.questions.indexOfFirst { it.id == question.id }
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y

                                        val dragged = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                        val currentQuestions = dataManager.habitData.questions
                                        val threshold = with(density) { 40.dp.toPx() }
                                        val targetIndex = if (dragOffset > threshold && dragged < currentQuestions.size - 1) {
                                            dragged + 1
                                        } else if (dragOffset < -threshold && dragged > 0) {
                                            dragged - 1
                                        } else {
                                            null
                                        }
                                        if (targetIndex != null) {
                                            val mutableList = currentQuestions.toMutableList()
                                            val temp = mutableList[dragged]
                                            mutableList[dragged] = mutableList[targetIndex]
                                            mutableList[targetIndex] = temp
                                            dataManager.updateQuestionsList(mutableList)
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
                                                draggedIndex = dataManager.habitData.questions.indexOfFirst { it.id == question.id }
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y

                                                val dragged = draggedIndex ?: return@detectDragGestures
                                                val currentQuestions = dataManager.habitData.questions
                                                val threshold = with(density) { 40.dp.toPx() }
                                                val targetIndex = if (dragOffset > threshold && dragged < currentQuestions.size - 1) {
                                                    dragged + 1
                                                } else if (dragOffset < -threshold && dragged > 0) {
                                                    dragged - 1
                                                } else {
                                                    null
                                                }
                                                if (targetIndex != null) {
                                                    val mutableList = currentQuestions.toMutableList()
                                                    val temp = mutableList[dragged]
                                                    mutableList[dragged] = mutableList[targetIndex]
                                                    mutableList[targetIndex] = temp
                                                    dataManager.updateQuestionsList(mutableList)
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
                                onClick = { editingQuestion = question },
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
                                    dataManager.deleteQuestion(question.id)
                                    Toast.makeText(context, "Habit deleted", Toast.LENGTH_SHORT).show()
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
}
