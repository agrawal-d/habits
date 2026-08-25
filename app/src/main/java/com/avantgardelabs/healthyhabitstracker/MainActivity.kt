package com.avantgardelabs.healthyhabitstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.avantgardelabs.healthyhabitstracker.data.DataManager
import com.avantgardelabs.healthyhabitstracker.data.ReminderScheduler
import com.avantgardelabs.healthyhabitstracker.data.ReminderSettings
import com.avantgardelabs.healthyhabitstracker.ui.MainScreen
import com.avantgardelabs.healthyhabitstracker.ui.OnboardingScreen
import com.avantgardelabs.healthyhabitstracker.ui.theme.HealthyHabitsTrackerTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private lateinit var dataManager: DataManager
    private val currentIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dataManager = DataManager(applicationContext)
        currentIntent.value = intent

        setContent {
            val themeName = dataManager.habitData.theme
            val themeColor = when (themeName) {
                "orange" -> Color(0xFFFF6600)
                "slate" -> Color(0xFF455A64)
                "blue" -> Color(0xFF1565C0)
                else -> Color(0xFF1B5E20) // default green
            }

            HealthyHabitsTrackerTheme(primaryColor = themeColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current

                    // Check notification permission state reactively
                    var notificationsEnabled by remember {
                        mutableStateOf(checkNotificationPermission())
                    }

                    // Observe lifecycle changes (like returning from settings or another activity)
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                notificationsEnabled = checkNotificationPermission()
                                // Reload questions list to sync with EditQuestionsActivity changes
                                dataManager.reload()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    // Launcher for request permissions on API 33+
                    val requestPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted: Boolean ->
                        notificationsEnabled = isGranted
                        if (isGranted) {
                            val reminder = dataManager.habitData.reminder
                            if (reminder.enabled) {
                                ReminderScheduler.scheduleReminder(
                                    context,
                                    reminder.hour,
                                    reminder.minute,
                                    true
                                )
                            }
                        }
                    }

                    val questions = dataManager.habitData.questions
                    val isOnboardingRequired = questions.isEmpty()

                    if (isOnboardingRequired) {
                        OnboardingScreen(
                            notificationsEnabled = notificationsEnabled,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsEnabled = checkNotificationPermission()
                                }
                            },
                            onFinished = { onboardingQuestions, hour, minute ->
                                dataManager.updateQuestionsList(onboardingQuestions)
                                
                                // Save and schedule reminder selected in onboarding
                                dataManager.updateReminder(ReminderSettings(hour, minute, true))
                                ReminderScheduler.scheduleReminder(
                                    context,
                                    hour,
                                    minute,
                                    true
                                )
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRestoreBackup = { json ->
                                dataManager.importData(json)
                            }
                        )
                    } else {
                        // Extract notification intents parameters
                        var editingDate by remember { mutableStateOf<String?>(null) }
                        val activeIntent = currentIntent.value

                        LaunchedEffect(activeIntent) {
                            if (activeIntent?.getBooleanExtra("extra_launch_questionnaire", false) == true) {
                                val date = activeIntent.getStringExtra("extra_date") ?: LocalDate.now().toString()
                                editingDate = date
                                activeIntent.putExtra("extra_launch_questionnaire", false)
                            }
                        }

                        // Set up initial alarm on first launch of the main screen if not done
                        LaunchedEffect(Unit) {
                            val reminder = dataManager.habitData.reminder
                            if (reminder.enabled) {
                                ReminderScheduler.scheduleReminder(
                                    context,
                                    reminder.hour,
                                    reminder.minute,
                                    true
                                )
                            }
                        }

                        if (editingDate != null) {
                            val targetDate = editingDate!!
                            val existingLog = dataManager.habitData.logs.firstOrNull { it.date == targetDate }
                            val questionsToLog = existingLog?.questions ?: dataManager.habitData.questions
                            val existingAnswers = existingLog?.answers
                            val existingNote = existingLog?.note

                            com.avantgardelabs.healthyhabitstracker.ui.AnswerQuestionScreen(
                                dateStr = targetDate,
                                questions = questionsToLog,
                                existingAnswers = existingAnswers,
                                existingNote = existingNote,
                                onSave = { answers, note ->
                                    val newEntry = com.avantgardelabs.healthyhabitstracker.data.LogEntry(
                                        date = targetDate,
                                        questions = questionsToLog,
                                        answers = answers,
                                        note = note
                                    )
                                    dataManager.saveLogEntry(newEntry)
                                    editingDate = null
                                },
                                onCancel = {
                                    editingDate = null
                                }
                            )
                        } else {
                            MainScreen(
                                dataManager = dataManager,
                                notificationsEnabled = notificationsEnabled,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        notificationsEnabled = checkNotificationPermission()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent.value = intent
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }
}