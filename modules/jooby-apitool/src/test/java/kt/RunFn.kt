package kt

import org.jooby.*
import org.jooby.apitool.ApiTool

fun main(args: Array<String>) {
    run(*args) {
        use(KResource::class)

        use(ApiTool()
                .swagger())
    }
}
