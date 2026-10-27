package com.disciplinescore.app.domain

import kotlin.math.min
import kotlin.math.roundToInt

enum class BodySex(val label: String) {
    FEMALE("Female"),
    MALE("Male"),
}

enum class ActivityLevel(val label: String, val detail: String, val multiplier: Double) {
    SEDENTARY("Desk", "Little walking", 1.2),
    LIGHT("Light", "Walks most days", 1.375),
    MODERATE("Moderate", "Training 3–5 days", 1.55),
    ACTIVE("Active", "Hard training most days", 1.725),
}

enum class TrainingGoal(val label: String) {
    CUTTING("Cutting"),
    BULKING("Bulking"),
}

data class MealPlan(val name: String, val items: List<String>)

data class GymDay(val title: String, val exercises: List<String>, val training: Boolean = true)

const val MIN_TRAINING_DAYS = 1
const val MAX_TRAINING_DAYS = 6

data class TrainingPlan(
    val maintenanceCalories: Int,
    val targetCalories: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbGrams: Int,
    val deficitApplied: Boolean,
    val pace: String,
    val water: String,
    val plate: List<String>,
    val wheyProteinGrams: Int,
    val meals: List<MealPlan>,
    val days: List<GymDay>,
    val trainingNote: String,
)

private const val KCAL_PER_KG = 7700.0

/**
 * A starting diet and gym week from height, weight, age, and sex.
 * Targets never go under a floor, and ages under 18 get no plan.
 */
fun buildTrainingPlan(
    heightCm: Float,
    weightKg: Float,
    age: Int,
    sex: BodySex,
    activity: ActivityLevel,
    goal: TrainingGoal,
    trainingDays: Int = 4,
    wheyProteinGrams: Int = 0,
): TrainingPlan? {
    if (!heightCm.inHeightRange() || !weightKg.inWeightRange()) return null
    if (age !in 18..80) return null
    val bmr = bmr(weightKg, heightCm, age, sex)
    val maintenanceExact = bmr * activity.multiplier
    val maintenance = maintenanceExact.roundToInt()
    val floor = if (sex == BodySex.FEMALE) 1600 else 1800
    val rawTarget = when (goal) {
        TrainingGoal.CUTTING -> maintenanceExact - min(400.0, maintenanceExact * 0.15)
        TrainingGoal.BULKING -> maintenanceExact + min(300.0, maintenanceExact * 0.10)
    }.roundToInt()
    val target = maxOf(rawTarget, floor)
    val deficitApplied = goal == TrainingGoal.CUTTING && target < maintenance
    val (protein, fat, carbs) = macros(weightKg, target, goal)
    val whey = if (wheyProteinGrams <= 0) 0 else wheyProteinGrams.coerceIn(10, 50)
    val menu = sriLankanMenu(goal, protein, carbs, fat, whey)
    return TrainingPlan(
        maintenanceCalories = maintenance,
        targetCalories = target,
        proteinGrams = protein,
        fatGrams = fat,
        carbGrams = carbs,
        deficitApplied = deficitApplied,
        pace = pace(goal, maintenance, target),
        water = water(weightKg),
        plate = menu.plate,
        wheyProteinGrams = whey,
        meals = menu.meals,
        days = buildWorkoutWeek(goal, trainingDays),
        trainingNote = workoutNote(goal, trainingDays),
    )
}

fun buildWorkoutWeek(goal: TrainingGoal, trainingDays: Int, block: Int = 0): List<GymDay> {
    val count = trainingDays.coerceIn(MIN_TRAINING_DAYS, MAX_TRAINING_DAYS)
    val turn = block.mod(2) == 1
    if (count == 4) {
        val week = if (goal == TrainingGoal.CUTTING) cuttingWeek() else bulkingWeek()
        return if (turn) emphasizeOtherDays(week) else week
    }
    val kinds = sessionKinds(count)
    val used = slots(count).toSet()
    val seen = mutableMapOf<SessionKind, Int>()
    var next = 0
    return weekDayNames.mapIndexed { index, name ->
        if (index !in used) {
            restDay(name, goal)
        } else {
            val kind = kinds[next]
            val occurrence = seen.getOrDefault(kind, 0)
            seen[kind] = occurrence + 1
            next += 1
            val shown = if (!turn) occurrence else if (occurrence == 0) 1 else 0
            GymDay(
                title = "$name · ${sessionLabel(kind, goal, shown, count)}",
                exercises = sessionExercises(kind, goal, shown),
            )
        }
    }
}

