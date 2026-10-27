package com.disciplinescore.app.domain

const val MAX_EXERCISES_PER_DAY = 12

private const val EXERCISE_SEPARATOR = " — "
private val prescriptionPattern = Regex("^[\\p{L}\\p{N} ×–\\-]+$")
private val sessionLabelPattern = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} '&+-]*$")

enum class ExerciseKind { COMPOUND, ISOLATION, HOLD, CARDIO }

enum class ExerciseCategory(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    LEGS("Legs"),
    CORE("Core"),
    CARDIO("Cardio"),
}

data class CatalogExercise(
    val name: String,
    val kind: ExerciseKind,
    val category: ExerciseCategory,
    val selectable: Boolean = true,
)

data class ExerciseGroup(val category: ExerciseCategory, val exercises: List<CatalogExercise>)

fun workoutKey(goal: TrainingGoal, trainingDays: Int): String =
    "${goal.name}:${trainingDays.coerceIn(MIN_TRAINING_DAYS, MAX_TRAINING_DAYS)}"

fun exerciseName(line: String): String = line.substringBefore(EXERCISE_SEPARATOR).trim()

fun exerciseLine(exercise: CatalogExercise, goal: TrainingGoal): String =
    "${exercise.name}$EXERCISE_SEPARATOR${prescription(exercise.kind, goal)}"

fun prescription(kind: ExerciseKind, goal: TrainingGoal): String = when (kind) {
    ExerciseKind.COMPOUND -> if (goal == TrainingGoal.BULKING) "4×6–8" else "3×8–12"
    ExerciseKind.ISOLATION -> if (goal == TrainingGoal.BULKING) "3×8–12" else "2×12–15"
    ExerciseKind.HOLD -> "3×20–40 seconds"
    ExerciseKind.CARDIO -> "10–20 minutes"
}

fun filterExercises(
    query: String,
    exclude: Set<String> = emptySet(),
    limit: Int = Int.MAX_VALUE,
): List<CatalogExercise> {
    val needle = query.trim()
    val hidden = exclude.map { it.lowercase() }.toSet()
    val available = exerciseCatalog.filter { it.selectable && it.name.lowercase() !in hidden }
    val matched = if (needle.isEmpty()) {
        available
    } else {
        available.filter { it.name.contains(needle, ignoreCase = true) }
    }
    return matched.take(limit)
}

fun exercisesByCategory(
    query: String,
    exclude: Set<String> = emptySet(),
): List<ExerciseGroup> {
    val matched = filterExercises(query, exclude).groupBy { it.category }
    return ExerciseCategory.values().mapNotNull { category ->
        val exercises = matched[category].orEmpty()
        if (exercises.isEmpty()) null else ExerciseGroup(category, exercises)
    }
}

data class CustomDay(val label: String, val exercises: List<String>)

fun weekdayOf(title: String): String = title.substringBefore(" · ").trim()

fun sessionOf(title: String): String = title.substringAfter(" · ", title).trim()

fun snapshotWeek(template: List<GymDay>): List<CustomDay> =
    template.map { day ->
        CustomDay(
            label = if (day.training) sessionOf(day.title) else "",
            exercises = if (day.training) day.exercises else emptyList(),
        )
    }

fun filledWeek(template: List<GymDay>, custom: List<CustomDay>?): List<CustomDay> {
    val suggested = snapshotWeek(template)
    if (custom == null || custom.size != suggested.size) return suggested
    return suggested.mapIndexed { index, day ->
        val saved = custom[index]
        day.copy(
            label = saved.label.ifBlank { day.label },
            exercises = saved.exercises,
        )
    }
}

fun applyCustomExercises(template: List<GymDay>, custom: List<CustomDay>?): List<GymDay> {
    if (custom == null || custom.size != template.size) return template
    return template.mapIndexed { index, day ->
        if (!day.training) {
            day
        } else {
            val saved = custom[index]
            val label = saved.label.ifBlank { sessionOf(day.title) }
            day.copy(title = "${weekdayOf(day.title)} · $label", exercises = saved.exercises)
        }
    }
}

fun setSessionLabel(week: List<CustomDay>, dayIndex: Int, label: String): List<CustomDay> {
    val cleaned = sanitizeSessionLabel(label) ?: return week
    if (dayIndex !in week.indices) return week
    return week.mapIndexed { index, day ->
        if (index == dayIndex) day.copy(label = cleaned) else day
    }
}

