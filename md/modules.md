# modules

Modules are a key concept for building reusable and configurable piece of software.

Modules are thin and do a lot of work to bootstrap and configure the external library, but they **don't provide a new level of abstraction** nor [do] they provide a custom API to access functionality in that library. Instead they expose the library components as they are

Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

There is an extensive [module ecosystem](/modules) which makes Jooby a **full stack** framework (when need it).

## do less and be flexible

**Do less** might sounds confusing, but is the key to flexibility.

A module should do as **less as possible** (key difference with other frameworks). A module for a library *X* should:

* Bootstrap X
* Configure X
* exports raw API of X

This means a module should NOT create wrapper for a library. Instead, provides a way to extend, configure and use the raw library.

> This principle, keep module usually small, maintainable and flexible.

## creating modules

A module is represented by the [Jooby.Module]({{defdocs}}/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config conf, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

The configure callback is similar to a [Guice module](https://github.com/google/guice), except you can access to the [Env]({{defdocs}}/Env.html) and [config](https://github.com/typesafehub/config) objects.

## module properties

In addition to the **configure** callback, a module in {{Jooby}} has one additional method:  **config**. The ```config``` method allows a module to set default properties.

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

## usage

A module is registered at startup time:

```java
import org.jooby.Jooby;

public class MyApp extends Jooby {

  {
     // as lambda
     use((env, config, binder) -> {
        binder.bind(...).to(...);
     });
     // as instance
     use(new M1());
     use(new M2());
  }

}
```
