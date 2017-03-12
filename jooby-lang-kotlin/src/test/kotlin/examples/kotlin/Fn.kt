package examples.kotlin

import org.jooby.*
import com.typesafe.config.Config

data class User(val name: String, val age: Int)

fun main(args: Array<String>) {
  run(*args) {

    get {
      val name = param("name").value
      "Hi $name!!"
    }

    with {
      get("/with") { "With" }
    }.name("w")

    get("/user") {
      User("Pedro", 42)
    }

    route("/api/pets") {
      get { ->
        path()
      }

      get("/a") { ->
        path()
      }

      get("/b") { req, rsp ->
        rsp.send(req.path())
      }

      get("/c") { req, rsp, chain ->
        rsp.send(req.path())
        chain.next(req, rsp)
      }
    }.name("pets")
  }
}
