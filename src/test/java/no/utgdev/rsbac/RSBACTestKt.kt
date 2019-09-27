package no.utgdev.rsbac

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RSBACTestKt {
    @Test
    fun `should deny if denyCondition is true`() {
        val rsbac = RSBACImpl(null)

        assertThrows<RSBACException> {
            rsbac
                    .deny("") { true }
                    .get { "OK" }
        }

        assertThrows<RSBACException> {
            rsbac
                    .deny("") { true }
                    .get { "OK" }
        }
    }

    @Test
    fun `should deny if permitCondition is false`() {
        val rsbac = RSBACImpl(null)

        assertThrows<RSBACException> {
            rsbac
                    .permit("") { false }
                    .get { "OK" }
        }

        assertThrows<RSBACException> {
            rsbac
                    .permit("") { false }
                    .get { "OK" }
        }
    }

    @Test
    fun `should return message from failed rule`() {
        val rsbac = RSBACImpl(null)

        val failOnFirst: () -> Unit = {
            rsbac
                    .permit("Error 1") { false }
                    .permit("Error 2") { false }
                    .get { "OK" }
        }
        assertThrows<RSBACException>(failOnFirst)
        assertThrowsHasMessage("Error 1", failOnFirst)

        val failOnLast: () -> Unit = {
            rsbac
                    .permit("Error 1") { true }
                    .permit("Error 2") { false }
                    .get { "OK" }
        }

        assertThrows<RSBACException>(failOnLast)
        assertThrowsHasMessage("Error 2", failOnLast)
    }

    @Test
    fun `should return result if every test pass`() {
        val rsbac = RSBACImpl(null)

        val result = rsbac
                .permit("Error 1") { true }
                .deny("Error 2") { false }
                .permit("Error 3") { true }
                .deny("Error 4") { false }
                .get { "OK" }

        assertEquals("OK", result)
    }

    @Test
    fun `should expose context to all rules`() {
        val rsbac = RSBACImpl("Value")

        val result = rsbac
                .permit("Error 1") { context: String -> context == "Value" }
                .permit("Error 2") { context: String -> context == "Value" }
                .deny("Error 3") { context: String -> context != "Value" }
                .get { "OK" }

        assertEquals("OK", result)
    }

    @Test
    fun `should have default deny bias`() {
        val rsbac = RSBACImpl("value")

        val biased: () -> Unit = {
            rsbac
                    .check(Policy("I have no Idea") { DecisionEnums.NOT_APPLICABLE })
                    .get { "OK" }
        }

        assertThrows<RSBACException>(biased)
        assertThrowsHasMessage("No matching rule found", biased)
    }

    @Test
    fun `should respect bias`() {
        val rsbac = RSBACImpl("value")

        val biased = rsbac
                .bias(DecisionEnums.PERMIT)
                .check(Policy("I have no Idea") { DecisionEnums.NOT_APPLICABLE })
                .get { "OK" }

        assertEquals("OK", biased)
    }

    private fun assertThrowsHasMessage(expected: String, executable: () -> kotlin.Unit) {
        var caught = false
        try {
            executable.invoke()
        } catch (throwable: Throwable) {
            caught = true
            val message = throwable.message
            assertEquals(expected, message)
        } finally {
            assertTrue(caught, "Did not catch any exception")
        }
    }
}
