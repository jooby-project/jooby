# application

A Jooby app looks like:

```java
public class App extends Jooby { // 1.

  {
    // 2. use a module
    use(new MyModule());

    // 2. listen for application start and stop events.
    onStart(() -> log.info("starting app"));
    onStop(() -> log.info("stopping app"));

    // 2. add a route
    get("/", () -> "Hello");
  }

  public static void main(String[] args) throws Exception {
    // 3. run my app
    run(App::new, args);
  }
}
```

1) You create a new App by extending Jooby

2) From the instance initializer you can use modules, define routes add life cycle callbacks

3) Finally run the application.


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

Modules are covered later all you need to know now is that you can start/stop module as you usually do with your application. 

### callbacks order

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

Order is useful for service dependencies, like ServiceB should be started after ServiceA.

### service registry

You have access to the the service registry and start or stop too:

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

If you prefer the annotation way... you can too:

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

It works as expected just make sure ```MyService``` is a **Singleton** object.
