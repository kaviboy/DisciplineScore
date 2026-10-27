package com.disciplinescore.app.domain

import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt

const val MIN_HEIGHT_CM = 50f
const val MAX_HEIGHT_CM = 272f
const val MIN_WEIGHT_KG = 20f
const val MAX_WEIGHT_KG = 400f

private const val CM_PER_INCH = 2.54
private const val LB_PER_KG = 2.2046226218

enum class MeasureSystem(val label: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial"),
}

data class FeetInches(val feet: Int, val inches: Int)

data class BmiReading(val value: Double, val band: String)

fun ageYears(birthDate: LocalDate, today: LocalDate): Int =
    Period.between(birthDate, today).years

fun Float.inHeightRange(): Boolean = this in MIN_HEIGHT_CM..MAX_HEIGHT_CM

fun Float.inWeightRange(): Boolean = this in MIN_WEIGHT_KG..MAX_WEIGHT_KG

fun cmToFeetInches(cm: Float): FeetInches {
    val totalInches = (cm / CM_PER_INCH).roundToInt()
    return FeetInches(feet = totalInches / 12, inches = totalInches % 12)
}

fun feetInchesToCm(feet: Int, inches: Int): Float =
    ((feet * 12 + inches) * CM_PER_INCH).toFloat()

fun kgToLb(kg: Float): Double = kg * LB_PER_KG

fun lbToKg(lb: Float): Float = (lb / LB_PER_KG).toFloat()

/** Adult BMI bands. A figure from height and weight, not a diagnosis. */
fun bmi(heightCm: Float, weightKg: Float): BmiReading? {
    if (!heightCm.inHeightRange() || !weightKg.inWeightRange()) return null
    val meters = heightCm / 100.0
    val value = weightKg / (meters * meters)
    val band = when {
        value < 18.5 -> "Underweight"
        value < 25.0 -> "Healthy range"
        value < 30.0 -> "Overweight"
        else -> "Higher range"
    }
    return BmiReading(value, band)
}

fun formatStat(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val fraction = kotlin.math.abs(tenths % 10)
    return if (fraction == 0) whole.toString() else "$whole.$fraction"
}

fun formatHeight(cm: Float, system: MeasureSystem): String = when (system) {
    MeasureSystem.METRIC -> "${formatStat(cm.toDouble())} cm"
    MeasureSystem.IMPERIAL -> {
        val parts = cmToFeetInches(cm)
        "${parts.feet} ft ${parts.inches} in"
    }
}

fun formatWeight(kg: Float, system: MeasureSystem): String = when (system) {
    MeasureSystem.METRIC -> "${formatStat(kg.toDouble())} kg"
    MeasureSystem.IMPERIAL -> "${formatStat(kgToLb(kg))} lb"
}

fun heightPrimaryInput(cm: Float?, system: MeasureSystem): String {
    val height = cm?.takeIf { it.inHeightRange() } ?: return ""
    return when (system) {
        MeasureSystem.METRIC -> formatStat(height.toDouble())
        MeasureSystem.IMPERIAL -> cmToFeetInches(height).feet.toString()
    }
}

fun heightInchesInput(cm: Float?, system: MeasureSystem): String {
    val height = cm?.takeIf { it.inHeightRange() } ?: return ""
    if (system != MeasureSystem.IMPERIAL) return ""
    return cmToFeetInches(height).inches.toString()
}

fun weightInput(kg: Float?, system: MeasureSystem): String {
    val weight = kg?.takeIf { it.inWeightRange() } ?: return ""
    val shown = when (system) {
        MeasureSystem.METRIC -> weight.toDouble()
        MeasureSystem.IMPERIAL -> kgToLb(weight)
    }
    return formatStat(shown)
}

/** One line for the day screen. Empty until height or weight is saved. */
fun bodySummary(heightCm: Float?, weightKg: Float?, system: MeasureSystem): String? {
    val height = heightCm?.takeIf { it.inHeightRange() }
    val weight = weightKg?.takeIf { it.inWeightRange() }
    if (height == null && weight == null) return null
    val parts = mutableListOf<String>()
    if (height != null) parts += formatHeight(height, system)
    if (weight != null) parts += formatWeight(weight, system)
    if (height != null && weight != null) {
        bmi(height, weight)?.let { reading -> parts += "BMI ${formatStat(reading.value)}" }
    }
    return parts.joinToString(" · ")
}
