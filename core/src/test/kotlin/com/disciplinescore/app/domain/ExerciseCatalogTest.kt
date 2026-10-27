package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun searchFindsALiftAndSkipsOnesAlreadyOnTheDay() {
        val matches = filterExercises("bench", exclude = setOf("Bench press"))
        assertTrue(matches.none { it.name == "Bench press" })
        assertEquals(
            listOf(
                "Dumbbell bench press",
                "Decline bench press",
                "Close-grip bench press",
                "Bench dip",
            ),
            matches.map { it.name },
        )
    }

    @Test
    fun everyCategoryListsItsExercises() {
        val groups = exercisesByCategory("")
        assertEquals(ExerciseCategory.values().toList(), groups.map { it.category })
        val names = groups.flatMap { group -> group.exercises.map { it.name } }
        assertEquals(names.size, names.distinct().size)
        assertTrue(names.size > 40)
        assertTrue(groups.all { group -> group.exercises.all { it.category == group.category } })
        assertTrue(names.none { " or " in it })
    }

    @Test
    fun addingAndRemovingKeepsTheRestOfTheWeek() {
        val week = List(7) { CustomDay("", emptyList()) }
        val added = addExercise(week, 0, exerciseLine(exerciseCatalog.first { it.name == "Bench press" }, TrainingGoal.CUTTING))
        assertEquals("Bench press", exerciseName(added[0].exercises.single()))
        assertEquals(1, added[0].exercises.size)
        assertTrue(added.drop(1).all { it.exercises.isEmpty() })

        val duplicate = addExercise(added, 0, "Bench press — 4×6–8")
        assertEquals(1, duplicate[0].exercises.size)

        val removed = removeExercise(duplicate, 0, 0)
        assertTrue(removed[0].exercises.isEmpty())
    }

    @Test
    fun customExercisesReplaceOnlyLiftingDays() {
        val template = buildWorkoutWeek(TrainingGoal.CUTTING, 1)
        val custom = snapshotWeek(template).mapIndexed { index, day ->
            if (index == 0) day.copy(exercises = listOf("Deadlift — 3×5")) else day
        }
        val shown = applyCustomExercises(template, custom)
        assertEquals("Deadlift — 3×5", shown.first().exercises.single())
        assertEquals(template.last().exercises, shown.last().exercises)
    }

    @Test
    fun sessionNameCanBeRenamed() {
        val template = buildWorkoutWeek(TrainingGoal.CUTTING, 3)
        val renamed = setSessionLabel(snapshotWeek(template), 0, "  Chest day ")
        val shown = applyCustomExercises(template, renamed)
        assertEquals("Monday · Chest day", shown.first().title)
        assertEquals(template.first().exercises, shown.first().exercises)
        assertEquals(renamed, setSessionLabel(renamed, 0, "!!!"))
        assertNull(sanitizeSessionLabel("   "))
    }

    @Test
    fun storedLinesStayInsideTheCatalog() {
        assertNull(sanitizeExerciseLine("Made-up move — 3×10"))
        assertEquals("Bench press — 3×6–8", sanitizeExerciseLine("  bench press — 3×6–8 "))
        val junk = sanitizeCustomWorkouts(
            mapOf(
                "CUTTING:4" to List(7) { CustomDay("Push!", listOf("Nope — 1×1", "Lat pulldown — 3×10")) },
                "NOPE:2" to List(7) { CustomDay("", emptyList()) },
            ),
        )
        assertEquals(setOf("CUTTING:4"), junk.keys)
        assertEquals(listOf("Lat pulldown — 3×10"), junk.getValue("CUTTING:4").first().exercises)
        assertEquals("", junk.getValue("CUTTING:4").first().label)
    }

    @Test
    fun everyGeneratedExerciseCanBeSaved() {
        TrainingGoal.values().forEach { goal ->
            (MIN_TRAINING_DAYS..MAX_TRAINING_DAYS).forEach { days ->
                listOf(0, 1).forEach { block ->
                    buildWorkoutWeek(goal, days, block).filter { it.training }.flatMap { it.exercises }.forEach { line ->
                        assertNotNull(line, sanitizeExerciseLine(line))
                    }
                }
            }
        }
    }
}
