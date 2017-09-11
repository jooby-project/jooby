package kt

import org.jooby.Kooby
import org.jooby.run
import parser.Foo

class KtAppWithMain : Kooby({
    get {
        val foos = listOf<Foo>()
        foos
    }
})

fun main(args: Array<String>) {
    run(::KtAppWithMain, *args)
}
