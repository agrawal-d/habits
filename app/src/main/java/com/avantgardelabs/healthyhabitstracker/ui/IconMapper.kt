package com.avantgardelabs.healthyhabitstracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    val availableIcons = listOf(
        "star" to Icons.Default.Star,
        "food" to Icons.Default.Restaurant,
        "alarm" to Icons.Default.Alarm,
        "night" to Icons.Default.NightsStay,
        "gym" to Icons.Default.FitnessCenter,
        "water" to Icons.Default.WaterDrop,
        "book" to Icons.Default.Book,
        "walk" to Icons.Default.DirectionsWalk,
        "check" to Icons.Default.Check
    )

    fun getIconByName(name: String): ImageVector {
        val matched = availableIcons.firstOrNull { it.first.equals(name.trim(), ignoreCase = true) }
        return matched?.second ?: Icons.Default.Star // default to star
    }
}
