package com.disciplinescore.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.MeasureSystem
import com.disciplinescore.app.domain.bmi
import com.disciplinescore.app.domain.feetInchesToCm
import com.disciplinescore.app.domain.formatStat
import com.disciplinescore.app.domain.heightInchesInput
import com.disciplinescore.app.domain.heightPrimaryInput
import com.disciplinescore.app.domain.inHeightRange
import com.disciplinescore.app.domain.inWeightRange
import com.disciplinescore.app.domain.lbToKg
import com.disciplinescore.app.domain.weightInput
import com.disciplinescore.app.ui.components.ChoiceTrack

@Composable
fun BodyMeasureFields(
    measureSystem: MeasureSystem,
    heightCm: Float?,
    weightKg: Float?,
    onMeasureSystem: (MeasureSystem) -> Unit,
    onHeightCm: (Float?) -> Unit,
    onWeightKg: (Float?) -> Unit,
) {
    var heightText by remember { mutableStateOf(heightPrimaryInput(heightCm, measureSystem)) }
    var inchesText by remember { mutableStateOf(heightInchesInput(heightCm, measureSystem)) }
    var weightText by remember { mutableStateOf(weightInput(weightKg, measureSystem)) }
    val imperial = measureSystem == MeasureSystem.IMPERIAL
    Text(
        text = "Height and weight",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    val systems = MeasureSystem.values()
    ChoiceTrack(
        labels = systems.map { it.label },
        selectedIndex = systems.indexOf(measureSystem),
        onSelect = { index ->
            val system = systems[index]
            if (system != measureSystem) {
                onMeasureSystem(system)
                heightText = heightPrimaryInput(heightCm, system)
                inchesText = heightInchesInput(heightCm, system)
                weightText = weightInput(weightKg, system)
            }
        },
        modifier = Modifier.padding(top = 12.dp),
    )
    if (imperial) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = heightText,
                onValueChange = { raw ->
                    heightText = digitsInput(raw, max = 2)
                    commitImperialHeight(heightText, inchesText, onHeightCm)
                },
                label = { Text("Feet") },
                isError = imperialHeightError(heightText, inchesText) != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = inchesText,
                onValueChange = { raw ->
                    inchesText = digitsInput(raw, max = 2)
                    commitImperialHeight(heightText, inchesText, onHeightCm)
                },
                label = { Text("Inches") },
                isError = imperialHeightError(heightText, inchesText) != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        imperialHeightError(heightText, inchesText)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    } else {
        OutlinedTextField(
            value = heightText,
            onValueChange = { raw ->
                heightText = decimalInput(raw)
                commitMetric(heightText, onHeightCm) { it.inHeightRange() }
            },
            label = { Text("Height (cm)") },
            isError = metricError(heightText, minLength = 2, "Use 50–272 cm") { it.inHeightRange() } != null,
            supportingText = {
                val message = metricError(heightText, minLength = 2, "Use 50–272 cm") { it.inHeightRange() }
                Text(message ?: "50–272 cm")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
    OutlinedTextField(
        value = weightText,
        onValueChange = { raw ->
            weightText = decimalInput(raw)
            if (imperial) commitImperialWeight(weightText, onWeightKg)
            else commitMetric(weightText, onWeightKg) { it.inWeightRange() }
        },
        label = { Text(if (imperial) "Weight (lb)" else "Weight (kg)") },
        isError = weightError(weightText, imperial) != null,
        supportingText = {
            Text(weightError(weightText, imperial) ?: if (imperial) "44.1–881.8 lb" else "20–400 kg")
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = if (imperial) 8.dp else 0.dp),
    )
    if (heightCm != null && weightKg != null) {
        val reading = bmi(heightCm, weightKg)
        if (reading != null) {
            Text(
                text = "BMI ${formatStat(reading.value)} · ${reading.band}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "From height and weight. Not a diagnosis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun commitMetric(text: String, onValue: (Float?) -> Unit, inRange: (Float) -> Boolean) {
    when {
        text.isBlank() -> onValue(null)
        else -> text.toFloatOrNull()?.takeIf(inRange)?.let(onValue)
    }
}

private fun commitImperialHeight(feetText: String, inchesText: String, onHeightCm: (Float?) -> Unit) {
    if (feetText.isBlank() && inchesText.isBlank()) {
        onHeightCm(null)
        return
    }
    val feet = feetText.toIntOrNull() ?: return
    val inches = if (inchesText.isBlank()) 0 else inchesText.toIntOrNull() ?: return
    if (inches !in 0..11) return
    val cm = feetInchesToCm(feet, inches)
    if (cm.inHeightRange()) onHeightCm(cm)
}

private fun commitImperialWeight(text: String, onWeightKg: (Float?) -> Unit) {
    when {
        text.isBlank() -> onWeightKg(null)
        else -> text.toFloatOrNull()?.let { pounds ->
            val kg = lbToKg(pounds)
            if (kg.inWeightRange()) onWeightKg(kg)
        }
    }
}

private fun imperialHeightError(feetText: String, inchesText: String): String? {
    if (feetText.isBlank() && inchesText.isBlank()) return null
    val inches = inchesText.toIntOrNull()
    if (inchesText.isNotEmpty() && (inches == null || inches !in 0..11)) return "Inches go from 0 to 11"
    val feet = feetText.toIntOrNull() ?: return null
    val cm = feetInchesToCm(feet, inches ?: 0)
    return if (cm.inHeightRange()) null else "Use 1 ft 8 in to 8 ft 11 in"
}

private fun metricError(
    text: String,
    minLength: Int,
    rangeMessage: String,
    inRange: (Float) -> Boolean,
): String? {
    if (text.isBlank() || text.length < minLength) return null
    val value = text.toFloatOrNull() ?: return "Enter a number"
    return if (inRange(value)) null else rangeMessage
}

private fun weightError(text: String, imperial: Boolean): String? {
    if (text.isBlank() || text.length < 2) return null
    val value = text.toFloatOrNull() ?: return "Enter a number"
    val kg = if (imperial) lbToKg(value) else value
    return if (kg.inWeightRange()) null else if (imperial) "Use 44.1–881.8 lb" else "Use 20–400 kg"
}

private fun digitsInput(raw: String, max: Int): String = raw.filter { it.isDigit() }.take(max)

private fun decimalInput(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    if (dot < 0) return cleaned.take(5)
    val whole = cleaned.substring(0, dot).take(4)
    val fraction = cleaned.substring(dot + 1).filter { it.isDigit() }.take(1)
    return "$whole.$fraction"
}
