package com.disciplinescore.app.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.disciplinescore.app.data.UserPreferences
import com.disciplinescore.app.domain.bodySummary
import com.disciplinescore.app.domain.workoutKey
import com.disciplinescore.app.ui.components.AppBottomBar
import com.disciplinescore.app.ui.components.BarItem
import com.disciplinescore.app.ui.calendar.CalendarScreen
import com.disciplinescore.app.ui.habits.HabitDraft
import com.disciplinescore.app.ui.habits.HabitEditorSheet
import com.disciplinescore.app.ui.habits.HabitsScreen
import com.disciplinescore.app.ui.insights.InsightsScreen
import com.disciplinescore.app.ui.plan.DietScreen
import com.disciplinescore.app.ui.plan.WorkoutScreen
import com.disciplinescore.app.ui.profile.ProfileScreen
import com.disciplinescore.app.ui.settings.SettingsScreen
import com.disciplinescore.app.ui.today.TodayScreen
import com.disciplinescore.app.ui.week.WeekScreen

private const val SETTINGS = "Settings"
private const val PROFILE = "Profile"

private enum class Dest(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.DateRange),
    Diet("Diet", Icons.Filled.Restaurant),
    Workout("Workout", Icons.Filled.FitnessCenter),
    Calendar("Month", Icons.Filled.CalendarMonth),
    Week("Week", Icons.Filled.BarChart),
    Insights("Insights", Icons.Filled.Lightbulb),
    Habits("Habits", Icons.Filled.Checklist),
}

@Composable
fun DisciplineApp(viewModel: DisciplineViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    if (!preferences.profileComplete) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            ProfileScreen(
                initial = preferences,
                onboarding = true,
                onSave = viewModel::saveProfile,
                onBack = null,
                modifier = Modifier.padding(padding),
            )
        }
    } else {
        DisciplineHome(viewModel = viewModel, preferences = preferences)
    }
}

