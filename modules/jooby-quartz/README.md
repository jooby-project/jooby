[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-quartz/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-quartz/1.6.4)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-quartz.svg)](https://javadoc.io/doc/org.jooby/jooby-quartz/1.6.4)
[![jooby-quartz website](https://img.shields.io/badge/jooby-quartz-brightgreen.svg)](http://jooby.org/doc/quartz)
# quartz

Cron triggers, job scheduling and async processing via [Quartz](http://quartz-scheduler.org/).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-quartz</artifactId>
  <version>1.6.4</version>
</dependency>
```

## usage

```java
import org.jooby.quartz.Quartz;
...
{
  use(new Quartz().with(MyJob.class));
}
```

Previous example will startup Quartz and schedule MyJob.

## jobs

A job can implement the ```Job``` interface as described in the [Quartz documentation](http://quartz-scheduler.org/documentation)

If you prefer to not implement the ```Job``` interface, all you have to do is to annotated a method with the ```Scheduled``` annotation.

By default, job name is set the class name or to the method name. Default group is set to the package name of the job class.

### job methods

A job method must follow these rules:

* It must be a public method
* Without a return value
* Have ZERO arguments
* or just ONE argument of type ```JobExecutionContext```

The next section will you show how to add a trigger to a job and some examples too.

## triggers

Trigger are defined by the ```Scheduled``` annotation. The annotation defines a single and required attributes, which is basically a trigger expression or a reference to one.

Examples:

Run every 5 minutes, start immediately and repeat for ever:

    @Scheduled("5m")
    @Scheduled("5m; delay=0")
    @Scheduled("5m; delay=0; repeat=*")

Previous, expressions are identical.

Run every 1 hour with an initial delay of 15 minutes for 10 times:

    @Scheduled("1h; delay=15m; repeat=10")

Fire at 12pm (noon) every day:

    0 0 12* ?


Fire at 10:15am every day:

    0 15 10 ?*

It is also possible to put the name of property:

    @Scheduled("job.expr")


## grouping jobs together

If you have two or more jobs doing something similar, it is possible to group all them into one single class:

```java
public class MyJobs {
  @Scheduled("5minutes")
  public void job1() {
    ...
  }

  @Scheduled("1h")
  public void job2() {
    ...
  }
}
```

## dependency injection

Not much to add here, just let you know jobs are created by Guice.

```java
public class MyJob {
  private A a;

  @Inject
   public MyJob(A a) {
     this.a = a;
   }

   @Scheduled("5minutes")
   public void doWork() {
     this.a.doWork();
   }
}
```

Injecting a ```Scheduler```

```java
public class MyJobManager {
  private Scheduler scheduler;

  @Inject
  public MyJobManager(Scheduler scheduler) {
    this.scheduler = scheduler;
  }
}
```

## configuration

Example: Setting max number of threads

```properties
# default is number of available processors

org.quartz.threadPool.threadCount = 1
```

Configuration follows the [Quartz
documentation](http://quartz-scheduler.org/documentation). The only difference is that you need to put add the properties on your ```*.conf``` file, NOT in a custom ```quartz.properties``` file.

### jdbc store

Jdbc Store is fully supported but it depends on the <code>jooby-jdbc</code> module. So, in order to use the Jdbc Store you need to follow these steps:

1st. Install the Jdbc module:

```java
{
  use(new Jdbc());
  use(new Quartz(MyJob.class));
}
```

2nd. Set the quartz properties:

```properties
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
org.quartz.jobStore.dataSource = db
```

## adding jobs programmatically

When ```Scheduled``` isn't not enough and/or if you prefer to build jobs manually, you can try one of the available alternatives.

Example 1: build the trigger and use default job naming

```java
{
  use(new Quartz()
    .with(MyJob.class, trigger -> {
      trigger
        .withSchedule(withIntervalInDays(3))
        .startAt(futureDate(10, MINUTES));
      })
  );
}
```

Example 2: build the job, the trigger and use default job naming

```java
{
  use(new Quartz()
    .with(MyJob.class, (job, trigger) -> {
      job.withDescription("etc...");
 
      trigger
        .withSchedule(withIntervalInDays(3))
        .startAt(futureDate(10, MINUTES));
    })
  );
}
```

Example 3: build and set everything from scratch

```java
{
  use(new Quartz()
   .with(
     newJob(MyJob.class).withDescription("etc...")
       .build(),
     newTrigger()
       .withSchedule(withIntervalInDays(3))
       .startAt(futureDate(10, MINUTES))
       .build()
   })
  );
}
```

## quartz.conf
These are the default properties for quartz:

```properties
org.quartz.scheduler.instanceName = quartz

org.quartz.scheduler.instanceId = local

# thread pool

org.quartz.threadPool.threadNamePrefix = quartz

org.quartz.threadPool.threadCount = ${runtime.processors}

org.quartz.scheduler.skipUpdateCheck = true
```
