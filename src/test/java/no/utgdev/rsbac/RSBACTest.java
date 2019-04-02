package no.utgdev.rsbac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;


class RSBACTest {

    @Test
    void should_deny_if_denyCondition_is_true() {
        RSBAC<Void> rsbac = new RSBAC<>(null);

        assertThrows(RSBACException.class, () -> rsbac
                .deny("", () -> true)
                .get(() -> "OK"));

        assertThrows(RSBACException.class, () -> rsbac
                .deny("", (aVoid) -> true)
                .get(() -> "OK"));
    }

    @Test
    void should_deny_if_permitCondition_is_false() {
        RSBAC<Void> rsbac = new RSBAC<>(null);

        assertThrows(RSBACException.class, () -> rsbac
                .permit("", () -> false)
                .get(() -> "OK"));

        assertThrows(RSBACException.class, () -> rsbac
                .permit("", (aVoid) -> false)
                .get(() -> "OK"));
    }

    @Test
    void should_return_message_from_failed_rule() {
        RSBAC<Void> rsbac = new RSBAC<>(null);

        Executable failOnFirst = () -> rsbac.permit("Error 1", () -> false).get(() -> "OK");
        assertThrows(RSBACException.class, failOnFirst);
        assertThrowsHasMessage("Error 1", failOnFirst);

        Executable failOnLast = () -> rsbac
                .permit("Error 1", () -> true)
                .permit("Error 2", () -> false)
                .get(() -> "OK");
        assertThrows(RSBACException.class, failOnLast);
        assertThrowsHasMessage("Error 2", failOnLast);
    }

    @Test
    void should_return_result_if_every_test_pass() {
        RSBAC<Void> rsbac = new RSBAC<>(null);

        String result = rsbac
                .permit("Error 1", () -> true)
                .deny("Error 2", () -> false)
                .permit("Error 3", () -> true)
                .deny("Error 4", () -> false)
                .get(() -> "OK");

        assertEquals("OK", result);
    }

    @Test
    void should_expose_context_to_all_rules() {
        RSBAC<String> rsbac = new RSBAC<>("Value");

        String result = rsbac
                .permit("Error 1", (String context) -> context.equals("Value"))
                .permit("Error 2", (String context) -> context.equals("Value"))
                .deny("Error 3", (String context) -> !context.equals("Value"))
                .get(() -> "OK");

        assertEquals("OK", result);
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
