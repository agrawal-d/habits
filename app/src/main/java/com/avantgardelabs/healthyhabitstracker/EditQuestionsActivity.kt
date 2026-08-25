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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
            val themeName = dataManager.habitData.theme
            val themeColor = when (themeName) {
                "orange" -> Color(0xFFFF6600)
                "slate" -> Color(0xFF455A64)
                "blue" -> Color(0xFF1565C0)
                else -> Color(0xFF1B5E20) // default green
            }

            HealthyHabitsTrackerTheme(primaryColor = themeColor) {
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

    var editModeId by remember { mutableStateOf<String?>(null) }
    var editModeText by remember { mutableStateOf("") }
    var editModeIcon by remember { mutableStateOf("") }

    var addModeText by remember { mutableStateOf("") }
    var addModeIcon by remember { mutableStateOf("star") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(20.dp)
    ) {
        // Custom Header Bar with Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Configure Habit Questions",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create box
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
                Text(
                    text = "Add Habit Question",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Level 1: Icon Selector
                Text(
                    text = "Select Habit Icon",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable row of icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconMapper.availableIcons.forEach { pair ->
                        val name = pair.first
                        val vector = pair.second
                        val isSelected = addModeIcon == name
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { addModeIcon = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Level 2: Description
                Text(
                    text = "Question Description",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (addModeText.isEmpty()) {
                        Text(
                            text = "e.g. Ate healthy breakfast today",
                            style = TextStyle(color = Color.Gray, fontSize = 13.sp)
                        )
                    }
                    BasicTextField(
                        value = addModeText,
                        onValueChange = { addModeText = it },
                        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val text = addModeText.trim()
                        if (text.isNotEmpty()) {
                            dataManager.addQuestion(Question(text = text, icon = addModeIcon))
                            addModeText = ""
                            keyboardController?.hide()
                            Toast.makeText(context, "Habit added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter question description", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "Add Habit",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Active Questions (${dataManager.habitData.questions.size})",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Questions List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val questions = dataManager.habitData.questions
            if (questions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No questions configured yet.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
            } else {
                itemsIndexed(questions) { idx, question ->
                    val isEditing = editModeId == question.id
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
                                                val mutableList = questions.toMutableList()
                                                val temp = mutableList[dragged]
                                                mutableList[dragged] = mutableList[targetIndex]
                                                mutableList[targetIndex] = temp
                                                dataManager.updateQuestionsList(mutableList)
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
                            if (!isEditing) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (isEditing) {
                                // Inline Edit
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Rotate icons inside edit modes
                                            val currentIdx = IconMapper.availableIcons.indexOfFirst { it.first == editModeIcon }
                                            val nextIdx = (currentIdx + 1) % IconMapper.availableIcons.size
                                            editModeIcon = IconMapper.availableIcons[nextIdx].first
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIconByName(editModeIcon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = editModeText,
                                        onValueChange = { editModeText = it },
                                        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                 Text(
                                     text = "Save",
                                     style = TextStyle(
                                         fontFamily = FontFamily.SansSerif,
                                         fontSize = 11.sp,
                                         color = Color(0xFF2E7D32),
                                         fontWeight = FontWeight.Bold
                                     ),
                                     modifier = Modifier.clickable {
                                         if (editModeText.trim().isNotEmpty()) {
                                             dataManager.updateQuestion(
                                                 Question(
                                                     id = question.id,
                                                     text = editModeText.trim(),
                                                     icon = editModeIcon
                                                 )
                                             )
                                             editModeId = null
                                             keyboardController?.hide()
                                             Toast.makeText(context, "Habit updated", Toast.LENGTH_SHORT).show()
                                         }
                                     }
                                 )
                                 Spacer(modifier = Modifier.width(12.dp))
                                 Text(
                                     text = "Cancel",
                                     style = TextStyle(
                                         fontFamily = FontFamily.SansSerif,
                                         fontSize = 11.sp,
                                         color = Color.Gray
                                     ),
                                     modifier = Modifier.clickable {
                                         editModeId = null
                                         keyboardController?.hide()
                                     }
                                 )
                            } else {
                                // Display Row
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
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
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(onClick = {
                                    editModeId = question.id
                                    editModeText = question.text
                                    editModeIcon = question.icon
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                 IconButton(onClick = {
                                     dataManager.deleteQuestion(question.id)
                                     Toast.makeText(context, "Habit deleted", Toast.LENGTH_SHORT).show()
                                 }, modifier = Modifier.size(28.dp)) {
                                     Icon(
                                         imageVector = Icons.Default.Delete,
                                         contentDescription = "Delete",
                                         tint = Color.Gray,
                                         modifier = Modifier.size(16.dp)
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
