package com.disciplinescore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disciplinescore.app.ui.DisciplineApp
import com.disciplinescore.app.ui.DisciplineViewModel
import com.disciplinescore.app.ui.theme.DisciplineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DisciplineViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            DisciplineTheme(mode = preferences.theme, accent = preferences.accent) {
                DisciplineApp(viewModel)
            }
        }
    }
}
