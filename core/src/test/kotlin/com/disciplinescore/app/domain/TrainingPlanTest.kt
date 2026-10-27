package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPlanTest {
    @Test
    fun moderateMaleGetsADeficitForCuttingAndASurplusForBulking() {
        val cutting = plan(TrainingGoal.CUTTING)!!
        val bulking = plan(TrainingGoal.BULKING)!!

        assertEquals(2616, cutting.maintenanceCalories)
        assertTrue(cutting.targetCalories < cutting.maintenanceCalories)
        assertTrue(cutting.targetCalories >= 1800)
        assertTrue(cutting.deficitApplied)
        assertTrue(cutting.proteinGrams in 120..140)
        assertEquals(7, cutting.days.size)
        assertEquals(4, cutting.days.count { it.training })
        assertTrue(cutting.days[2].title.contains("Easy"))

        assertTrue(bulking.targetCalories > bulking.maintenanceCalories)
        assertEquals(7, bulking.days.size)
        assertEquals(4, bulking.days.count { it.training })
        assertTrue(bulking.days.first().exercises.first().contains("Bench press"))
        assertTrue(bulking.meals.size == 4)
        val lines = cutting.plate + cutting.meals.flatMap { it.items }
        assertTrue(lines.any { it == "1 egg" || it.endsWith(" eggs") })
        assertTrue(lines.any { it.contains("g chicken") })
        assertTrue(lines.any { it.contains("g cooked rice") })
        assertTrue(lines.any { it.contains("g parippu") })
        assertTrue(lines.filterNot { it.contains("egg") }.all { it.contains(" g ") })
        assertFalse(lines.any { "cup" in it || "piece" in it })
    }

    @Test
    fun wheyScoopLowersTheFoodOnThePlate() {
        val plain = plan(TrainingGoal.CUTTING)!!
        val withWhey = buildTrainingPlan(
            heightCm = 178f,
            weightKg = 72f,
            age = 30,
            sex = BodySex.MALE,
            activity = ActivityLevel.MODERATE,
            goal = TrainingGoal.CUTTING,
            wheyProteinGrams = 30,
        )!!

        assertEquals(30, withWhey.wheyProteinGrams)
        assertEquals(plain.proteinGrams, withWhey.proteinGrams)
        assertTrue(withWhey.plate.contains("30 g whey protein"))
        assertTrue(withWhey.plate != plain.plate)
        assertEquals(listOf("30 g whey protein"), withWhey.meals.last().items)
    }

    @Test
    fun workoutWeekFollowsTheSelectedDayCount() {
        val one = buildWorkoutWeek(TrainingGoal.CUTTING, 1)
        val three = buildWorkoutWeek(TrainingGoal.BULKING, 3)
        val six = buildWorkoutWeek(TrainingGoal.CUTTING, 6)

        assertEquals(1, one.count { it.training })
        assertTrue(one.first().title.contains("Full body"))
        assertTrue(one[2].title.contains("Easy"))
        assertFalse(one.last().training)

        assertEquals(listOf("Push", "Pull", "Legs"), three.filter { it.training }.map { it.title.substringAfter("· ").trim() })
        assertTrue(three.first().exercises.first().contains("4×5"))
        assertEquals(3, three.count { it.training })

        assertEquals(6, six.count { it.training })
        assertTrue(six[3].title.contains("Push B"))
        assertFalse(six.last().training)
        assertTrue(six.last().title.contains("Rest"))
    }

    @Test
    fun aLowEstimateDoesNotCreateADeficitUnderTheFloor() {
        val plan = buildTrainingPlan(
            heightCm = 155f,
            weightKg = 50f,
            age = 25,
            sex = BodySex.FEMALE,
            activity = ActivityLevel.SEDENTARY,
            goal = TrainingGoal.CUTTING,
        )!!

        assertTrue(plan.maintenanceCalories < 1600)
        assertEquals(1600, plan.targetCalories)
        assertFalse(plan.deficitApplied)
        assertTrue(plan.pace.contains("No deficit"))
    }

    @Test
    fun teensDoNotGetACalorieTarget() {
        assertNull(
            buildTrainingPlan(
                heightCm = 170f,
                weightKg = 60f,
                age = 16,
                sex = BodySex.FEMALE,
                activity = ActivityLevel.MODERATE,
                goal = TrainingGoal.CUTTING,
            ),
        )
    }

    private fun plan(goal: TrainingGoal) = buildTrainingPlan(
        heightCm = 178f,
        weightKg = 72f,
        age = 30,
        sex = BodySex.MALE,
        activity = ActivityLevel.MODERATE,
        goal = goal,
    )
}