fun addExercise(week: List<CustomDay>, dayIndex: Int, line: String): List<CustomDay> {
    if (dayIndex !in week.indices) return week
    val saved = sanitizeExerciseLine(line) ?: return week
    val day = week[dayIndex].exercises
    if (day.size >= MAX_EXERCISES_PER_DAY) return week
    if (day.any { exerciseName(it).equals(exerciseName(saved), ignoreCase = true) }) return week
    return week.mapIndexed { index, custom ->
        if (index == dayIndex) custom.copy(exercises = custom.exercises + saved) else custom
    }
}

fun removeExercise(week: List<CustomDay>, dayIndex: Int, exerciseIndex: Int): List<CustomDay> {
    if (dayIndex !in week.indices) return week
    val day = week[dayIndex].exercises
    if (exerciseIndex !in day.indices) return week
    return week.mapIndexed { index, custom ->
        if (index == dayIndex) {
            custom.copy(exercises = custom.exercises.filterIndexed { i, _ -> i != exerciseIndex })
        } else {
            custom
        }
    }
}

fun sanitizeSessionLabel(raw: String): String? {
    val trimmed = raw.trim().replace(Regex("\\s+"), " ")
    if (trimmed.length !in 1..24 || !sessionLabelPattern.matches(trimmed)) return null
    return trimmed
}

fun sanitizeExerciseLine(line: String): String? {
    val trimmed = line.trim()
    if (trimmed.length !in 3..80 || EXERCISE_SEPARATOR !in trimmed) return null
    val name = exerciseName(trimmed)
    val canonical = catalogByName[name.lowercase()] ?: return null
    val detail = trimmed.substringAfter(EXERCISE_SEPARATOR).trim()
    if (detail.length !in 1..32 || !prescriptionPattern.matches(detail)) return null
    return "${canonical.name}$EXERCISE_SEPARATOR$detail"
}

fun sanitizeCustomWorkouts(raw: Map<String, List<CustomDay>>): Map<String, List<CustomDay>> {
    val keys = TrainingGoal.values().flatMap { goal ->
        (MIN_TRAINING_DAYS..MAX_TRAINING_DAYS).map { days -> workoutKey(goal, days) }
    }.toSet()
    return raw.mapNotNull { (key, days) ->
        if (key !in keys || days.size != 7) return@mapNotNull null
        val cleaned = days.map { day ->
            CustomDay(
                label = sanitizeSessionLabel(day.label).orEmpty(),
                exercises = day.exercises.mapNotNull(::sanitizeExerciseLine)
                    .distinctBy { exerciseName(it).lowercase() }
                    .take(MAX_EXERCISES_PER_DAY),
            )
        }
        key to cleaned
    }.toMap()
}

private val catalogByName: Map<String, CatalogExercise> by lazy {
    exerciseCatalog.associateBy { it.name.lowercase() }
}

