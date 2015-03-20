# modules

Modules are reusable and configurable piece of software. Modules like in [Guice](https://github.com/google/guice) are used to wire services, connect data, etc...

## app module
An application module is represented by the [Jooby.Module](http://jooby.org/apidocs/org/jooby/Jooby.Module.html) class. The configure callback looks like:

```java
public class M1 implements Jooby.Module {
    public void configure(Env env, Config config, Binder binder) {
      binder.bind(...).to(...);
    }
}
```

The configure callback is similar to a [Guice module](https://github.com/google/guice), except you can access to the [Env](http://jooby.org/apidocs/org/jooby/Env.html) and [Type Safe Config](https://github.com/typesafehub/config) objects.

In addition to the **configure** callback, a module in Jooby has one additional method:  **config**. The ```config``` method allow a module to specify default properties, by default a module has an empty config.


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
