package kt

import io.jooby.Context
import io.jooby.runApp
import io.swagger.v3.oas.annotations.Operation

@Operation(summary = "function reference")
fun fnRef(ctx: Context): Int {
  return 0
}

fun main(args: Array<String>) {
  runApp(args) {
    get("/path") {
      "Foo"
    }

    get("/fn", ::fnRef)
  }
}