private val chest = listOf(
    lift("Bench press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Incline press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Incline dumbbell press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Dumbbell bench press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Decline bench press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Decline dumbbell press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Machine chest press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Floor press", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Dip", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Push-up", ExerciseKind.COMPOUND, ExerciseCategory.CHEST),
    lift("Chest fly", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Dumbbell fly", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Cable fly", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Cable crossover", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Low-to-high cable fly", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("High-to-low cable fly", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Pec deck", ExerciseKind.ISOLATION, ExerciseCategory.CHEST),
    lift("Bench press or push-up", ExerciseKind.COMPOUND, ExerciseCategory.CHEST, selectable = false),
    lift("Dip or push-up", ExerciseKind.COMPOUND, ExerciseCategory.CHEST, selectable = false),
)

private val back = listOf(
    lift("Deadlift", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Sumo deadlift", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Trap-bar deadlift", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Rack pull", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Barbell row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Pendlay row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Chest-supported row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Seated cable row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Single-arm dumbbell row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("T-bar row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Meadow row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Machine row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Inverted row", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Pull-up", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Chin-up", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Assisted pull-up", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Weighted pull-up", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Lat pulldown", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Wide-grip pulldown", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Close-grip pulldown", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Neutral-grip pulldown", ExerciseKind.COMPOUND, ExerciseCategory.BACK),
    lift("Straight-arm pulldown", ExerciseKind.ISOLATION, ExerciseCategory.BACK),
    lift("Band pull-apart", ExerciseKind.ISOLATION, ExerciseCategory.BACK),
    lift("Pull-up or assisted pull-up", ExerciseKind.COMPOUND, ExerciseCategory.BACK, selectable = false),
    lift("Weighted pull-up or lat pulldown", ExerciseKind.COMPOUND, ExerciseCategory.BACK, selectable = false),
)

private val shoulders = listOf(
    lift("Overhead press", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Dumbbell shoulder press", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Arnold press", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Machine shoulder press", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Landmine press", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Pike push-up", ExerciseKind.COMPOUND, ExerciseCategory.SHOULDERS),
    lift("Lateral raise", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Cable lateral raise", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Front raise", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Rear delt fly", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Reverse pec deck", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Face pull", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Upright row", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Shrug", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
    lift("Y-raise", ExerciseKind.ISOLATION, ExerciseCategory.SHOULDERS),
)

private val biceps = listOf(
    lift("Curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Barbell curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("EZ-bar curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Hammer curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Preacher curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Incline dumbbell curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Concentration curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Cable curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Reverse curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Spider curl", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS),
    lift("Curl and triceps extension", ExerciseKind.ISOLATION, ExerciseCategory.BICEPS, selectable = false),
)

private val triceps = listOf(
    lift("Triceps extension", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Overhead triceps extension", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Overhead cable extension", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Skull crusher", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Close-grip bench press", ExerciseKind.COMPOUND, ExerciseCategory.TRICEPS),
    lift("Cable pushdown", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Rope pushdown", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Triceps kickback", ExerciseKind.ISOLATION, ExerciseCategory.TRICEPS),
    lift("Bench dip", ExerciseKind.COMPOUND, ExerciseCategory.TRICEPS),
    lift("Diamond push-up", ExerciseKind.COMPOUND, ExerciseCategory.TRICEPS),
)

private val legs = listOf(
    lift("Back squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Front squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Goblet squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Hack squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Box squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Smith machine squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Leg press", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Romanian deadlift", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Good morning", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Hip thrust", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Glute bridge", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Bulgarian split squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Walking lunge", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Reverse lunge", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Lateral lunge", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Step-up", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Leg extension", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Leg curl", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Lying leg curl", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Seated leg curl", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Nordic curl", ExerciseKind.COMPOUND, ExerciseCategory.LEGS),
    lift("Hip abduction", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Hip adduction", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Cable kickback", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Calf raise", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Seated calf raise", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Donkey calf raise", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Leg press calf raise", ExerciseKind.ISOLATION, ExerciseCategory.LEGS),
    lift("Wall sit", ExerciseKind.HOLD, ExerciseCategory.LEGS),
    lift("Front squat or hack squat", ExerciseKind.COMPOUND, ExerciseCategory.LEGS, selectable = false),
    lift("Front squat or leg press", ExerciseKind.COMPOUND, ExerciseCategory.LEGS, selectable = false),
    lift("Squat or leg press", ExerciseKind.COMPOUND, ExerciseCategory.LEGS, selectable = false),
    lift("Romanian or trap-bar deadlift", ExerciseKind.COMPOUND, ExerciseCategory.LEGS, selectable = false),
)

private val core = listOf(
    lift("Plank", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Side plank", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Dead bug", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Bird dog", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Hollow hold", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Ab wheel", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Farmer carry", ExerciseKind.HOLD, ExerciseCategory.CORE),
    lift("Hanging knee raise", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Hanging leg raise", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Cable crunch", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Crunch", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Reverse crunch", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Bicycle crunch", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Sit-up", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Russian twist", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Cable woodchop", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Pallof press", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Toe touch", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Mountain climber", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Back extension", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
    lift("Hyperextension", ExerciseKind.ISOLATION, ExerciseCategory.CORE),
)

private val cardio = listOf(
    lift("Bike", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Air bike", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Rowing machine", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Treadmill walk", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Incline walk", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Jog", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Elliptical", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Stair climber", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Jump rope", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Ski erg", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Battle ropes", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
    lift("Burpee", ExerciseKind.CARDIO, ExerciseCategory.CARDIO),
)

val exerciseCatalog: List<CatalogExercise> = chest + back + shoulders + biceps + triceps + legs + core + cardio

private fun lift(
    name: String,
    kind: ExerciseKind,
    category: ExerciseCategory,
    selectable: Boolean = true,
) = CatalogExercise(name, kind, category, selectable)
