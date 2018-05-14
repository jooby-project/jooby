# akka

Small module to build concurrent & distributed applications via [Akka](http://akka.io).

## exports

* An ```ActorSystem```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-akka</artifactId>
  <version>{{version}}</version>
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

{{appendix}}
