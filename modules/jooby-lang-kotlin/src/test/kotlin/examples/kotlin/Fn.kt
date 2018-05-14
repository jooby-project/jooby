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

    // reified version
    post {
      val h = header<Int>("id")
      val name = param<String>("name")
      val user = body<User>()
      name + user + h
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

    get("/params") {
      val name = param<String>("name")
      val age = param<Int>("age")
      "Hi $name, your username is now ${name + age}"
    }

    get("/headers") {
      val name = header<String>("name")
      val age = header<Int>("age")
      "Hi $name, your username is now ${name + age}"
    }

    get("/body") {
      val user = body<User>()
      user
    }

    onStart {
      println("Starting")
    }

    onStarted {
      println("Started")
    }

    onStop {
      println("Stopped")
    }
  }
}
