package io.jooby

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(args) {
    get("/") {
      val q: List<String> by ctx.query
      q
    }
  }
}
