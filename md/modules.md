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
