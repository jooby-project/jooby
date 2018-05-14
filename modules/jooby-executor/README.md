[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-executor/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-executor)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-executor.svg)](https://javadoc.io/doc/org.jooby/jooby-executor/1.3.0)
[![jooby-executor website](https://img.shields.io/badge/jooby-executor-brightgreen.svg)](http://jooby.org/doc/executor)
# executor

Manage the life cycle of {@link ExecutorService} and build async apps, schedule tasks, etc...

## exports

* One or more ```ExecutorService``` or sub-types of it

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-executor</artifactId>
 <version>1.3.0</version>
</dependency>
```

## usage

```java
...
import org.jooby.exec.Exec;
...
{
  use(new Exec());

  get("/", req -> {
    ExecutorService executor = require(ExecutorService.class);
    // work with executor
  });

}
```

The default executor is a ```Executors#newFixedThreadPool(int)``` with threads defined by ```Runtime#availableProcessors()```.

## explicit creation

The default ```ExecutorService``` is nice and give you something that just works out of the box. But, what if you need to control the number of threads?

Explicit control is provided via ```executors``` which allow the following syntax:

```java
type (= int)? (, daemon (= boolean)? )? (, priority (= int)? )?
```

Let's see some examples:

```
# fixed thread pool with a max number of threads equals to the available runtime processors
executors = "fixed"
```

```
# fixed thread pool with a max number of 10 threads
executors = "fixed = 10"
```

```
# fixed thread pool with a max number of 10 threads
executors = "fixed = 10"
```

```
# scheduled thread pool with a max number of 10 threads
executors = "scheduled = 10"
```

```
# cached thread pool with daemon threads and max priority
executors = "cached, daemon = true, priority = 10"
```

```
# forkjoin thread pool with asyncMode
executors = "forkjoin, asyncMode = true"
```

## multiple executors

Multiple executors are provided by expanding the ```executors``` properties, like:

```
executors {
   pool1: fixed
   jobs: forkjoin
 }
```

Later, you can request your executor like:

```java
{
  use(new Exec());

  get("/", req -> {
    ExecutorService pool1 = require("pool1", ExecutorService.class);
    ExecutorService jobs = require("jobs", ExecutorService.class);
  });
}
```

## deferred

Executors created by this module can be referenced by [deferred results](/apidocs/org/jooby/Deferred.html):

```java
{
  use(new Exec());

  get("/", deferred("pool1", () -> {
    return "from pool1";
  });

  get("/", deferred("jobs", () -> {
    return "from jobs";
  });
}
```

## shutdown

Any ```ExecutorService``` created by this module will automatically shutdown on application shutdown time.
