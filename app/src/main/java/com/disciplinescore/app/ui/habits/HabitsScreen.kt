package com.disciplinescore.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.domain.HabitCategory
import com.disciplinescore.app.ui.components.ChoicePills
import com.disciplinescore.app.ui.HabitListItem
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SoftCard
import com.disciplinescore.app.ui.icon
import com.disciplinescore.app.ui.tint

@Composable
fun HabitsScreen(
    habits: List<HabitListItem>,
    onAdd: () -> Unit,
    onAddSuggestion: (HabitSuggestion) -> Unit,
    onEdit: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<HabitCategory?>(null) }
    val filtering = query.isNotBlank() || category != null
    val visible = habits.filter { habit ->
        val matchesCategory = category == null || habit.category == category
        val needle = query.trim()
        val matchesQuery = needle.isEmpty() ||
            habit.name.contains(needle, ignoreCase = true) ||
            habit.note.contains(needle, ignoreCase = true)
        matchesCategory && matchesQuery
    }
    val suggestions = habitSuggestions.filter { suggestion ->
        habits.none { it.name.equals(suggestion.name, ignoreCase = true) }
    }
    val categoriesInUse = habits.map { it.category }.distinct()
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 96.dp),
        ) {
            ScreenHeader(
                title = "Habits",
                subtitle = "Search, reorder, or tap a habit to change its days, weight, or reminder.",
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile,
            )
            if (suggestions.isNotEmpty()) {
                Text(
                    text = "Add in one tap",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = "Every day, no reminder. You can change that after.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                ChoicePills(
                    labels = suggestions.map { it.name },
                    isSelected = { false },
                    onSelect = { onAddSuggestion(suggestions[it]) },
                )
            }
            if (habits.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                if (categoriesInUse.size > 1) {
                    val filters = buildList<HabitCategory?> {
                        add(null)
                        addAll(categoriesInUse)
                    }
                    ChoicePills(
                        labels = filters.map { it?.label ?: "All" },
                        isSelected = { filters[it] == category },
                        onSelect = { index ->
                            val item = filters[index]
                            category = if (item != null && category == item) null else item
                        },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            if (habits.isEmpty()) {
                SoftCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(text = "Start with one habit", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Pick a suggestion above, or add something you can finish today.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            } else if (visible.isEmpty()) {
                Text(
                    text = "Nothing matches that search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                SoftCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column {
                        visible.forEachIndexed { index, habit ->
                            val sourceIndex = habits.indexOfFirst { it.id == habit.id }
                            HabitManageRow(
                                habit = habit,
                                canReorder = !filtering,
                                canMoveUp = sourceIndex > 0,
                                canMoveDown = sourceIndex >= 0 && sourceIndex < habits.lastIndex,
                                onClick = { onEdit(habit.id) },
                                onMoveUp = { onMove(habit.id, -1) },
                                onMoveDown = { onMove(habit.id, 1) },
                            )
                            if (index != visible.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add habit")
        }
    }
}

@Composable
private fun HabitManageRow(
    habit: HabitListItem,
    canReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = habit.category.tint()
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = habit.category.icon(),
                    contentDescription = null,
                    tint = tint,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = habit.name, style = MaterialTheme.typography.titleMedium)
                if (habit.note.isNotBlank()) {
                    Text(
                        text = habit.note,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = habit.meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (canReorder) {
            Column(Modifier.padding(end = 4.dp)) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move ${habit.name} up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move ${habit.name} down")
                }
            }
        }
    }
}
