package i2598

import io.jooby.Kooby

class App2598 : Kooby({
  get("/2598") {
    val sign = mutableListOf<Int>()
    ctx.send("{\"success\":\"true\"}")
    //some imaginary long running operation here
    sign.removeIf { it == 1 }
    return@get ctx
  }
})

