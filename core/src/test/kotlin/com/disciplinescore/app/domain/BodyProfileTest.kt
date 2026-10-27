package com.disciplinescore.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyProfileTest {
    @Test
    fun metricHeightConvertsToFeetAndInches() {
        assertEquals(FeetInches(5, 10), cmToFeetInches(178f))
        assertEquals(177.8f, feetInchesToCm(5, 10), 0.05f)
    }

    @Test
    fun bmiUsesHeightAndWeight() {
        val reading = bmi(178f, 72f)!!

        assertEquals(22.7, reading.value, 0.05)
        assertEquals("Healthy range", reading.band)
    }

    @Test
    fun valuesOutsideTheRangeAreLeftOut() {
        assertNull(bmi(10f, 72f))
        assertNull(bmi(178f, 5f))
        assertNull(bodySummary(null, null, MeasureSystem.METRIC))
    }

    @Test
    fun ageCountsFullYearsOnly() {
        val today = LocalDate.of(2026, 10, 20)
        assertEquals(30, ageYears(LocalDate.of(1996, 10, 20), today))
        assertEquals(29, ageYears(LocalDate.of(1996, 10, 21), today))
    }

    @Test
    fun summaryFollowsTheChosenUnits() {
        assertEquals("178 cm · 72 kg · BMI 22.7", bodySummary(178f, 72f, MeasureSystem.METRIC))
        assertEquals("5 ft 10 in · 158.7 lb · BMI 22.7", bodySummary(178f, 72f, MeasureSystem.IMPERIAL))
    }
}
