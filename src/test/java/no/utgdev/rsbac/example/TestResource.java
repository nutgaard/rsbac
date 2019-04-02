package no.utgdev.rsbac.example;

import no.utgdev.rsbac.RSBAC;

public class TestResource {
    private TestService service;
    private RSBAC<UserService> rsbac;

    public TestResource(TestService service, RSBAC<UserService> rsbac) {
        this.service = service;
        this.rsbac = rsbac;
    }

    public String shouldNotBeAccessable(String value) {
        return rsbac
                .permit("Must have length", () -> value.length() > 0)
                .permit("Did not meet \"OK\"-value", (UserService userService) -> "OK".equals(value))
                .deny("Matched illegal value", () -> "NOT OK".equals(value))
                .get(() -> service.shouldNotBeAccessable(value));
    }
}
