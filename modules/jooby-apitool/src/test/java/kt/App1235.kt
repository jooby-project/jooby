package kt

import com.typesafe.config.Config
import org.jooby.*

fun main(args: Array<String>) {
  org.jooby.run(::App1235, *args)
}

class App1235 : Kooby({
  get("/qwe") { req ->
    require(Config::class.java)
  }
  get("/asd") { req ->
    require(Config::class.java)
  }
  get("/zxc") { req ->
    require(Config::class.java)
  }
  get("/rty") { req ->
    require(Config::class.java)
  }
})
