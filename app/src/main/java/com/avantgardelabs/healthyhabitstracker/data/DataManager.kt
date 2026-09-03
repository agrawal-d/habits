package com.avantgardelabs.healthyhabitstracker.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File

class DataManager(private val context: Context) {
    private val file = File(context.filesDir, "habits_data.json")

    var habitData by mutableStateOf(HabitData())
        private set

    init {
        load()
    }

    private fun load() {
        if (file.exists()) {
            try {
                val json = file.readText()
                habitData = HabitData.fromJsonObject(JSONObject(json))
            } catch (e: Exception) {
                e.printStackTrace()
                habitData = HabitData()
            }
        } else {
            habitData = HabitData()
        }
    }

    private fun save() {
        try {
            val json = habitData.toJsonObject().toString(2)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addQuestion(question: Question) {
        val newQuestions = habitData.questions + question
        habitData = habitData.copy(questions = newQuestions)
        save()
    }

    fun deleteQuestion(id: String) {
        val newQuestions = habitData.questions.filter { it.id != id }
        habitData = habitData.copy(questions = newQuestions)
        save()
    }

    fun updateQuestionsList(questions: List<Question>) {
        habitData = habitData.copy(questions = questions)
        save()
    }

    fun updateQuestion(updatedQuestion: Question) {
        val newQuestions = habitData.questions.map {
            if (it.id == updatedQuestion.id) updatedQuestion else it
        }
        habitData = habitData.copy(questions = newQuestions)
        save()
    }

    fun saveLogEntry(entry: LogEntry) {
        val filteredLogs = habitData.logs.filter { it.date != entry.date }
        val newLogs = (filteredLogs + entry).sortedByDescending { it.date }
        habitData = habitData.copy(logs = newLogs)
        save()
    }

    fun deleteLogEntry(date: String) {
        val newLogs = habitData.logs.filter { it.date != date }
        habitData = habitData.copy(logs = newLogs)
        save()
    }

    fun updateReminder(settings: ReminderSettings) {
        habitData = habitData.copy(reminder = settings)
        save()
    }

    fun updateTheme(themeName: String) {
        habitData = habitData.copy(theme = themeName)
        save()
    }

    fun reload() {
        load()
    }

    fun incrementLaunchCount(): Int {
        val newCount = habitData.launchCount + 1
        habitData = habitData.copy(launchCount = newCount)
        save()
        return newCount
    }

    fun markRatingPrompted() {
        habitData = habitData.copy(hasPromptedRating = true)
        save()
    }

    fun clearAllData() {
        habitData = HabitData()
        save()
    }

    fun saveHabitData(data: HabitData) {
        habitData = data
        save()
    }

    fun exportData(): String {
        return habitData.toJsonObject().toString(2)
    }

    fun importData(jsonString: String): Boolean {
        return try {
            val imported = HabitData.fromJsonObject(JSONObject(jsonString))
            habitData = imported
            save()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
