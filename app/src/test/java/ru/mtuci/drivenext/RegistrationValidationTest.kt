package ru.mtuci.drivenext

import org.junit.Assert.*
import org.junit.Test
import ru.mtuci.drivenext.domain.RegistrationValidation

class RegistrationValidationTest {
    @Test
    fun acceptsRequiredEmailDomains() {
        assertTrue(RegistrationValidation.isEmailValid("student@example.ru"))
        assertTrue(RegistrationValidation.isEmailValid("name+test@mail.example.com"))
        assertTrue(RegistrationValidation.isEmailValid("STUDENT@EXAMPLE.RU"))
    }

    @Test
    fun rejectsMalformedOrUnsupportedEmails() {
        for (email in listOf("", "student", "@example.ru", "student@.ru", "student@example.org")) {
            assertFalse(email, RegistrationValidation.isEmailValid(email))
        }
    }

    @Test
    fun requiresNonEmptyMatchingPasswords() {
        assertTrue(RegistrationValidation.passwordsMatch("password", "password"))
        assertFalse(RegistrationValidation.passwordsMatch("password", "different"))
        assertFalse(RegistrationValidation.passwordsMatch("", ""))
    }

    @Test
    fun convertsBothRequiredDateFormats() {
        assertEquals("2000-12-31", RegistrationValidation.dateForStorage("12/31/2000", "MM/dd/uuuu"))
        assertEquals("2020-01-15", RegistrationValidation.dateForStorage("15/01/2020", "dd/MM/uuuu"))
    }

    @Test(expected = java.time.format.DateTimeParseException::class)
    fun rejectsImpossibleDates() {
        RegistrationValidation.dateForStorage("02/30/2000", "MM/dd/uuuu")
    }
}
