package com.disciplinescore.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.disciplinescore.app.ui.InsightTone
import com.disciplinescore.app.ui.InsightsUi
import com.disciplinescore.app.ui.components.ScreenHeader
import com.disciplinescore.app.ui.components.SoftCard

@Composable
fun InsightsScreen(
    state: InsightsUi,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        ScreenHeader(
            title = "Insights",
            subtitle = "Where consistency slips. A missed day becomes a pattern only if it keeps happening.",
            onOpenSettings = onOpenSettings,
            onOpenProfile = onOpenProfile,
        )
        state.cards.forEach { card ->
            val tone = when (card.tone) {
                InsightTone.CALM -> MaterialTheme.colorScheme.primary
                InsightTone.FOCUS -> MaterialTheme.colorScheme.tertiary
                InsightTone.STEADY -> MaterialTheme.colorScheme.secondary
            }
            SoftCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .padding(start = 12.dp, top = 16.dp, bottom = 16.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(tone),
                    )
                    Column(Modifier.padding(start = 14.dp, end = 18.dp, top = 16.dp, bottom = 16.dp)) {
                        Text(text = card.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = card.body,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
