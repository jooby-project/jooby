package kt

import org.jooby.*

class App1079 : Kooby({
    get {
        val page = param("p1").toOptional(Int::class) // uses org.jooby.toOptional
        val pageSize = param("p2").toOptional(Int::class.java)
        val p3 = param("p3").to(Int::class)
        "optional"
    }
})