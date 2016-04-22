# rxjava

Make your web application reactive with <a href="https://github.com/ReactiveX/RxJava">RxJava</a>.

RxJava is a Java VM implementation of <a href="http://reactivex.io">Reactive Extensions</a>: a library for composing asynchronous and event-based programs by using observable sequences.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rxjava</artifactId>
 <version>1.0.0.CR2</version>
</dependency>
```

## exports

* map route operator: [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) that converts ```Observable``` (and family) into [Deferred API](/apidocs/org/jooby/Deferred.html).
* manage the lifecycle of ```Schedulers``` and make sure they go down on application shutdown time.
* set a default server thread pool with the number of available processors.

## usage

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx());

  get("/", req -> Observable.from("reactive programming in jooby!"))
     .map(Rx.rx());
}
```

## how it works?

Previous example is translated to:

```java
{
  use(new Rx());

  get("/", req -> {
   return new Deferred(deferred -> {
     Observable.from("reactive programming in jooby!")
       .subscribe(deferred::resolve, deferred::reject);
   });
  });

}
```

Translation is done with the [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) route operator. If you are a <a href="https://github.com/ReactiveX/RxJava">RxJava</a> programmer then you don't need to worry for learning a new API and semantic. The [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) route operator deal and take cares of the [Deferred API](/apidocs/org/jooby/Deferred.html) API.

## rx()

We just learn that we are not force to learn a new API, just write <a href="https://github.com/ReactiveX/RxJava">RxJava</a> code. That's cool!

But.. what if you have 10 routes? 50 routes?

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx());

  get("/1", req -> Observable...)
     .map(Rx.rx());

  get("/2", req -> Observable...)
     .map(Rx.rx());
  ....

  get("/N", req -> Observable...)
     .map(Rx.rx());
}
```

This is better than written N routes using the [Deferred API](/apidocs/org/jooby/Deferred.html) route by route... but still there is one more option to help you (and your fingers) to right less code:

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx());

  with(() -> {

    get("/1", req -> Observable...);
    get("/2", req -> Observable...);
    ....
    get("/N", req -> Observable...);

  }).map(Rx.rx());

}
```

**Beautiful, hugh?**

The [with](/apidocs/org/jooby/Routes.html#with-java.lang.Runnable-) operator let you group any number of routes and apply common attributes and/or operator to all them!!!

## rx()+scheduler

You can provide a ```Scheduler``` to the [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) operator:

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx());

  with(() -> {

    get("/1", req -> Observable...);
    get("/2", req -> Observable...);
    ....
    get("/N", req -> Observable...);

  }).map(Rx.rx(Schedulers::io));

}
```

All the routes here will ```Observable#subscribeOn(Scheduler)``` the provided ```Scheduler```.

## schedulers

This module provides the default ```Scheduler``` from <a href="https://github.com/ReactiveX/RxJava">RxJava</a>. But also let you define your own ```Scheduler``` using the [executor module](/doc/executor).

```
rx.schedulers.io = forkjoin
rx.schedulers.computation = fixed
rx.schedulers.newThread = "fixed = 10"
```

The previous example defines a:

* forkjoin pool for ```Schedulers#io()``` 
* fixed thread pool equals to the number of available processors for ```Schedulers#computation()```
* fixed thread pool with a max of 10 for ```Schedulers#newThread()```

Of course, you can define/override all, some or none of them. In any case the ```Scheduler``` will be shutdown at application shutdown time.