private fun emphasizeOtherDays(week: List<GymDay>): List<GymDay> {
    val next = week.toMutableList()
    fun trade(first: Int, second: Int) {
        val left = next[first]
        val right = next[second]
        next[first] = left.copy(
            title = "${weekdayOf(left.title)} · ${sessionOf(right.title)}",
            exercises = right.exercises,
        )
        next[second] = right.copy(
            title = "${weekdayOf(right.title)} · ${sessionOf(left.title)}",
            exercises = left.exercises,
        )
    }
    trade(0, 3)
    trade(1, 4)
    return next
}

fun workoutNote(goal: TrainingGoal, trainingDays: Int): String {
    val count = trainingDays.coerceIn(MIN_TRAINING_DAYS, MAX_TRAINING_DAYS)
    val split = when (count) {
        1 -> "One full-body day covers every major lift."
        2 -> "Two full-body days, with rest between them."
        3 -> "Push, pull, and legs, with a day between sessions."
        4 -> if (goal == TrainingGoal.CUTTING) {
            "Keep the weight on the bar and stop a rep or two before failure. Steps do the extra work, not a second hard workout."
        } else {
            "Add weight when every set hits the top of the range. Rest about two minutes on the big lifts."
        }
        5 -> "Five days: push, pull, legs, then a shorter upper and lower."
        else -> "Six lifting days and Sunday off. Drop a set if a lift feels stale."
    }
    if (count == 4) return split
    val extra = if (goal == TrainingGoal.CUTTING) {
        " Steps fill the days you do not lift."
    } else {
        " Rest days stay easy so you can add weight."
    }
    return split + extra
}

private fun bmr(weightKg: Float, heightCm: Float, age: Int, sex: BodySex): Double {
    val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
    return if (sex == BodySex.MALE) base + 5 else base - 161
}

private fun macros(weightKg: Float, calories: Int, goal: TrainingGoal): Triple<Int, Int, Int> {
    val proteinPerKg = if (goal == TrainingGoal.CUTTING) 1.8 else 1.6
    var protein = (weightKg * proteinPerKg).roundToInt()
    var fat = (weightKg * 0.8f).roundToInt()
    var carbs = carbGrams(calories, protein, fat)
    if (carbs < 100) {
        fat = (weightKg * 0.6f).roundToInt()
        carbs = carbGrams(calories, protein, fat)
    }
    if (carbs < 80) {
        protein = (weightKg * 1.6f).roundToInt()
        carbs = carbGrams(calories, protein, fat)
    }
    return Triple(protein, fat, maxOf(carbs, 0))
}

private fun carbGrams(calories: Int, protein: Int, fat: Int): Int =
    ((calories - protein * 4 - fat * 9) / 4.0).roundToInt()

private fun pace(goal: TrainingGoal, maintenance: Int, target: Int): String {
    val changeKg = (target - maintenance) * 7.0 / KCAL_PER_KG
    if (goal == TrainingGoal.CUTTING && target >= maintenance) {
        return "No deficit. This is the lowest target the app will show."
    }
    val amount = formatStat(kotlin.math.abs(changeKg))
    return if (goal == TrainingGoal.CUTTING) {
        "About $amount kg down each week."
    } else {
        "About $amount kg up each week."
    }
}

private fun water(weightKg: Float): String {
    val liters = (weightKg * 0.033f).toDouble().coerceIn(2.0, 4.0)
    return "About ${formatStat(liters)} liters of water, and more on training days."
}

private data class DayMenu(val plate: List<String>, val meals: List<MealPlan>)

