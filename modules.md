# modules

Modules are a key concept for building reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

A module is usually a small piece of software that bootstrap and configure common code and/or an external library.

### do less and be flexible

A module should do as less as possible (key difference with other frameworks). A module for a library *X* should:

* Bootstrap X
* Configure X
* Exposes raw API of X

This means a module should NOT create wrapper for a library. Instead, it should provide a way to extend, configure and use the raw library.

This principle, keep module usually small, maintainable and flexible.

A module is represented by the [Jooby.Module](/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

The configure callback is similar to a [Guice module](https://github.com/google/guice), except you can access to the [Env](/apidocs/org/jooby/Env.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

In addition to the **configure** callback, a module in Jooby has one additional method:  **config**. The ```config``` method allow a module to specify default properties.

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }

   public Config config() {
     return Config.parseResources(getClass(), "m1.properties");
   }
}
```

This is useful for setting defaults values or similar.

A module is registered at startup time:

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

}
```

Cool, isn't?
