# rxjava

Make your web application reactive with <a href="https://github.com/ReactiveX/RxJava">RxJava</a>.

RxJava is a Java VM implementation of <a href="http://reactivex.io">Reactive Extensions</a>: a library for composing asynchronous and event-based programs by using observable sequences.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rxjava</artifactId>
 <version>1.0.2</version>
</dependency>
```

## exports

* map route operator: [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) that converts ```Observable``` (and family) into [Deferred API](/apidocs/org/jooby/Deferred.html).
* manage the lifecycle of ```Schedulers``` and make sure they go down on application shutdown time.

## usage

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx());

  get("/", () -> Observable.from("reactive programming in jooby!"));

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

Translation is done via [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) route operator. If you are a <a href="https://github.com/ReactiveX/RxJava">RxJava</a> programmer then you don't need to worry for learning a new API and semantic. The [Rx.rx()](/apidocs/org/jooby/rx/Rx.html#rx--) route operator deal and take cares of the [Deferred API](/apidocs/org/jooby/Deferred.html).

## rx mapper

Advanced observable configuration is allowed via adapter function:

```java
...
import org.jooby.rx.Rx;
...
{
  use(new Rx()
      .withObservable(observable -> observable.observeOn(Schedulers.io())));

  get("/observable", req -> Observable...);

}
```

The observable adapter function allow you to customize observables from routes.

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
