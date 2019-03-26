import io.jooby.runApp

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(args) {
    val p = getBasePackage()
    println(p)
    get {
      ":+1"
    }
  }
}
