# jooby-camel

[Apache Camel](http://camel.apache.org) integration for Jooby.

Exposes a [CamelContext], [ProducerTemplate] and [ConsumerTemplate].

NOTE: This module was designed to provide a better integration with Jooby. This module doesn't
depend on [camel-guice](http://camel.apache.org/guice.html), but it provides similar features.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-camel</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Camel()
    .routes((rb, config) -> {
      rb.from("direct:noop").to("mock:out");
    })
  );

  get("/noop", req -> {
    req.require(ProducerTemplate.class).sendBody("direct:noop", "NOOP");
    return "/noop";
  });

}
```

Previous example, add a direct route using the Java DSL. A route builder can be created and
injected by Guice, see next section.

## camel routes

```java
public class MyRoutes extends RouteBuilder {

  @Inject
  public MyRoutes(Service service) {
    this.service = service;
  }

  public void configure() {
    from("direct:noop").to("mock:out").bean(service);
  }
}

...
{
  use(new Camel().routes(MyRoutes.class));
}
```

or without extending RouteBuilder:

```java
public class MyRoutes {

  @Inject
  public MyRoutes(RouteBuilder router, Service service) {
    router.from("direct:noop").to("mock:out").bean(service);
  }

}

...
{
  use(new Camel().routes(MyRoutes.class));
}
```

## configuration

Custom configuration is achieved in two ways:

### application.conf

A [CamelContext] can be configured from your ```application.conf```:

```properties
camel.handleFault = false
camel.shutdownRoute = Default
camel.shutdownRunningTask = CompleteCurrentTaskOnly
camel.streamCaching.enabled = false
camel.tracing = false
camel.autoStartup = true
camel.allowUseOriginalMessage = false
camel.jmx = false
```

Same for [ShutdownStrategy]:

```properties
camel.shutdown.shutdownRoutesInReverseOrder = true
camel.shutdown.timeUnit = SECONDS
camel.shutdown.timeout = 10
```

[ThreadPoolProfile]:

```properties
camel.threads.poolSize = ${runtime.processors-plus1}
camel.threads.maxPoolSize = ${runtime.processors-x2}
camel.threads.keepAliveTime = 60
camel.threads.timeUnit = SECONDS
camel.threads.rejectedPolicy = CallerRuns
camel.threads.maxQueueSize = 1000
camel.threads.id = default
camel.threads.defaultProfile = true
```

and [StreamCachingStrategy]:

```properties
camel.streamCaching.enabled = false
camel.streamCaching.spoolDirectory = ${application.tmpdir}${file.separator}"camel"${file.separator}"#uuid#"
```

### programmatically

Using the ```doWith(Configurer)``` method:

```java
{
  use(new Camel().doWith((ctx, config) -> {
    // set/override any other property.
  }));
}
```

That's all folks! Enjoy it!!!

{{appendix}}
