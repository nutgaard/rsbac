package no.utgdev.rsbac.example;

public class TestService {
    public String shouldNotBeAccessable(String value) {
        return "No nonono. Should not get " + value;
    }
}
