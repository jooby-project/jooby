# thread model

You can see {{jooby}} as an `event loop server` thanks to the supported web servers: {{netty_server}}, {{jetty_server}} and {{undertow_server}}. The default web server is {{netty_server}}.

{{jooby}} isn't a traditional `thread server` where a HTTP request is bound to a thread.

In {{jooby}} all the HTTP IO operations are performed in async & non blocking fashion. HTTP IO operations run in an IO thread (a.k.a event loop) while the application logic (your code) **always run in a worker thread**.

## worker threads

The **worker thread** pool is provided by one of the supported web servers: {{netty_server}}, {{jetty_server}} or {{undertow_server}}. To simplify application programming you can **block a worker thread**, for example you can safely run a **jdbc** query in a **worker thread**:

```java
{
  get("/search-db", () -> {
    try(Connection db = require(Connection.class)) {
      try(Statement stt = db.createStatement()) {
        ...
      } 
    }
  });
}
```

The web server can accept as many connections it can (as its on non blocking) while the worker thread might block.

The default worker thread pool is `20/100`. The optimal size depends on the **business** and **work load** your application is suppose to handle. We suggest you to start with the default setup and see how it goes, later you can reduce or increase the thread pool.

In {{jooby}} we favor simplicity over complexity that is why your **code** can block, still there are more advanced options that allow you to build async and reactive applications.

## deferred

Async processing is achieved via: {{deferred}} result, with a {{deferred}} result an application can produces a result from a **thread of its choice**:

Script API:

```java
{
  get("/async", deferred(() -> {
    return ...;
  });
}
```

MVC API:

```java
  @GET
  @Path("/async")
  public Deferred async() {
    return Deferred.deferred(() -> {
      return ...;
    });
  }
```

The previous examples are just `syntactic sugar` for:

```
  return new Deferred(deferred -> {
     try {
       deferred.resolve(...);
     } catch (Throwable x) {
       deferred.reject(x);
     }
  });
```

You can get more `syntactic sugar` if you add the [AsyncMapper]({{defdocs}}/AsyncMapper.html) to your application:

Script API:

```java
{
  map(new AsyncMapper());
  
   get("/async", () -> {
     Callable<String> callable = () -> {
       return ...;
     }; 
    return callable;
  });
}
```

MVC API:

```java
@GET
  @Path("/async")
  public Callable<String> async() {
    return () -> {
      return ...;
    };
  }
```

The [AsyncMapper]({{defdocs}}/AsyncMapper.html) converts `java.util.concurrent.Callable` and `java.util.concurrent.CompletableFuture` objects to {{deferred}} objects.

Another important thing to notice is that the deferred run in the **caller thread** (i.e. worker thread), so by default there is **no context switch** involved while running a {{deferred}} result:

```java
{
  get("/async", () -> {
    String callerThread = Thread.current().getName();
    return Deferred.deferred(() -> {
      assertEquals(callerThread, Thread.current().getName());
      return ...;
    });
  });
}
```

You might see this as a bad thing at first, but it's actually a good decision, because:

* It is super easy to setup a default executor (we will see how soon)

* Provides better integration with async & reactive libraries. A `direct` executor avoid the need of switching to a new thread and then probably dispatch (again) to a different thread provided by a library.

## executor

As previously mentioned, the default executor runs in the caller thread (a.k.a direct executor). Let's see how to override the default executor:

```java
{
  executor(new ForkJoinPool());

  get("/async", deferred(() -> {
    return ...;
  });
}
```

Done! Now all our {{deferred}} results run in a `ForkJoinPool`. It's also possible to specify an alternative executor:

Script API:

```java
{
  executor(new ForkJoinPool());

  executor("jdbc", Executors.newFixedThreadPool(10));

  get("/", deferred(() -> {
    return ...;
  });

  get("/db", deferred("jdbc", () -> {
    return ...;
  });
}
```

MVC API:

```java

import static org.jooby.Deferred.deferred;
...

  @GET
  @Path("/")
  public Deferred home() {
    return deferred(() -> {
      return ...;
    });
  }

  @GET
  @Path("/db")
  public Deferred db() {
    return deferred("jdbc", () -> {
      return ...;
    });
  }
```

It's worth mentioning that the [executor(ExecutorService)]({{defdocs}}/Jooby.html#executor-java.util.concurrent.ExecutorService-) methods automatically `shutdown` at application shutdown time.

