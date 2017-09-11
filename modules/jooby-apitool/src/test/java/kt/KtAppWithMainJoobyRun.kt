package kt

import org.jooby.Jooby
import org.jooby.Kooby
import parser.Foo

class KtAppWithMainJoobyRun : Kooby({
    get {
        val foos = listOf<Foo>()
        foos
    }
})

fun main(args: Array<String>) {
    Jooby.run(::KtAppWithMain, args)
}
