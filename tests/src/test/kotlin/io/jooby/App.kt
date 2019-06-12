package io.jooby

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(args) {
    get("/") {ctx ->
      val q = ctx.query<SearchQuery>()
      q.q
    }
  }
}