## promise

The {{deferred}} contains two useful methods:

* [deferred#resolve]({{defdocs}}/Deferred.html#resolve-java.lang.Object-)
* [deferred#reject]({{defdocs}}/Deferred.html#reject-java.lang.Throwable-)

These two methods allow you to use a {{deferred}} object as a `promise`:

Script API:

```java
{
  get("/", promise(deferred -> {
    try {
      deferred.resolve(...);
    } catch (Throwable x) {
      deferred.reject(x);
    }
  });
}
```

MVC API:

```java
  @Path("/")
  @GET
  public Deferred promise() {
    return new Deferred(deferred -> {
      try {
        deferred.resolve(...);
      } catch (Throwable x) {
        deferred.reject(x);
      }
    });
  }
```

The **"promise"** version of the {{deferred}} object is a key concept for integrating with external libraries.

## advanced configuration

Suppose you want to build a truly async application and after a **deep analysis** of your business you realize your application need to:

* Access a database
* Call a remote service
* Make a CPU intensive computation

These are the 3 points where your application is supposed to block and wait for a result.

Let's start by reducing the **worker thread pool** to the number of **available processors**:

```
server.threads.Min = ${runtime.processors}
server.threads.Max = ${runtime.processors}
```

With this change, you need to be careful and **don't run blocking code** on routes anymore. Otherwise performance will be affected.

Let's create a custom thread pool for each blocking access:

```java
{
  executor("db", Executors.newCachedThreadPool());
  executor("remote", Executors.newFixedThreadPool(32));
  executor("intensive", Executors.newSingleThreadExecutor());
}
```

For `database` access, we use a `cached` executor that will grow without a limit but free and release threads that are idle after `60s`.

For `remote` service, we use a `fixed` executor of `32` threads. The number here: `32` is just a random number for the purpose of the example.

For `intensive` computation, we use a `single` thread executor. Computation is too expensive and we want **one and only one** running at any time.

```java
{
  executor("db", Executors.newCachedThreadPool());
  executor("remote", Executors.newFixedThreadPool(32));
  executor("intensive", Executors.newSingleThreadExecutor());

  get("/nonblocking", () -> "I'm nonblocking");

  get("/list", deferred("db", () -> {
    Database db = require(Database.class);
    return db.fetch();
  });
  
  get("/remote", deferred("remote", () -> {
    RemoteService rs = require(RemoteService.class);
    return rs.call();
  });

  get("/compute", deferred("intensive", () -> {
    return someCPUIntensiveTask();
  });
}
```

Here is the same example with [rx java](https://github.com/ReactiveX/RxJava):

```java
{
  get("/nonblocking", () -> "I'm nonblocking");

  get("/list", deferred(() -> {
    Database db = require(Database.class);
    Observable.<List<String>> create(s -> {
      s.onNext(db.fetch());
      s.onCompleted();
    }).subscribeOn(Schedulers.io())
      .subscribe(deferred::resolve, deferred::reject);
    }));
  });

  get("/remote", deferred(() -> {
    RemoteService rs = require(RemoteService.class);
    Observable.<List<String>> create(s -> {
      s.onNext(rs.call());
      s.onCompleted();
    }).subscribeOn(Schedulers.io())
      .subscribe(deferred::resolve, deferred::reject);
    }));
  });
  
  get("/compute", deferred(() -> {
    Observable.<List<String>> create(s -> {
      s.onNext(someCPUIntensiveTask());
      s.onCompleted();
    }).subscribeOn(Schedulers.computation())
      .subscribe(deferred::resolve, deferred::reject);
    }));
  });
}
```

The main differences are:

* we keep the default executor: `direct`. So we don't create a new thread and avoid context switching.
* we use {{deferred}} object as `promise` and integrate with [rx java](https://github.com/ReactiveX/RxJava).
* different thread pool semantic is done via [rx schedulers](http://reactivex.io/documentation/scheduler.html).

This is just one more example to demonstrate the value of the {{deferred}} object, because we provide an [rxjava](/doc/rxjava) module which takes care of binding {{deferred}} object into `Observables`.

That sums up everything about the {{deferred}} object. It allows you to build async and reactive applications and at the same time: **keep it simple** (a Jooby design goal).

You're also invited to check out the available [async/reactive modules](/doc/async).
