# application

A Jooby app looks like:

```java
public class App extends Jooby { // 1.

  {
    // 2. add a route
    get("/", () -> "Hello");
  }

  public static void main(String[] args) {
    // 3. run my app
    run(App::new, args);
  }
}
```

1) Create a new App extending Jooby

2) Define your application in the `instance initializer`

3) Run the application.

## life cycle

Application provides start and stop events. These events are useful for starting/stopping services.

### onStart/onStop events

Start/stop callbacks are accessible via application:

```java
{
   onStart(() -> {
     log.info("starting app");
   });
   onStop(() -> {
     log.info("stopping app");
   });
}
```

Or via module:

```java
public class MyModule implements Jooby.Module {

  public void configure(Env env, Config conf, Binder binder) {

    env.onStart(() -> {
      log.info("starting module");
    });
    env.onStop(() -> {
      log.info("stopping module");
    });
  }

}
```

Modules are covered later all you need to know now is that you can start/stop module as you usually do from your application. 

### order

Callback order is preserved:

```java
{
  onStart(() -> {
    log.info("first");
  });

  onStart(() -> {
    log.info("second");
  });

  onStart(() -> {
    log.info("third");
  });

}
```

Order is useful for service dependencies, like `ServiceB` should be started after `ServiceA`.

### service registry

You have access to the the service registry from start/stop events:

```java
{
  onStart(registry -> {
    MyService service = registry.require(MyService.class);
    service.start();
  });

  onStop(registry -> {
    MyService service = registry.require(MyService.class);
    service.stop();
  });
}
```

### PostConstruct/PreDestroy annotations

If you prefer the annotation way then:

```java
@Singleton
public class MyService {

  @PostConstruct
  public void start() {
   // ...
  }

  @PreDestroy
  public void stop() {
    // ...
  }

}

App.java:
{
  lifeCycle(MyService.class);
}
```

Service must be a **Singleton** object.
