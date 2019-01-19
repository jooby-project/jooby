package kt

import org.jooby.*

data class ResultData(
    val text: String,
    val number: Int
)

class App1261 : Kooby({
  get("/") {req, rsp ->
    val response = ResultData("Test", 123)
    rsp.send(response)
  }
})
