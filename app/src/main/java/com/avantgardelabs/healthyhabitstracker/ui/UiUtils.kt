package com.avantgardelabs.healthyhabitstracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getSentimentIconAndColor(score: Int?): Pair<ImageVector, Color> {
    return when {
        score == null -> Icons.Default.SentimentNeutral to Color.Gray
        score >= 70 -> Icons.Default.SentimentSatisfied to Color(0xFF4CAF50)
        score >= 40 -> Icons.Default.SentimentNeutral to Color(0xFFFF9800)
        else -> Icons.Default.SentimentDissatisfied to Color(0xFFF44336)
    }
}

@Composable
fun ScoreBadge(
    score: Int?,
    modifier: Modifier = Modifier
) {
    if (score == null) {
        Box(
            modifier = modifier
                .size(44.dp)
                .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "—",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            )
        }
    } else {
        val (bgColor, textColor) = when {
            score >= 80 -> Color(0xFF2E7D32).copy(alpha = 0.12f) to Color(0xFF2E7D32)
            score >= 50 -> Color(0xFFF57F17).copy(alpha = 0.12f) to Color(0xFFE65100)
            else -> Color(0xFF5D4037).copy(alpha = 0.12f) to Color(0xFF5D4037)
        }
        val borderColor = textColor.copy(alpha = 0.4f)

        Box(
            modifier = modifier
                .size(44.dp)
                .background(bgColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
fun StatusIcon(
    pct: Double,
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    val icon = when {
        pct >= 100.0 -> Icons.Default.EmojiEvents
        pct >= 50.0 -> Icons.Default.SentimentNeutral
        else -> Icons.Default.SentimentDissatisfied
    }
    val defaultColor = when {
        pct >= 100.0 -> Color(0xFF2E7D32) // Green
        pct >= 50.0 -> Color(0xFFF57F17)  // Yellow/Orange
        else -> Color(0xFF5D4037)         // Brown
    }
    Icon(
        imageVector = icon,
        contentDescription = "Status",
        tint = tintColor ?: defaultColor,
        modifier = modifier
    )
}
