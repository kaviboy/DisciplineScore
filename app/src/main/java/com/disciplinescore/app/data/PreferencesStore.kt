package com.disciplinescore.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import com.disciplinescore.app.domain.ActivityLevel
import com.disciplinescore.app.domain.BodySex
import com.disciplinescore.app.domain.MeasureSystem
import com.disciplinescore.app.domain.MAX_TRAINING_DAYS
import com.disciplinescore.app.domain.MIN_TRAINING_DAYS
import com.disciplinescore.app.domain.ScheduleLength
import com.disciplinescore.app.domain.TrainingGoal
import com.disciplinescore.app.domain.CustomDay
import com.disciplinescore.app.domain.sanitizeCustomWorkouts
import com.disciplinescore.app.domain.inHeightRange
import com.disciplinescore.app.domain.inWeightRange
import com.disciplinescore.app.ui.theme.AccentTheme
import com.disciplinescore.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.AMBER,
    val displayName: String = "",
    val measureSystem: MeasureSystem = MeasureSystem.METRIC,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val sex: BodySex? = null,
    val birthDate: LocalDate? = null,
    val profileComplete: Boolean = false,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val wheyEnabled: Boolean = false,
    val wheyGrams: Int = 24,
    val trainingGoal: TrainingGoal = TrainingGoal.CUTTING,
    val workoutDays: Int = 4,
    val scheduleLength: ScheduleLength = ScheduleLength.SIX_WEEKS,
    val workoutStartedOn: LocalDate? = null,
    val workoutBlock: Int = 0,
    val customWorkouts: Map<String, List<CustomDay>> = emptyMap(),
    val haptics: Boolean = true,
    val sinkCompleted: Boolean = true,
    val defaultReminderMinutes: Int = 7 * 60,
)

class PreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value).sanitized()
        _state.value = next
        prefs.edit()
            .putString(KEY_THEME, next.theme.name)
            .putString(KEY_ACCENT, next.accent.name)
            .putString(KEY_NAME, next.displayName)
            .putString(KEY_MEASURE, next.measureSystem.name)
            .putOptionalFloat(KEY_HEIGHT, next.heightCm)
            .putOptionalFloat(KEY_WEIGHT, next.weightKg)
            .putOptionalString(KEY_SEX, next.sex?.name)
            .putOptionalString(KEY_BIRTH, next.birthDate?.toString())
            .putBoolean(KEY_PROFILE, next.profileComplete)
            .putString(KEY_ACTIVITY, next.activity.name)
            .putBoolean(KEY_WHEY, next.wheyEnabled)
            .putInt(KEY_WHEY_GRAMS, next.wheyGrams)
            .putString(KEY_GOAL, next.trainingGoal.name)
            .putInt(KEY_WORKOUT_DAYS, next.workoutDays)
            .putString(KEY_BLOCK_LENGTH, next.scheduleLength.name)
            .putOptionalString(KEY_BLOCK_START, next.workoutStartedOn?.toString())
            .putInt(KEY_BLOCK, next.workoutBlock)
            .putString(KEY_WORKOUTS, encodeWorkouts(next.customWorkouts))
            .putBoolean(KEY_HAPTICS, next.haptics)
            .putBoolean(KEY_SINK, next.sinkCompleted)
            .putInt(KEY_REMINDER, next.defaultReminderMinutes)
            .apply()
    }

    private fun read(): UserPreferences {
        val storedStart = prefs.getString(KEY_BLOCK_START, null)
        val loaded = UserPreferences(
        theme = enumOr(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        accent = enumOr(prefs.getString(KEY_ACCENT, null), AccentTheme.AMBER),
        displayName = prefs.getString(KEY_NAME, "").orEmpty(),
        measureSystem = enumOr(prefs.getString(KEY_MEASURE, null), MeasureSystem.METRIC),
        heightCm = prefs.getString(KEY_HEIGHT, null)?.toFloatOrNull(),
        weightKg = prefs.getString(KEY_WEIGHT, null)?.toFloatOrNull(),
        sex = prefs.getString(KEY_SEX, null)?.let { raw ->
            runCatching { BodySex.valueOf(raw) }.getOrNull()
        },
        birthDate = prefs.getString(KEY_BIRTH, null)?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        },
        profileComplete = prefs.getBoolean(KEY_PROFILE, false),
        activity = enumOr(prefs.getString(KEY_ACTIVITY, null), ActivityLevel.MODERATE),
        wheyEnabled = prefs.getBoolean(KEY_WHEY, false),
        wheyGrams = prefs.getInt(KEY_WHEY_GRAMS, 24),
        trainingGoal = enumOr(prefs.getString(KEY_GOAL, null), TrainingGoal.CUTTING),
        workoutDays = prefs.getInt(KEY_WORKOUT_DAYS, 4),
        scheduleLength = enumOr(prefs.getString(KEY_BLOCK_LENGTH, null), ScheduleLength.SIX_WEEKS),
        workoutStartedOn = storedStart?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        },
        workoutBlock = prefs.getInt(KEY_BLOCK, 0),
        customWorkouts = decodeWorkouts(prefs.getString(KEY_WORKOUTS, null)),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        sinkCompleted = prefs.getBoolean(KEY_SINK, true),
        defaultReminderMinutes = prefs.getInt(KEY_REMINDER, 7 * 60),
        ).sanitized()
        if (loaded.profileComplete && storedStart == null) {
            val started = loaded.copy(workoutStartedOn = LocalDate.now())
            prefs.edit().putString(KEY_BLOCK_START, started.workoutStartedOn.toString()).apply()
            return started
        }
        return loaded
    }

    private fun UserPreferences.sanitized() = copy(
        displayName = displayName.trim().take(24),
        heightCm = heightCm?.takeIf { it.inHeightRange() },
        weightKg = weightKg?.takeIf { it.inWeightRange() },
        birthDate = birthDate?.takeIf { date ->
            !date.isAfter(LocalDate.now()) && !date.isBefore(LocalDate.now().minusYears(120))
        },
        defaultReminderMinutes = defaultReminderMinutes.coerceIn(0, 23 * 60 + 59),
        workoutDays = workoutDays.coerceIn(MIN_TRAINING_DAYS, MAX_TRAINING_DAYS),
        wheyGrams = wheyGrams.coerceIn(10, 50),
        workoutBlock = workoutBlock.coerceAtLeast(0),
        workoutStartedOn = workoutStartedOn?.takeIf { date ->
            !date.isAfter(LocalDate.now().plusDays(1)) && !date.isBefore(LocalDate.now().minusYears(5))
        },
        customWorkouts = sanitizeCustomWorkouts(customWorkouts),
    )

    private companion object {
        const val FILE = "discipline_prefs"
        const val KEY_THEME = "theme"
        const val KEY_ACCENT = "accent"
        const val KEY_NAME = "display_name"
        const val KEY_MEASURE = "measure_system"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_SEX = "body_sex"
        const val KEY_BIRTH = "birth_date"
        const val KEY_PROFILE = "profile_complete"
        const val KEY_ACTIVITY = "activity"
        const val KEY_WHEY = "whey_enabled"
        const val KEY_WHEY_GRAMS = "whey_grams"
        const val KEY_GOAL = "training_goal"
        const val KEY_WORKOUT_DAYS = "workout_days"
        const val KEY_BLOCK_LENGTH = "schedule_length"
        const val KEY_BLOCK_START = "workout_started_on"
        const val KEY_BLOCK = "workout_block"
        const val KEY_WORKOUTS = "custom_workouts"
        const val KEY_HAPTICS = "haptics"
        const val KEY_SINK = "sink_completed"
        const val KEY_REMINDER = "default_reminder"
    }
}

private fun SharedPreferences.Editor.putOptionalFloat(key: String, value: Float?): SharedPreferences.Editor =
    if (value == null) remove(key) else putString(key, value.toString())

private fun SharedPreferences.Editor.putOptionalString(key: String, value: String?): SharedPreferences.Editor =
    if (value == null) remove(key) else putString(key, value)

private fun encodeWorkouts(value: Map<String, List<CustomDay>>): String {
    val root = JSONObject()
    value.forEach { (key, days) ->
        val week = JSONArray()
        days.forEach { custom ->
            val exercises = JSONArray()
            custom.exercises.forEach { exercises.put(it) }
            week.put(
                JSONObject()
                    .put("label", custom.label)
                    .put("exercises", exercises),
            )
        }
        root.put(key, week)
    }
    return root.toString()
}

private fun decodeWorkouts(raw: String?): Map<String, List<CustomDay>> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val root = JSONObject(raw)
        val parsed = mutableMapOf<String, List<CustomDay>>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val week = root.optJSONArray(key) ?: continue
            parsed[key] = List(week.length()) { dayIndex -> decodeCustomDay(week.opt(dayIndex)) }
        }
        sanitizeCustomWorkouts(parsed)
    }.getOrDefault(emptyMap())
}

private fun decodeCustomDay(raw: Any?): CustomDay = when (raw) {
    is JSONArray -> CustomDay("", List(raw.length()) { raw.optString(it) })
    is JSONObject -> {
        val exercises = raw.optJSONArray("exercises")
        CustomDay(
            label = raw.optString("label"),
            exercises = if (exercises == null) emptyList() else List(exercises.length()) { exercises.optString(it) },
        )
    }
    else -> CustomDay("", emptyList())
}

private inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
    raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