private fun sriLankanMenu(
    goal: TrainingGoal,
    protein: Int,
    carbs: Int,
    fat: Int,
    wheyGrams: Int,
): DayMenu {
    val whey = wheyGrams.coerceIn(0, protein)
    val mallum = 300
    val parippu = (((carbs * 0.12) / 0.18).roundToInt() / 50 * 50).coerceIn(100, 250)
    val milk = if (protein - whey >= 8) 240 else 0
    val banana = if (whey == 0) 120 else 0
    val fixedProtein = parippu * 9 / 100 + milk * 3 / 100 + banana / 100 + mallum * 2 / 100
    val maxEggGrams = if (goal == TrainingGoal.BULKING) 300 else 200
    var eggGrams = 0
    var chickenGrams = 0
    var bestScore = Int.MAX_VALUE
    val proteinForMeat = (protein - whey - fixedProtein).coerceAtLeast(0)
    var eggs = 0
    while (eggs * 50 <= maxEggGrams) {
        var chicken = 0
        while (chicken <= 800) {
            val got = eggs * 6 + chicken * 25 / 100
            val over = got - proteinForMeat
            var score = kotlin.math.abs(over)
            if (over > 12) score += 40
            if (eggs == 0 && proteinForMeat >= 12) score += 8
            if (score < bestScore) {
                bestScore = score
                eggGrams = eggs * 50
                chickenGrams = chicken
            }
            chicken += 50
        }
        eggs += 1
    }
    val fixedCarbs = parippu * 18 / 100 + milk * 5 / 100 + banana * 23 / 100 + mallum * 4 / 100
    val rice = (((carbs - fixedCarbs).coerceAtLeast(0) / 0.28).roundToInt() / 10 * 10).coerceIn(0, 900)
    val cookedFat = eggGrams * 10 / 100 + chickenGrams * 4 / 100 + parippu * 3 / 100 + milk * 3 / 100
    val oil = (((fat - cookedFat).coerceAtLeast(0) + 2) / 5 * 5).coerceIn(0, 30)
    val plate = listOfNotNull(
        eggLine(eggGrams),
        gramLine(chickenGrams, "chicken"),
        gramLine(rice, "cooked rice"),
        gramLine(parippu, "parippu"),
        gramLine(milk, "milk"),
        gramLine(mallum, "mallum"),
        gramLine(banana, "banana"),
        gramLine(oil, "coconut oil"),
        gramLine(whey, "whey protein"),
    )
    val breakfastRice = if (rice >= 400) 150 else 0
    val lunchRice = ((rice - breakfastRice) / 20) * 10
    val dinnerRice = rice - breakfastRice - lunchRice
    val lunchChicken = if (chickenGrams == 0) 0 else ((chickenGrams / 50 + 1) / 2) * 50
    val dinnerChicken = chickenGrams - lunchChicken
    val lunchParippu = (parippu / 100) * 50
    val dinnerParippu = parippu - lunchParippu
    return DayMenu(
        plate = plate,
        meals = listOf(
            MealPlan(
                "Breakfast",
                listOfNotNull(
                    eggLine(eggGrams),
                    gramLine(breakfastRice, "cooked rice"),
                    gramLine(milk, "milk"),
                ),
            ),
            MealPlan(
                "Lunch",
                listOfNotNull(
                    gramLine(lunchRice, "cooked rice"),
                    gramLine(lunchChicken, "chicken"),
                    gramLine(lunchParippu, "parippu"),
                    gramLine(150, "mallum"),
                    gramLine(oil, "coconut oil"),
                ),
            ),
            MealPlan(
                "Dinner",
                listOfNotNull(
                    gramLine(dinnerRice, "cooked rice"),
                    gramLine(dinnerChicken, "chicken"),
                    gramLine(dinnerParippu, "parippu"),
                    gramLine(150, "mallum"),
                ),
            ),
            MealPlan(
                "Snack",
                listOfNotNull(if (whey > 0) gramLine(whey, "whey protein") else gramLine(banana, "banana")),
            ),
        ),
    )
}

private fun gramLine(grams: Int, food: String): String? = if (grams <= 0) null else "$grams g $food"

private fun eggLine(grams: Int): String? = when (val count = grams / 50) {
    0 -> null
    1 -> "1 egg"
    else -> "$count eggs"
}

private val weekDayNames = listOf(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday",
)

private enum class SessionKind { FULL, UPPER, LOWER, PUSH, PULL, LEGS }

