package com.avantgardelabs.healthyhabitstracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getSentimentIconAndColor(score: Int?): Pair<ImageVector, Color> {
    return when {
        score == null -> Icons.Default.SentimentNeutral to Color.Gray
        score >= 70 -> Icons.Default.SentimentSatisfied to Color(0xFF4CAF50)
        score >= 40 -> Icons.Default.SentimentNeutral to Color(0xFFFF9800)
        else -> Icons.Default.SentimentDissatisfied to Color(0xFFF44336)
    }
}