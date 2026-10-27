package com.disciplinescore.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.disciplinescore.app.domain.HabitCategory

fun HabitCategory.icon(): ImageVector = when (this) {
    HabitCategory.BODY -> Icons.Filled.FitnessCenter
    HabitCategory.MIND -> Icons.AutoMirrored.Filled.MenuBook
    HabitCategory.REST -> Icons.Filled.Bedtime
    HabitCategory.HEALTH -> Icons.Filled.WaterDrop
    HabitCategory.FOCUS -> Icons.Filled.School
}

@Composable
fun HabitCategory.tint(): Color {
    val onDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (onDark) darkTint() else lightTint()
}

private fun HabitCategory.lightTint(): Color = when (this) {
    HabitCategory.BODY -> Color(0xFF1B6B45)
    HabitCategory.MIND -> Color(0xFF2C5E9A)
    HabitCategory.REST -> Color(0xFF5C4D8A)
    HabitCategory.HEALTH -> Color(0xFF1E7A84)
    HabitCategory.FOCUS -> Color(0xFFA8641A)
}

private fun HabitCategory.darkTint(): Color = when (this) {
    HabitCategory.BODY -> Color(0xFF8FCBB0)
    HabitCategory.MIND -> Color(0xFF9EBEF0)
    HabitCategory.REST -> Color(0xFFC4B4E8)
    HabitCategory.HEALTH -> Color(0xFF8FD4DC)
    HabitCategory.FOCUS -> Color(0xFFF0C27A)
}