private fun slots(count: Int): List<Int> = when (count) {
    1 -> listOf(0)
    2 -> listOf(0, 3)
    3 -> listOf(0, 2, 4)
    5 -> listOf(0, 1, 2, 4, 5)
    else -> listOf(0, 1, 2, 3, 4, 5)
}

private fun sessionKinds(count: Int): List<SessionKind> = when (count) {
    1 -> listOf(SessionKind.FULL)
    2 -> listOf(SessionKind.FULL, SessionKind.FULL)
    3 -> listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS)
    5 -> listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS, SessionKind.UPPER, SessionKind.LOWER)
    else -> listOf(
        SessionKind.PUSH,
        SessionKind.PULL,
        SessionKind.LEGS,
        SessionKind.PUSH,
        SessionKind.PULL,
        SessionKind.LEGS,
    )
}

private fun sessionLabel(kind: SessionKind, goal: TrainingGoal, occurrence: Int, count: Int): String {
    val repeat = occurrence > 0
    return when (kind) {
        SessionKind.FULL -> when {
            count == 1 -> "Full body"
            repeat -> "Full body B"
            else -> "Full body A"
        }
        SessionKind.UPPER -> when {
            goal == TrainingGoal.BULKING && !repeat -> "Upper strength"
            goal == TrainingGoal.BULKING -> "Upper volume"
            else -> "Upper"
        }
        SessionKind.LOWER -> when {
            goal == TrainingGoal.BULKING && !repeat -> "Lower strength"
            goal == TrainingGoal.BULKING -> "Lower volume"
            else -> "Lower"
        }
        SessionKind.PUSH -> if (repeat) "Push B" else "Push"
        SessionKind.PULL -> if (repeat) "Pull B" else "Pull"
        SessionKind.LEGS -> if (repeat) "Legs B" else "Legs"
    }
}

