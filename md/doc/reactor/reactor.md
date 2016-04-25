# reactor

<a href="http://projectreactor.io">Reactor</a> is a second-generation Reactive library for building non-blocking applications on the JVM based on the <a href="http://www.reactive-streams.org">Reactive Streams Specification</a>

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-reactor</artifactId>
 <version>{{version}}</version>
</dependency>
```

## exports

* map route operator that converts ```Flux``` and ```Mono``` into [Deferred API]({{defdocs}}/Deferred.html).
* set a default server thread pool with the number of available processors. 

## usage

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor());

  get("/", req -> Flux.just("reactive programming in jooby!"))
     .map(Reactor.reactor());
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

Translation is done with the [Reactor.reactor()]({{defdocs}}/reactor/Reactor.html#reactor--) route operator. If you are a <a href="http://projectreactor.io">reactor</a> programmer then you don't need to worry for learning a new API and semantic. The [Reactor.reactor()]({{defdocs}}/reactor/Reactor.html#reactor--) route operator deal and take cares of the [Deferred API]({{defdocs}}/Deferred.html).

## reactor()

We just learn that we are not force to learn a new API, just write <a href="http://projectreactor.io">reactor</a> code. That's cool!

But.. what if you have 10 routes? 50 routes?

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor());

  get("/1", req -> Observable...)
     .map(Reactor.reactor());
  
  get("/2", req -> Observable...)
     .map(Reactor.reactor());
  ....

  get("/N", req -> Observable...)
     .map(Reactor.reactor());
}
```

This is better than written N routes using the [Deferred API]({{defdocs}}/Deferred.html) route by route... but still there is one more option to help you (and your fingers) to right less code:

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor());

  with(() -> {
    get("/1", req -> Observable...);
    get("/2", req -> Observable...);
    ....
    get("/N", req -> Observable...);
  }).map(Reactor.reactor());

}
```

**Beautiful, hugh?**

The [with]({{defdocs}}/Routes.html#with-java.lang.Runnable-) operator let you group any number of routes and apply common attributes and/or operator to all them!!!

## reactor()+scheduler

You can provide a ```Scheduler``` to the [Reactor.reactor(Supplier)]({{defdocs}}/reactor/Reactor.html#reactor--) operator:

```java
...
import org.jooby.reactor.Reactor;
...
{
  use(new Reactor());

  with(() -> {

    get("/1", req -> Observable...);
    get("/2", req -> Observable...);
    ....
    get("/N", req -> Observable...);
  }).map(Reactor.reactor(Computations::concurrent));

}
```

All the routes here will ```Flux#subscribeOn(Scheduler)``` subscribe-on the provided ```Scheduler```.
