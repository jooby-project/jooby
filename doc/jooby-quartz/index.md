---
layout: index
title: jooby-quartz
version: 0.4.2.1
---

# jooby-quartz

A job scheduler from [Quartz](http://quartz-scheduler.org/).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-quartz</artifactId>
  <version>0.4.2.1</version>
</dependency>
```
## usage

```java
import org.jooby.quartz.Quartz;

{
  use(new Quartz().with(MyJob.class));
}
```

Previous example will startup Quartz and schedule MyJob.

# Jobs
A job can implement the ```Job``` interface as described in the [Quartz documentation]
(http://quartz-scheduler.org/documentation)

If you prefer to not implement the ```Job``` interface, all you have to do is to annotated a method with the ```Scheduled``` annotation.

By default, job name is set the class name or to the method name. Default group is set to the package name of the job class.

## Job methods

A job method must follow this rules:

* It must be a public method
* Without a return value
* Have ZERO arguments
* or just ONE argument of type ```JobExecutionContext```

The next section will you show how to add a trigger to a job and some examples too.

# Triggers

Trigger are defined by the ```Scheduled``` annotation. The annotation defined a single and required attributes, which is basically a trigger expression or a reference to it.

Example 1: run every 10s

```java
public class MyJob implements Job {
  @Scheduled("10s")
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ...
  }
}
```

Example 2: run every 10s (no ```Job```)

```java
public class MyJob {
  @Scheduled("10s")
  public void doWork() {
    ...
  }
}
```

The ```Scheduled``` define a ```Quartz Trigger```. There you can put expressions
like: ```5s```, ```15minutes```, ```2hours```, etc... or a CRON expression: ```0/3 * * * * ?```.

It is also possible to put the name of property:

```java
public class MyJob {
  @Scheduled("job.expr")
  public void doWork() {
    ...
  }
}
```

And again the property: ```job.expr``` must be one of the previously described expressions.
 

# Grouping jobs together

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

# Dependency Injection

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

# Configuration

Example: Setting max number of threads

```properties
# application.conf
org.quartz.threadPool.threadCount = 1 # default is number of available processors
```

Configuration follows the [Quartz
documentation](http://quartz-scheduler.org/documentation). The only difference is that you need to put add the properties on your ```*.conf``` file, NOT in a custom ```quartz.properties``` file.

## Jdbc Store

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

# Adding jobs programmatically

When ```Scheduled``` isn't not enough and/or if you prefer to build jobs manually, you can try
one of the available alternatives.

Example 1: build the trigger and use default job naming

```java
{
  use(new Quartz()
    .with(MyJob.class, trigger {@literal ->} {
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
    .with(MyJob.class, (job, trigger) {@literal ->} {
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

That's all folks! Enjoy it!!
