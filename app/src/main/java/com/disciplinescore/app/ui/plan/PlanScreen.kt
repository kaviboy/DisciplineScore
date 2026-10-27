package com.disciplinescore.app.ui.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.data.UserPreferences
import com.disciplinescore.app.domain.ActivityLevel
import com.disciplinescore.app.domain.TrainingGoal
import com.disciplinescore.app.domain.TrainingPlan
import com.disciplinescore.app.domain.ageYears
import com.disciplinescore.app.domain.buildTrainingPlan
import com.disciplinescore.app.ui.components.ChoicePills
import com.disciplinescore.app.ui.components.PillSwitch
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SoftCard

@Composable
fun DietScreen(
    preferences: UserPreferences,
    onActivity: (ActivityLevel) -> Unit,
    onTrainingGoal: (TrainingGoal) -> Unit,
    onWheyEnabled: (Boolean) -> Unit,
    onWheyGrams: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = preferences.heightCm
    val weight = preferences.weightKg
    val sex = preferences.sex
    val age = preferences.birthDate?.let { ageYears(it, java.time.LocalDate.now()) }
    val activity = preferences.activity
    val goal = preferences.trainingGoal
    val whey = if (preferences.wheyEnabled) preferences.wheyGrams else 0
    val plan = remember(height, weight, age, sex, activity, goal, whey) {
        if (height != null && weight != null && sex != null && age != null) {
            buildTrainingPlan(
                height,
                weight,
                age,
                sex,
                activity,
                goal,
                wheyProteinGrams = whey,
            )
        } else {
            null
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        ScreenHeader(
            title = "Diet",
            subtitle = "${preferences.trainingGoal.label} portions from Sri Lankan food.",
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
        )
        GoalToggle(goal = preferences.trainingGoal, onGoal = onTrainingGoal)

        if (height == null || weight == null) {
            SoftCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Add height and weight", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "The meals and calories scale from those two numbers. Set them on your profile.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    OutlinedButton(onClick = onOpenProfile, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Open profile")
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Activity", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Age comes from your date of birth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                val levels = ActivityLevel.values()
                ChoicePills(
                    labels = levels.map { it.label },
                    isSelected = { levels[it] == preferences.activity },
                    onSelect = { onActivity(levels[it]) },
                )
                Text(
                    text = preferences.activity.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        WheyCard(
            enabled = preferences.wheyEnabled,
            grams = preferences.wheyGrams,
            onEnabled = onWheyEnabled,
            onGrams = onWheyGrams,
        )

        if (plan != null) {
            DietBody(plan)
        } else if (age != null && age !in 18..80) {
            Text(
                text = "The calorie plan is for ages 18 to 80. Your habits are unchanged.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun WheyCard(
    enabled: Boolean,
    grams: Int,
    onEnabled: (Boolean) -> Unit,
    onGrams: (Int) -> Unit,
) {
    var gramsText by remember { mutableStateOf(grams.toString()) }
    val parsed = gramsText.toIntOrNull()
    val invalid = gramsText.isNotEmpty() && (parsed == null || parsed !in 10..50)
    SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Whey protein", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "One scoop a day. Meals use less chicken and eggs by that many grams.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                PillSwitch(checked = enabled, onCheckedChange = onEnabled)
            }
            if (enabled) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { raw ->
                        gramsText = raw.filter { it.isDigit() }.take(2)
                        gramsText.toIntOrNull()?.takeIf { it in 10..50 }?.let(onGrams)
                    },
                    label = { Text("Protein in one scoop (g)") },
                    isError = invalid,
                    supportingText = { Text(if (invalid) "Use 10–50 g" else "A common scoop is about 24 g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun GramList(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        items.forEach { item ->
            Text(
                text = "•  $item",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DietBody(plan: TrainingPlan) {
    SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = plan.targetCalories.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Calories a day · maintenance about ${plan.maintenanceCalories}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = plan.pace,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "${plan.proteinGrams} g protein · ${plan.carbGrams} g carbs · ${plan.fatGrams} g fat",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (plan.wheyProteinGrams > 0) {
                Text(
                    text = "${plan.wheyProteinGrams} g of that protein is one scoop of whey. The meals cover the rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                text = plan.water,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    Text(
        text = "For the day",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
    SoftCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        GramList(plan.plate, Modifier.padding(16.dp))
    }
    Text(
        text = "Meals",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
    plan.meals.forEach { meal ->
        SoftCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(meal.name, style = MaterialTheme.typography.titleMedium)
                GramList(meal.items, Modifier.padding(top = 8.dp))
            }
        }
    }

    Text(
        text = "A starting estimate for a healthy adult. If you are pregnant, injured, or being treated for an illness, use a clinician's plan instead.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
