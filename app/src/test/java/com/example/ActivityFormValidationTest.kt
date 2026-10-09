package com.example

import com.example.ui.viewmodel.validateActivityForm
import org.junit.Assert.*
import org.junit.Test

class ActivityFormValidationTest {
    private fun validate(title: String = "Walk", location: String = "Thamel", date: String = "Tomorrow",
                         time: String = "8 AM", price: String = "Free", people: String = "4",
                         description: String = "Meet at the gate") =
        validateActivityForm(title, location, date, time, price, people, description)

    @Test fun acceptsValidActivityAndOptionalTimeAndPrice() {
        assertNull(validate())
        assertNull(validate(time = "", price = ""))
    }
    @Test fun rejectsMissingRequiredFields() {
        assertNotNull(validate(title = " "))
        assertNotNull(validate(location = ""))
        assertNotNull(validate(date = ""))
        assertNotNull(validate(description = ""))
    }
    @Test fun rejectsInvalidGroupSizeInsteadOfSilentlyDefaulting() {
        listOf("", "0", "-1", "10001", "abc", "99999999999").forEach { assertNotNull(validate(people = it)) }
        assertNull(validate(people = "1"))
        assertNull(validate(people = "10000"))
    }
    @Test fun matchesBackendTextLimits() {
        assertNull(validate(title = "x".repeat(200), description = "x".repeat(5000)))
        assertNotNull(validate(title = "x".repeat(201)))
        assertNotNull(validate(location = "x".repeat(201)))
        assertNotNull(validate(date = "x".repeat(81)))
        assertNotNull(validate(time = "x".repeat(81)))
        assertNotNull(validate(price = "x".repeat(81)))
        assertNotNull(validate(description = "x".repeat(5001)))
    }
}
