# kotlin

We do provide a very tiny module with some special functions that makes a {{jooby}} application more Kotlin idiomatic.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-lang-kotlin</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```kotlin

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

## idioms

### request access

Access to {{request}} is available via **request callback**:

```kotlin
run(*args) {
  get("/:name") {req ->
    val name = req.param("name").value
    "Hi $name!"
  }
}
```

The **request** idiom gives you implicit access to the {{request}} object, previous example can be written as:

```kotlin
run(*args) {
  get("/:name") {
    val name = param("name").value
    "Hi $name!"
  }
}
```


### route group

This idiom allow to group one or more routes under a common `path`:

```kotlin
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

We do provide `Kotlin class references` where a `Java class reference` is required.

Example 1: Register a `MVC routes`

```kotlin
run(*args) {
  use(Pets::class)
}
```

Example 2: Get an application service:

```kotlin
run(*args) {

  get("/query") {
    val db = require(MyDatabase::class)
    db.list()
  }

}
```

## examples

### JSON API

Next example uses the [jackson module](/doc/jackson) to parse and render `JSON`:

```kotlin

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

```kotlin

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

We do provide a starter [Kotlin](https://kotlinlang.org) project ready to use. Just go to [Github](https://github.com/jooby-project/lang-kotlin) and fork it.

That's all folks!!
