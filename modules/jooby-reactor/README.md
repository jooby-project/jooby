[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-reactor/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-reactor/1.6.3)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-reactor.svg)](https://javadoc.io/doc/org.jooby/jooby-reactor/1.6.3)
[![jooby-reactor website](https://img.shields.io/badge/jooby-reactor-brightgreen.svg)](http://jooby.org/doc/reactor)
# reactor

<a href="http://projectreactor.io">Reactor</a> is a second-generation Reactive library for building non-blocking applications on the JVM based on the <a href="http://www.reactive-streams.org">Reactive Streams Specification</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-reactor</artifactId>
 <version>1.6.3</version>
</dependency>
```

## exports

* map route operator that converts ```Flux``` and ```Mono``` into [Deferred API](/apidocs/org/jooby/Deferred.html).

## usage

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor());

  get("/", () -> Flux.just("reactive programming in jooby!"));
}
```

## how it works?

Previous example is translated to:

```java
{
  use(new Reactor());

  get("/", req -> {
   return new Deferred(deferred -> {
     Flux.just("reactive programming in jooby!")
       .consume(deferred::resolve, deferred::reject);
   });
  });

}
```

Translation is done with the [Reactor.reactor()](/apidocs/org/jooby/reactor/Reactor.html#reactor--) route operator. If you are a <a href="http://projectreactor.io">reactor</a> programmer then you don't need to worry for learning a new API and semantic. The [Reactor.reactor()](/apidocs/org/jooby/reactor/Reactor.html#reactor--) route operator deal and take cares of the [Deferred API](/apidocs/org/jooby/Deferred.html).


## reactor mapper

Advanced flux/mono configuration is allowed via function adapters:

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor()
    .withFlux(flux -> flux.pusblishOn(Computations.concurrent())
    .withMono(mono -> mono.pusblishOn(Computations.concurrent()));

  get("/flux", () -> Flux...);

  get("/mono", () -> Mono...);

}
```

Here every Flux/Mono from a route handler will publish on the ```concurrent``` scheduler.
