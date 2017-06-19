# kotlin

A tiny module that makes a Jooby application more Kotlin idiomatic.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-lang-kotlin</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

via run function:

```java

import org.jooby.*

fun main(args: Array<String>) {
  run(*args) {
    get {
      "Hello Kotlin"
    }
  }
}

```

The `run` function is a [type-safe builder](http://kotlinlang.org/docs/reference/type-safe-builders.html) that initializes, configures and executes a {{jooby}} application.

via Kooby class:

```java

import org.jooby.*

Class App: Kooby({
  get {
    "Hello Kotlin"
  }
})

fun main(args: Array<String>) {
  run(::App, *args)
}
```

## idioms


### request access

Access to the {{request}} is available via a **request callback**:

```java
run(*args) {
  get("/:name") {req ->
    val name = req.param("name").value
    "Hi $name!"
  }
}
```

The **request** idiom gives you implicit access to the {{request}} object. The previous example can be written as:

```java
run(*args) {
  get("/:name") {
    val name = param("name").value
    "Hi $name!"
  }
}
```

Reified `param`, `header`, `body` calls:

```java
run(*args) {
  get("/:name") {
    val count = param<Int>("count")
    count
  }

  post("/") {
    val myobj = body<MyObject>()
    myobj
  }
}
```

### route group

This idiom allows grouping one or more routes under a common `path`:

```java
run(*args) {

  route("/api/pets") {

    get {-> 
      // List all pets
    }

    get("/:id") {-> 
      // Get a Pet by ID
    }

    post {-> 
      // Create a new Pet
    }
  }
}
```

### class reference

{{jooby}} provides a `Kotlin class references` where a `Java class reference` is required.

Example 1: Register a `MVC routes`

```java
run(*args) {
  use(Pets::class)
}
```

Example 2: Get an application service:

```java
run(*args) {

  get("/query") {
    val db = require(MyDatabase::class)
    db.list()
  }

}
```

## examples

### JSON API

The next example uses the [jackson module](/doc/jackson) to parse and render `JSON`:

```java

import org.jooby.*
import org.jooby.json.*

data class User(val name: String, val age: Int)

fun main(args: Array<String>) {
  run(*args) {

    use(Jackson())

    get("/user") {
      User("Pedro", 42)
    }
  }
}

```

> NOTE: You need the [jackson-module-kotlin](https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin) for [Kotlin](http://kotlinlang.org/) data classes.

### mvc example

```java

import org.jooby.*
import org.jooby.mvc.*
import javax.inject.Inject

@Path("/api/pets")
class Pets @Inject constructor(val db: MyDatabase) {

  @GET
  fun list(): List<Pet> {
    return db.queryPets()
  }
}

fun main(args: Array<String>) {
  run(*args) {
    use(Pets::class)
  }
}
```

## starter project

{{jooby}} provides a [kotlin-starter](https://github.com/jooby-project/kotlin-starter) project. Go and [fork it](https://github.com/jooby-project/kotlin-starter).

That's all folks!
