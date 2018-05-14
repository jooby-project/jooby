package kt

import org.jooby.Kooby
import org.jooby.run
import org.jooby.value

class JavaApp : Kooby({
    /**
     * Java API.
     */
    put("/java") { req ->
        req.param("p1").value
    }
})

fun main(args: Array<String>) {
    run(::HelloWorld, *args)
}

