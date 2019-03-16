import io.jooby.run

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  run(args) {
    val p = basePackage()
    println(p)
    get {
      ":+1"
    }
  }
}
