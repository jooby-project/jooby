# modules

Modules are reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

## app module
An application module is represented by the [Jooby.Module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

Configure callback is similar to a [Guice module](https://github.com/google/guice), except you can acess to the [Mode](http://jooby.org/apidocs/org/jooby/Mode.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

In addition to the **configure** callback, a module in Jooby has two additional and useful methods:  **start** and **close**. If your module need/have to start an expensive resource, you should do it in the start callback and dispose/shutdown in the close callback.

From a module, you can bind your objects to the default [Guice scope](https://github.com/google/guice/wiki/Scopes) and/or to the Singleton scope.

An app module (might) defines his own set of defaults properties:

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      binder.bind(...).to(...);
    }

   public Config config() {
     return Config.parseResources(getClass(), "m1.properties");
   }
}
```
This is useful for setting defaults values or similar.

**Finally**, an app module is registered at startup time:

```java
import org.jooby.Jooby;

public class MyApp extends Jooby {

  {
     // as lambda
     use((mode, config, binder) -> {
        binder.bind(...).to(...);
     });
     // as instance
     use(new M1());
     use(new M2());
  }

  ...
}
```

Now, let's say M1 has a ```foo=bar``` property and M2 ```foo=foo``` then ```foo=foo``` wins! because last registered module can override/hide a property from previous module.

Cool, isn't?


## request module
A request module is represented by the [Request.Module]({{apidocs}}/Request.Module.html). The configure callback looks like:

```java
public class RM1 implements Request.Module {
   public void configure(Binder binder) {
      binder.bind(...).to(...);
   }
}
```

A request module is useful to wire and provide request scoped objects. In jooby, if you need a request scoped object you must bind it from a request module. 

A **new child injector** is created every time a new request is processed by Jooby.

### scope

If you need a single instance per request you need to bind the object with the **@Singleton** annotation. Otherwise, a new object will be created every time Guice need to inject a dependency.

```java
public class RM1 extends Request.Module {
   public void configure(Binder binder) {
      // request scoped
      binder.bind(...).to(...).in(Singleton.class);
     // or
     binder.bind(...).toInstance(...);
   }
}
```

Annotations like **RequestScoped** or **SessionScoped** are not supported in Jooby and they are ignored. A **request scoped** objects must be **explicitly declared inside a request module**.

There are two way of registering a request module:

1) by calling [Jooby.use(module)]({{apidocs}}/org/jooby/Jooby.html#use-org.jooby.Request.Module-)

```java
{
  // as lambda
  use(binder -> {
    binder.bind(...).to(...);
  });

  // as instance
  use(new RM1());
}
```

2) from [Jooby.Module.configure(module)]({{apidocs}}/org/jooby/Jooby.htmll#configure-org.jooby.Mode-com.typesafe.config.Config-com.google.inject.Binder-)

```java
public class M1 implements Jooby.Module {
    public void configure(Mode mode, Config config, Binder binder) {
      Multibinder<Request.Module> rm = Multibinder.newSetBinder(binder, Request.Module.class);
      rm.addBinding().toInstance(b -> {
        b.bind(...).toInstance(...);
      });
    }
}
```
