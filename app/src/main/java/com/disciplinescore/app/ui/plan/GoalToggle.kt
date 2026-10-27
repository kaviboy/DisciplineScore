package com.disciplinescore.app.ui.plan

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.TrainingGoal
import com.disciplinescore.app.ui.components.ChoiceTrack

@Composable
fun GoalToggle(
    goal: TrainingGoal,
    onGoal: (TrainingGoal) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val options = TrainingGoal.values()
    ChoiceTrack(
        labels = options.map { it.label },
        selectedIndex = options.indexOf(goal),
        onSelect = { onGoal(options[it]) },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