@Composable
private fun DisciplineHome(viewModel: DisciplineViewModel, preferences: UserPreferences) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val hideBar = route == SETTINGS || route == PROFILE
    val current = Dest.values().firstOrNull { it.name == route } ?: Dest.Today
    val openSettings = {
        navController.navigate(SETTINGS) { launchSingleTop = true }
    }
    val openProfile = {
        navController.navigate(PROFILE) { launchSingleTop = true }
    }
    var draft by remember { mutableStateOf<HabitDraft?>(null) }
    val context = LocalContext.current
    val notificationsEnabled = rememberNotificationsEnabled()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.undo.collect { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.performUndo(action)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!hideBar) {
                val items = Dest.values()
                AppBottomBar(
                    items = items.map { BarItem(it.label, it.icon) },
                    selectedIndex = items.indexOf(current).coerceAtLeast(0),
                    onSelect = { index ->
                        val dest = items[index]
                        navController.navigate(dest.name) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Today.name,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 18 }
            },
            exitTransition = {
                fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 24 }
            },
            popEnterTransition = {
                fadeIn(tween(240)) + slideInVertically(tween(280)) { -it / 18 }
            },
            popExitTransition = {
                fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 24 }
            },
        ) {
            composable(Dest.Today.name) {
                TodayScreen(
                    state = state.today,
                    displayName = preferences.displayName,
                    bodyLine = bodySummary(
                        preferences.heightCm,
                        preferences.weightKg,
                        preferences.measureSystem,
                    ),
                    haptics = preferences.haptics,
                    sinkCompleted = preferences.sinkCompleted,
                    onToggle = viewModel::toggle,
                    onSkip = viewModel::skipHabit,
                    onRestore = viewModel::restoreHabit,
                    onPrevious = { viewModel.shiftDay(-1) },
                    onNext = { viewModel.shiftDay(1) },
                    onToday = viewModel::jumpToToday,
                    onOpenProfile = openProfile,
                    onOpenSettings = openSettings,
                    trainingGoal = preferences.trainingGoal,
                    workoutDays = preferences.workoutDays,
                )
            }
            composable(Dest.Calendar.name) {
                CalendarScreen(
                    state = state.calendar,
                    haptics = preferences.haptics,
                    onOpenSettings = openSettings,
                    onOpenProfile = openProfile,
                    onPreviousMonth = { viewModel.shiftMonth(-1) },
                    onNextMonth = { viewModel.shiftMonth(1) },
                    onThisMonth = viewModel::jumpToThisMonth,
                    onSelect = viewModel::selectCalendarDay,
                    onToggle = viewModel::toggleCalendarHabit,
                    onSkip = viewModel::skipCalendarHabit,
                    onRestore = viewModel::restoreCalendarHabit,
                    onOpenDay = {
                        viewModel.showCalendarDay()
                        navController.navigate(Dest.Today.name) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Dest.Week.name) {
                WeekScreen(
                    state = state.week,
                    onOpenSettings = openSettings,
                    onOpenProfile = openProfile,
                    onPrevious = { viewModel.shiftWeek(-1) },
                    onNext = { viewModel.shiftWeek(1) },
                    onCurrent = viewModel::jumpToThisWeek,
                )
            }
            composable(Dest.Insights.name) {
                InsightsScreen(
                    state.insights,
                    onOpenSettings = openSettings,
                    onOpenProfile = openProfile,
                )
            }
            composable(Dest.Habits.name) {
                HabitsScreen(
                    habits = state.habitList,
                    onAdd = { draft = viewModel.newDraft() },
                    onAddSuggestion = viewModel::addSuggestion,
                    onEdit = { id -> draft = viewModel.draftFor(id) },
                    onMove = viewModel::moveHabit,
                    onOpenSettings = openSettings,
                    onOpenProfile = openProfile,
                )
            }
            composable(Dest.Diet.name) {
                DietScreen(
                    preferences = preferences,
                    onActivity = { level -> viewModel.updatePreferences { it.copy(activity = level) } },
                    onTrainingGoal = { goal -> viewModel.updatePreferences { it.copy(trainingGoal = goal) } },
                    onWheyEnabled = { enabled -> viewModel.updatePreferences { it.copy(wheyEnabled = enabled) } },
                    onWheyGrams = { grams -> viewModel.updatePreferences { it.copy(wheyGrams = grams) } },
                    onOpenProfile = openProfile,
                    onOpenSettings = openSettings,
                )
            }
            composable(Dest.Workout.name) {
                WorkoutScreen(
                    goal = preferences.trainingGoal,
                    trainingDays = preferences.workoutDays,
                    customDays = preferences.customWorkouts[workoutKey(preferences.trainingGoal, preferences.workoutDays)],
                    blockStart = preferences.workoutStartedOn,
                    scheduleLength = preferences.scheduleLength,
                    block = preferences.workoutBlock,
                    onGenerate = viewModel::generateNextWorkout,
                    onTrainingDays = { days -> viewModel.updatePreferences { it.copy(workoutDays = days) } },
                    onTrainingGoal = { goal -> viewModel.updatePreferences { it.copy(trainingGoal = goal) } },
                    onCustomDays = { days ->
                        val key = workoutKey(preferences.trainingGoal, preferences.workoutDays)
                        viewModel.updatePreferences { current ->
                            val next = if (days == null) {
                                current.customWorkouts - key
                            } else {
                                current.customWorkouts + (key to days)
                            }
                            current.copy(customWorkouts = next)
                        }
                    },
                    onOpenProfile = openProfile,
                    onOpenSettings = openSettings,
                )
            }
            composable(SETTINGS) {
                SettingsScreen(
                    preferences = preferences,
                    onTheme = { mode -> viewModel.updatePreferences { it.copy(theme = mode) } },
                    onAccent = { accent -> viewModel.updatePreferences { it.copy(accent = accent) } },
                    onOpenProfile = openProfile,
                    onHaptics = { enabled -> viewModel.updatePreferences { it.copy(haptics = enabled) } },
                    onSinkCompleted = { enabled -> viewModel.updatePreferences { it.copy(sinkCompleted = enabled) } },
                    onDefaultReminder = { minutes ->
                        viewModel.updatePreferences { it.copy(defaultReminderMinutes = minutes) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(PROFILE) {
                ProfileScreen(
                    initial = preferences,
                    onboarding = false,
                    onSave = { draft ->
                        viewModel.saveProfile(draft)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    draft?.let { currentDraft ->
        HabitEditorSheet(
            draft = currentDraft,
            notificationsEnabled = notificationsEnabled,
            onChange = { draft = it },
            onSave = {
                viewModel.save(currentDraft)
                draft = null
            },
            onDelete = if (currentDraft.id == 0L) {
                null
            } else {
                {
                    viewModel.deleteHabit(currentDraft.id)
                    draft = null
                }
            },
            onDismiss = { draft = null },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= 33) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    },
                )
            },
        )
    }
}

@Composable
private fun rememberNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}
