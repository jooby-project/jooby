import io.jooby.ExecutionMode
import io.jooby.runApp

data class SearchQuery(val q: String)

fun main(args: Array<String>) {
  runApp(ExecutionMode.EVENT_LOOP, args) {
    serverOptions {
      singleLoop = true
      ioThreads = 5
    }
    get {
      ":+1"
    }
  }
}
