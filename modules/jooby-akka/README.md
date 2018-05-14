[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-akka/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-akka)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-akka.svg)](https://javadoc.io/doc/org.jooby/jooby-akka/1.3.0)
[![jooby-akka website](https://img.shields.io/badge/jooby-akka-brightgreen.svg)](http://jooby.org/doc/akka)
# akka

Small module to build concurrent & distributed applications via [Akka](http://akka.io).

## exports

* An ```ActorSystem```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-akka</artifactId>
  <version>1.3.0</version>
</dependency>
```

## usage

```java
{
  use(new Akka());

  get("/akka", promise((req, deferred) -> {
    ActorSystem sys = require(ActorSystem.class);
    ActorRef actor = sys.actorOf(...);
    // send the deferred to the actor
    actor.tell(deferred, actor);
  });
}
```

That's all folks!

Happy coding!!!

## akka.conf
These are the default properties for akka:

```properties
akka {

  loggers = ["akka.event.slf4j.Slf4jLogger"]

  # Options: ERROR, WARNING, INFO, DEBUG

  loglevel = "INFO"

}
```
