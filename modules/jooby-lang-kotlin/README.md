[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-lang-kotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-lang-kotlin)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-lang-kotlin.svg)](https://javadoc.io/doc/org.jooby/jooby-lang-kotlin/1.4.1)
[![jooby-lang-kotlin website](https://img.shields.io/badge/jooby-lang-kotlin-brightgreen.svg)](http://jooby.org/doc/lang-kotlin)
# kotlin

A tiny module that makes a Jooby application more Kotlin idiomatic.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-lang-kotlin</artifactId>
  <version>1.4.1</version>
</dependency>
```

## usage

via Kooby class (preferred):

```kt

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

via run function:

```kt

import org.jooby.*

fun main(args: Array<String>) {
  run(*args) {
    get {
      "Hello Kotlin"
    }
  }
}

```

The `run` function is a [type-safe builder](http://kotlinlang.org/docs/reference/type-safe-builders.html) that initializes, configures and executes a [Jooby](http://jooby.org) application.

## idioms


### request access

Access to the [request](/apidocs/org/jooby/Request.html) is available via a **request callback**:

```java
{
  get("/:name") {req ->
    val name = req.param("name").value
    "Hi $name!"
  }
}
```

The **request** idiom gives you implicit access to the [request](/apidocs/org/jooby/Request.html) object. The previous example can be written as:

```java
{
  get("/:name") {
    val name = param("name").value
    "Hi $name!"
  }
}
```

Reified `param`, `header`, `body` calls:

```java
{
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

### path group

This idiom allows grouping one or more routes under a common `path`:

```java
{

  path("/api/pets") {

    get { 
      // List all pets
    }

    get("/:id") { 
      // Get a Pet by ID
    }

    post {
      // Create a new Pet
    }
  }
}
```

### class reference

[Jooby](http://jooby.org) provides a `Kotlin class references` where a `Java class reference` is required.

Example 1: Register a `MVC routes`

```java
{
  use(Pets::class)
}
```

Example 2: Get an application service:

```java
{

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

class App: Kooby({
  use(Jackson())

  get("/user") {
    User("Pedro", 42)
  }

})

fun main(args: Array<String>) {
  run(::App, *args)
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

We do provide a [kotlin-starter](https://github.com/jooby-project/kotlin-starter) demo project.

That's all folks!
