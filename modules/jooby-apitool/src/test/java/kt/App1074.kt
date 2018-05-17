package kt

import org.jooby.Kooby
import org.jooby.toOptional

class App1074 : Kooby({
    get {
        val page = param("page").toOptional(Integer::class) // uses org.jooby.toOptional
        val pageSize = param("page-size").toOptional(Integer::class.java)
        "optional"
    }
})