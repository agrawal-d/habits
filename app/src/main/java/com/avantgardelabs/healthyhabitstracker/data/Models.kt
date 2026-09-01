package com.avantgardelabs.healthyhabitstracker.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AnswerType {
    YES, NO, PARTIAL;

    companion object {
        fun fromString(value: String): AnswerType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                NO
            }
        }
    }
}

data class Question(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val icon: String // Changed from emoji to icon
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("icon", icon)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): Question {
            return Question(
                id = obj.getString("id"),
                text = obj.getString("text"),
                icon = obj.optString("icon", obj.optString("emoji", "check"))
            )
        }
    }
}

data class LogEntry(
    val date: String, // YYYY-MM-DD
    val questions: List<Question>,
    val answers: Map<String, AnswerType>, // question.id -> AnswerType
    val note: String = ""
) {
    fun getScore(): Double {
        var score = 0.0
        for ((qId, answer) in answers) {
            when (answer) {
                AnswerType.YES -> score += 1.0
                AnswerType.PARTIAL -> score += 0.5
                AnswerType.NO -> {}
            }
        }
        return score
    }

    fun getScorePercentage(): Double {
        if (questions.isEmpty()) return 0.0
        return (getScore() / questions.size) * 100.0
    }

    fun getAbsoluteScore(): Int {
        var score = 0
        for ((_, answer) in answers) {
            when (answer) {
                AnswerType.YES -> score += 10
                AnswerType.PARTIAL -> score += 5
                AnswerType.NO -> {}
            }
        }
        return score
    }

    fun getMaxScore(): Int {
        return questions.size * 10
    }

    fun getScaledScore(): Int {
        val max = getMaxScore()
        if (max == 0) return 0
        return Math.round((getAbsoluteScore().toDouble() / max) * 100).toInt()
    }

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("date", date)
        obj.put("note", note)

        val qArray = JSONArray()
        questions.forEach { qArray.put(it.toJsonObject()) }
        obj.put("questions", qArray)

        val aObj = JSONObject()
        answers.forEach { (qId, type) ->
            aObj.put(qId, type.name)
        }
        obj.put("answers", aObj)

        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): LogEntry {
            val date = obj.getString("date")
            val note = obj.optString("note", "")

            val questionsList = mutableListOf<Question>()
            val qArray = obj.getJSONArray("questions")
            for (i in 0 until qArray.length()) {
                questionsList.add(Question.fromJsonObject(qArray.getJSONObject(i)))
            }

            val answersMap = mutableMapOf<String, AnswerType>()
            val aObj = obj.getJSONObject("answers")
            aObj.keys().forEach { key ->
                answersMap[key] = AnswerType.fromString(aObj.getString(key))
            }

            return LogEntry(date, questionsList, answersMap, note)
        }
    }
}

data class ReminderSettings(
    val hour: Int = 21, // default 9 PM
    val minute: Int = 0,
    val enabled: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("hour", hour)
            put("minute", minute)
            put("enabled", enabled)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ReminderSettings {
            return ReminderSettings(
                hour = obj.optInt("hour", 21),
                minute = obj.optInt("minute", 0),
                enabled = obj.optBoolean("enabled", true)
            )
        }
    }
}

data class HabitData(
    val questions: List<Question> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val reminder: ReminderSettings = ReminderSettings(),
    val theme: String = "green"
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        
        val qArray = JSONArray()
        questions.forEach { qArray.put(it.toJsonObject()) }
        obj.put("questions", qArray)

        val lArray = JSONArray()
        logs.forEach { lArray.put(it.toJsonObject()) }
        obj.put("logs", lArray)

        obj.put("reminder", reminder.toJsonObject())
        obj.put("theme", theme)

        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): HabitData {
            val questionsList = mutableListOf<Question>()
            if (obj.has("questions")) {
                val qArray = obj.getJSONArray("questions")
                for (i in 0 until qArray.length()) {
                    questionsList.add(Question.fromJsonObject(qArray.getJSONObject(i)))
                }
            }

            val logsList = mutableListOf<LogEntry>()
            if (obj.has("logs")) {
                val lArray = obj.getJSONArray("logs")
                for (i in 0 until lArray.length()) {
                    logsList.add(LogEntry.fromJsonObject(lArray.getJSONObject(i)))
                }
            }

            val reminder = if (obj.has("reminder")) {
                ReminderSettings.fromJsonObject(obj.getJSONObject("reminder"))
            } else {
                ReminderSettings()
            }

            val theme = obj.optString("theme", "green")

            return HabitData(questionsList, logsList, reminder, theme)
        }
    }
}
