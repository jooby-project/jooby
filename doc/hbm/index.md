---
layout: index
title: hbm
version: 0.8.1
---

# jooby-hbm

Object-Relational-Mapping via [Hibernate](http://hibernate.org/). Exposes an ```EntityManagerFactory``` and ```EntityManager``` services.

This module extends [jdbc](/doc/jooby-dbc) module, before going forward, make sure you read the doc of the [jdbc](/doc/jooby-dbc) module first.

This module provides an advanced and recommended [Open Session in View](https://developer.jboss.org/wiki/OpenSessionInView#jive_content_id_Can_I_use_two_transactions_in_one_Session)
pattern, which basically keeps the ```Session``` opened until the view is rendered. But it uses two database transactions:

1) first transaction is committed before rendering the view and then

2) a read only transaction is opened for rendering the view.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbm</artifactId>
  <version>0.8.1</version>
</dependency>
```

## usage

```java
{
  use(new Hbm(EntityA.class, EntityB.class));

  get("/", req -> {
   EntityManager em = req.require(EntityManager.class);
   // work with em...
  });
}
```

At bootstrap time you will see something similar to this:

```bash
  *    /**      [*/*]    [*/*]  (hbm)
```

That is the filter with the <strong>Open Session in View</strong> pattern.

## life-cycle

You are free to inject an ```EntityManagerFactory``` create a new
```EntityManagerFactory#createEntityManager()```, start transactions and do everything you
need.

For the time being, this doesn't work for an ```EntityManager```. An ```EntityManager``` is
bound to the current request, which means you can't freely access from every single thread (like
manually started thread, started by an executor service, quartz, etc...).

Another restriction, is the access from ```Singleton``` services. If you need access from a
singleton services, you need to inject a ```Provider```.

```java
@Singleton
public class MySingleton {
 
  @Inject
  public MySingleton(Provider&lt;EntityManager&gt; em) {
    this.em = em;
  }
}
```

This is because the ```EntityManager``` is bound as [RequestScoped]({{defdocs/RequestScoped.html}}).


Still, we strongly recommend to leave your services in the default scope and avoid to use
```Singleton``` objects, except of course for really expensive resources. This is also
recommend it by [Guice](https://github.com/google/guice).

Services in the default scope won't have this problem and are free to inject the ```EntityManager``` directly.

## persistent classes

Classpath scanning is OFF by default, so you need to explicitly tell [Hibernate](http://hibernate.org/) which classes are
persistent. This intentional and helps to reduce bootstrap time and have explicit control over
persistent classes.

If you don't care about bootstrap time and/or just like the auto-discover feature, just do:

```java
{
  use(new Hbm().scan());
}
```

After calling ```scan()```, [Hibernate](http://hibernate.org/) will auto-discover all the entities application's
namespace. The namespace is defined by the package of your application. Given:
```org.myproject.App``` it will scan everything under ```org.myproject```.

## options

[Hibernate](http://hibernate.org/) options can be set from your ```application.conf``` file, just make sure to prefix them with ```hibernate.*```

```properties
hibernate.hbm2ddl.auto = update
```


# appendix: hbm.conf

```properties
hibernate {
  id.new_generator_mappings = true
  archive.autodetection = class

  # update for dev, validate for others
  # hbm2ddl.auto = update

  current_session_context_class = managed
}

javax.persistence {
}

```
