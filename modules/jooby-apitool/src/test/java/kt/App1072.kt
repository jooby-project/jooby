package kt

import org.jooby.Kooby

data class Person(
        val name: String,
        val firstname: String?)

class App1072 : Kooby({
    get {
        Person("John Doe", null)
    }
})