private fun sessionExercises(kind: SessionKind, goal: TrainingGoal, occurrence: Int): List<String> {
    val bulking = goal == TrainingGoal.BULKING
    val volume = occurrence > 0
    return when (kind) {
        SessionKind.FULL -> if (!volume && !bulking) {
            listOf(
                "Squat or leg press — 3×8–10",
                "Bench press or push-up — 3×8–10",
                "Chest-supported row — 3×8–12",
                "Romanian deadlift — 3×8",
                "Plank — 2×30 seconds",
            )
        } else if (!volume) {
            listOf(
                "Back squat — 4×5",
                "Bench press — 4×5",
                "Barbell row — 4×5",
                "Romanian deadlift — 3×6",
                "Overhead press — 3×6",
            )
        } else if (!bulking) {
            listOf(
                "Romanian or trap-bar deadlift — 3×6–8",
                "Overhead press — 3×8",
                "Lat pulldown — 3×10",
                "Walking lunge — 2×8 each leg",
                "Calf raise — 3×12",
            )
        } else {
            listOf(
                "Front squat or leg press — 4×6",
                "Incline press — 3×6–8",
                "Weighted pull-up or lat pulldown — 4×6",
                "Hip thrust — 3×8",
                "Lateral raise — 3×10",
            )
        }
        SessionKind.UPPER -> if (bulking && volume) {
            listOf(
                "Incline press — 3×8",
                "Chest-supported row — 3×8",
                "Dumbbell shoulder press — 3×8",
                "Lat pulldown — 3×10",
                "Dip or push-up — 2×8–12",
            )
        } else if (bulking) {
            listOf(
                "Bench press — 4×5",
                "Barbell row — 4×5",
                "Overhead press — 3×6",
                "Weighted pull-up or lat pulldown — 3×6–8",
                "Lateral raise — 3×10",
            )
        } else if (volume) {
            listOf(
                "Incline dumbbell press — 3×8–12",
                "Seated cable row — 3×10",
                "Pull-up or assisted pull-up — 3×6–10",
                "Dumbbell shoulder press — 2×10",
                "Curl and triceps extension — 2×12",
            )
        } else {
            listOf(
                "Bench press — 3×6–8",
                "Chest-supported row — 3×8–10",
                "Overhead press — 3×8",
                "Lat pulldown — 3×10",
                "Lateral raise — 2×12–15",
            )
        }
        SessionKind.LOWER -> if (bulking && volume) {
            listOf(
                "Front squat or hack squat — 3×8",
                "Hip thrust — 3×8",
                "Bulgarian split squat — 3×8 each leg",
                "Leg curl — 3×10",
                "Back extension — 2×10",
            )
        } else if (bulking) {
            listOf(
                "Back squat — 4×5",
                "Romanian deadlift — 3×6–8",
                "Leg press — 3×8",
                "Leg curl — 3×8–10",
                "Calf raise — 3×8",
            )
        } else if (volume) {
            listOf(
                "Romanian or trap-bar deadlift — 3×5–8",
                "Bulgarian split squat — 3×8 each leg",
                "Hip thrust — 3×10",
                "Leg extension — 2×12",
                "Calf raise — 3×12",
            )
        } else {
            listOf(
                "Squat or leg press — 3×6–8",
                "Romanian deadlift — 3×8",
                "Walking lunge — 2×10 each leg",
                "Leg curl — 3×10–12",
                "Calf raise — 3×12",
            )
        }
        SessionKind.PUSH -> if (bulking && volume) {
            listOf(
                "Incline press — 4×8",
                "Dumbbell shoulder press — 3×8",
                "Dip — 3×8–12",
                "Lateral raise — 3×12",
                "Triceps extension — 3×10",
            )
        } else if (bulking) {
            listOf(
                "Bench press — 4×5",
                "Overhead press — 3×6",
                "Dip — 3×6–8",
                "Lateral raise — 3×10",
                "Triceps extension — 3×8",
            )
        } else if (volume) {
            listOf(
                "Incline dumbbell press — 3×10–12",
                "Dumbbell shoulder press — 3×10",
                "Push-up — 3×8–15",
                "Lateral raise — 2×15",
                "Triceps extension — 2×12",
            )
        } else {
            listOf(
                "Bench press — 3×6–8",
                "Incline dumbbell press — 3×8–12",
                "Overhead press — 3×8",
                "Lateral raise — 2×12–15",
                "Triceps extension — 2×12",
            )
        }
        SessionKind.PULL -> if (bulking && volume) {
            listOf(
                "Chest-supported row — 4×8",
                "Lat pulldown — 3×10",
                "Seated cable row — 3×10",
                "Face pull — 3×12",
                "Curl — 3×10",
            )
        } else if (bulking) {
            listOf(
                "Barbell row — 4×5",
                "Weighted pull-up or lat pulldown — 4×6",
                "Seated cable row — 3×8",
                "Face pull — 3×12",
                "Curl — 3×8",
            )
        } else if (volume) {
            listOf(
                "Seated cable row — 3×10–12",
                "Lat pulldown — 3×12",
                "Straight-arm pulldown — 2×12",
                "Face pull — 2×15",
                "Curl — 2×12",
            )
        } else {
            listOf(
                "Chest-supported row — 3×8–10",
                "Lat pulldown — 3×10",
                "Pull-up or assisted pull-up — 3×6–10",
                "Face pull — 2×12",
                "Curl — 2×12",
            )
        }
        SessionKind.LEGS -> if (bulking && volume) {
            listOf(
                "Front squat or hack squat — 4×8",
                "Hip thrust — 3×8",
                "Bulgarian split squat — 3×8 each leg",
                "Leg curl — 3×10",
                "Calf raise — 3×10",
            )
        } else if (bulking) {
            listOf(
                "Back squat — 4×5",
                "Romanian deadlift — 3×6–8",
                "Leg press — 3×8",
                "Leg curl — 3×8–10",
                "Calf raise — 3×8",
            )
        } else if (volume) {
            listOf(
                "Walking lunge — 3×10 each leg",
                "Leg press — 3×12",
                "Leg curl — 3×12",
                "Hip thrust — 2×12",
                "Calf raise — 3×15",
            )
        } else {
            listOf(
                "Squat or leg press — 3×6–8",
                "Romanian deadlift — 3×8",
                "Walking lunge — 2×10 each leg",
                "Leg curl — 3×10–12",
                "Calf raise — 3×12",
            )
        }
    }
}

