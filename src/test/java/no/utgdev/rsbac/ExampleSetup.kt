package no.utgdev.rsbac

class UserService {
    fun getUsers() = listOf("User 1", "User 2")
}

class TestService {
    fun shouldNotBeAccessable(value: String) = "Nonono. Should never be called, but called with $value"
}

class TestResource(val service: TestService, val rsbac: RSBAC<UserService>) {
    fun shouldNotBeAccessable(value: String) =
            rsbac
                    .permit("Must have length") { value.isNotEmpty() }
                    .permit("Did not meed \"OK\"-value") { "OK" == value }
                    .deny("Matched illegal value") { "NOT OK" == value }
                    .get { service.shouldNotBeAccessable(value) }
}
