import io.jooby.ServerOptions
import io.jooby.runApp

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  val options = ServerOptions().apply {
    port = 8080
  }

  runApp(args) {
    val p = getBasePackage()
    println(p)
    get {
      ":+1"
    }
  }
}
