[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-metrics/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-metrics/1.5.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-metrics.svg)](https://javadoc.io/doc/org.jooby/jooby-metrics/1.5.1)
[![jooby-metrics website](https://img.shields.io/badge/jooby-metrics-brightgreen.svg)](http://jooby.org/doc/metrics)
# metrics

[Metrics](http://metrics.dropwizard.io) provides a powerful toolkit of ways to measure the behavior of critical components in your production environment.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-metrics</artifactId>
  <version>1.5.1</version>
</dependency>
```

## usage

```java
{
 use(new Metrics()
    .request()
    .threadDump()
    .ping()
    .healthCheck("db", new DatabaseHealthCheck())
    .metric("memory", new MemoryUsageGaugeSet())
    .metric("threads", new ThreadStatesGaugeSet())
    .metric("gc", new GarbageCollectorMetricSet())
    .metric("fs", new FileDescriptorRatioGauge())
    );
}
```

Let's see what all these means.

## metrics

Metrics are available at ```/sys/metrics``` or ```/sys/metrics/:type``` via:

```java
 use(new Metrics()
    .metric("memory", new MemoryUsageGaugeSet())
    .metric("threads", new ThreadStatesGaugeSet())
    .metric("gc", new GarbageCollectorMetricSet())
    .metric("fs", new FileDescriptorRatioGauge()));
```

The ```/:type``` parameter is optional and let you filter metrics by type ```counters```, ```guages```, etc..

There is a ```name``` filter too: ```/sys/metrics?name=memory``` or ```/sys/metrics/guages?name=memory```. The ```name``` parameter filter all the metrics where the name starts with the given ```name```.

## health checks

Health checks are available at ```/sys/healthCheck``` via:

```java
 use(new Metrics()
    .healthCheck("db", new DatabaseHealthCheck()));
```

## instrumented requests

Captures request information (like active requests or min/mean/max execution time) and a breakdown of the response codes being returned: [InstrumentedHandler](/apidocs/org/jooby/assets/InstrumentedHandler.html).

```java
 use(new Metrics()
    .request());
```

## thread dump

A thread dump is available at ```/sys/threadDump``` via:

```java
 use(new Metrics()
    .threadDump());
```

## reporting

Reporters are appended via a callback API:

```java
 {
   use(new Metrics()
      .reporter(registry -> {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();
        reporter.start(1, TimeUnit.MINUTES);
        return reporter;
      });
 }
```

You can add all the reporters you want. Keep in mind you have to start them (if need it), but you don't have to stop them as long they implements the [Closeable](/apidocs/org/jooby/assets/Closeable.html) interface.
