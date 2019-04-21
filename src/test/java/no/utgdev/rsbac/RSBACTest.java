package no.utgdev.rsbac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;


class RSBACTest {

    @Test
    void should_deny_if_denyCondition_is_true() {
        RSBAC<Void> rsbac = new RSBACImpl<>(null);

        assertThrows(RSBACException.class, () -> rsbac
                .deny("", (aVoid) -> true)
                .get(() -> "OK"));
    }

    @Test
    void should_deny_if_permitCondition_is_false() {
        RSBAC<Void> rsbac = new RSBACImpl<>(null);

        assertThrows(RSBACException.class, () -> rsbac
                .permit("", (aVoid) -> false)
                .get(() -> "OK"));
    }

    @Test
    void should_return_message_from_failed_rule() {
        RSBAC<Void> rsbac = new RSBACImpl<>(null);

        Executable failOnFirst = () -> rsbac.permit("Error 1", (aVoid) -> false).get(() -> "OK");
        assertThrows(RSBACException.class, failOnFirst);
        assertThrowsHasMessage("Error 1", failOnFirst);

        Executable failOnLast = () -> rsbac
                .permit("Error 1", (aVoid) -> true)
                .permit("Error 2", (aVoid) -> false)
                .get(() -> "OK");
        assertThrows(RSBACException.class, failOnLast);
        assertThrowsHasMessage("Error 2", failOnLast);
    }

    @Test
    void should_return_result_if_every_test_pass() {
        RSBAC<Void> rsbac = new RSBACImpl<>(null);

        String result = rsbac
                .permit("Error 1", (aVoid) -> true)
                .deny("Error 2", (aVoid) -> false)
                .permit("Error 3", (aVoid) -> true)
                .deny("Error 4", (aVoid) -> false)
                .get(() -> "OK");

        assertEquals("OK", result);
    }

    @Test
    void should_expose_context_to_all_rules() {
        RSBAC<String> rsbac = new RSBACImpl<>("Value");

        String result = rsbac
                .permit("Error 1", (String context) -> context.equals("Value"))
                .permit("Error 2", (String context) -> context.equals("Value"))
                .deny("Error 3", (String context) -> !context.equals("Value"))
                .get(() -> "OK");

        assertEquals("OK", result);
    }

    @Test
    void should_have_default_deny_bias() {
        RSBAC<String> rsbac = new RSBACImpl<>("value");

        Executable biased = () -> rsbac
                .check(new Policy<>("I have no Idea", (aVoid) -> DecisionEnums.NOT_APPLICABLE))
                .get(() -> "OK");

        assertThrows(RSBACException.class, biased);
        assertThrowsHasMessage("I have no Idea", biased);
    }

    @Test
    void should_respect_bias() {
        RSBAC<String> rsbac = new RSBACImpl<>("value");

        String biased = rsbac
                .bias(DecisionEnums.PERMIT)
                .check(new Policy<>("I have no Idea", (aVoid) -> DecisionEnums.NOT_APPLICABLE))
                .get(() -> "OK");

        assertEquals("OK", biased);
    }


    public void assertThrowsHasMessage(String expected, Executable executable) {
        boolean caught = false;
        try {
            executable.execute();
        } catch (Throwable throwable) {
            caught = true;
            String message = throwable.getMessage();
            assertEquals(expected, message);
        } finally {
            assertTrue(caught, "Did not catch any exception");
        }
    }
}