private fun restDay(dayName: String, goal: TrainingGoal): GymDay {
    val easy = goal == TrainingGoal.CUTTING && dayName != "Sunday"
    return if (easy) {
        GymDay(
            "$dayName · Easy",
            listOf("8–10,000 steps, or 20 easy minutes on a bike. Leave the hard effort for the lifting days."),
            training = false,
        )
    } else if (goal == TrainingGoal.CUTTING) {
        GymDay("$dayName · Rest", listOf("Full rest. Sleep is part of the cut."), training = false)
    } else {
        GymDay(
            "$dayName · Rest",
            listOf("Rest. Eat the surplus, including on days you do not lift."),
            training = false,
        )
    }
}

private fun cuttingWeek(): List<GymDay> = listOf(
    GymDay(
        "Monday · Upper",
        listOf(
            "Bench press — 3×6–8",
            "Chest-supported row — 3×8–10",
            "Overhead press — 3×8",
            "Lat pulldown — 3×10",
            "Lateral raise — 2×12–15",
        ),
    ),
    GymDay(
        "Tuesday · Lower",
        listOf(
            "Squat or leg press — 3×6–8",
            "Romanian deadlift — 3×8",
            "Walking lunge — 2×10 each leg",
            "Leg curl — 3×10–12",
            "Calf raise — 3×12",
        ),
    ),
    GymDay(
        "Wednesday · Easy",
        listOf("8–10,000 steps, or 20 easy minutes on a bike. Leave the hard effort for the lifting days."),
        training = false,
    ),
    GymDay(
        "Thursday · Upper",
        listOf(
            "Incline dumbbell press — 3×8–12",
            "Seated cable row — 3×10",
            "Pull-up or assisted pull-up — 3×6–10",
            "Dumbbell shoulder press — 2×10",
            "Curl and triceps extension — 2×12",
        ),
    ),
    GymDay(
        "Friday · Lower",
        listOf(
            "Romanian or trap-bar deadlift — 3×5–8",
            "Bulgarian split squat — 3×8 each leg",
            "Hip thrust — 3×10",
            "Leg extension — 2×12",
            "Calf raise — 3×12",
        ),
    ),
    GymDay(
        "Saturday · Walk",
        listOf("A long walk or an easy sport. Keep it light enough that Monday's first sets still feel strong."),
        training = false,
    ),
    GymDay("Sunday · Rest", listOf("Full rest. Sleep is part of the cut."), training = false),
)

private fun bulkingWeek(): List<GymDay> = listOf(
    GymDay(
        "Monday · Upper strength",
        listOf(
            "Bench press — 4×5",
            "Barbell row — 4×5",
            "Overhead press — 3×6",
            "Weighted pull-up or lat pulldown — 3×6–8",
            "Lateral raise — 3×10",
        ),
    ),
    GymDay(
        "Tuesday · Lower strength",
        listOf(
            "Back squat — 4×5",
            "Romanian deadlift — 3×6–8",
            "Leg press — 3×8",
            "Leg curl — 3×8–10",
            "Calf raise — 3×8",
        ),
    ),
    GymDay(
        "Wednesday · Rest",
        listOf("Walk if you like. Extra cardio is optional while you are trying to gain."),
        training = false,
    ),
    GymDay(
        "Thursday · Upper volume",
        listOf(
            "Incline press — 3×8",
            "Chest-supported row — 3×8",
            "Dumbbell shoulder press — 3×8",
            "Lat pulldown — 3×10",
            "Dip or push-up — 2×8–12",
        ),
    ),
    GymDay(
        "Friday · Lower volume",
        listOf(
            "Front squat or hack squat — 3×8",
            "Hip thrust — 3×8",
            "Bulgarian split squat — 3×8 each leg",
            "Leg curl — 3×10",
            "Back extension — 2×10",
        ),
    ),
    GymDay(
        "Saturday · Rest",
        listOf("Rest. Eat the surplus, including on days you do not lift."),
        training = false,
    ),
    GymDay(
        "Sunday · Rest",
        listOf("Rest. Next week, add a little weight only on lifts that hit the top of the range."),
        training = false,
    ),
)
