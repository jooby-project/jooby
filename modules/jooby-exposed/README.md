[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-exposed/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-exposed/1.6.0)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-exposed.svg)](https://javadoc.io/doc/org.jooby/jooby-exposed/1.6.0)
[![jooby-exposed website](https://img.shields.io/badge/jooby-exposed-brightgreen.svg)](http://jooby.org/doc/exposed)
# exposed
 
 <a href="https://github.com/JetBrains/Exposed">Exposed</a> is a prototype for a lightweight SQL library written over JDBC driver for Kotlin language
 
> NOTE: This module depends on [jdbc](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) module.

## exports

* Database object 

## usage

```java
{ 
   use(Jdbc())
   use(Exposed())

   get("/db") {
     val db = require(Database::class)
     transaction (db) {
       // Work with db...
     }
   }
 }
```

## multiple databases

```java
{
  use(Jdbc("db1"))

  use(Jdbc("db2"))
 
  use(Exposed("db1"))
 
  use(Exposed("db2"))
 
  get("/db") {
    val db1 = require("db1", Database::class)
    // Work with db1...
    
    val db2 = require("db2", Database::class)
    // Work with db2...
  }
}
```

That's all! Happy coding!!!
