# hibernate

<a href="http://hibernate.org/orm">Hibernate ORM</a> enables developers to more easily write applications whose data outlives the application process. As an Object/Relational Mapping (ORM) framework, Hibernate is concerned with data persistence as it applies to relational databases.

This module setup and configure <a href="http://hibernate.org/orm">Hibernate ORM</a> and ```JPA Provider```.

This module depends on [jdbc](/doc/jdbc) module, make sure you read the doc of the [jdbc](/doc/jdbc) module.

## exports

* SessionFactory / EntityManagerFactory 
* Session / EntityManager 
* UnitOfWork 

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-hbm</artifactId>
 <version>1.1.1</version>
</dependency>
```

## usage

```java
{
  use(new Hbm("jdbc:mysql://localhost/mydb")
      .classes(Beer.class)
  );

  get("/api/beer/", req -> {
    return require(UnitOfWork.class).apply(em -> {
      return em.createQuery("from Beer").getResultList();
    });
  });
}
```

## unit of work

We provide an [UnitOfWork](/apidocs/org/jooby/hbm/UnitOfWork.html) to simplify the amount of code required to interact within the database.

For example the next line:

```java
{
  require(UnitOfWork.class).apply(em -> {
    return em.createQuery("from Beer").getResultList();
  });
}
```

Is the same as:

```java
{
   Session session = require(SessionFactory.class).openSession();
   Transaction trx = session.getTransaction();
   try {
     trx.begin();
     List<Beer> beers = em.createQuery("from Beer").getResultList();
     trx.commit();
   } catch (Exception ex) {
     trx.rollback();
   } finally {
     session.close();
   }
}
```

An [UnitOfWork](/apidocs/org/jooby/hbm/UnitOfWork.html) takes care of transactions and session life-cycle. It's worth to mention too that a first requested [UnitOfWork](/apidocs/org/jooby/hbm/UnitOfWork.html) bind the `Session` to the current request. If later in the execution flow an [UnitOfWork](/apidocs/org/jooby/hbm/UnitOfWork.html), `Session` and/or `EntityManager` is required then the one that belong to the current request (first requested) will be provided it.

## open session in view

We provide an advanced and recommended <a href="https://developer.jboss.org/wiki/OpenSessionInView#jive_content_id_Can_I_use_two_transactions_in_one_Session">Open Session in View</a> pattern, which basically keep the `Session` opened until the view is rendered, but it uses two database transactions:

* first transaction is committed before rendering the view and then 

*  a read only transaction is opened for rendering the view 

Here is an example on how to setup the open session in view filter:

```java
{
   use(new Hbm());
   use("*", Hbm.openSessionInView());
}
```

## event listeners

JPA event listeners are provided by Guice, which means you can inject dependencies into your event listeners:

```java
@Entity
@EntityListeners({BeerListener.class})
public class Beer {
}

public class BeerListener {

  @Inject
  public BeerListener(DependencyA depA) {
    this.depA = depA;
  }

  @PostLoad
  public void postLoad(Beer beer) {
    this.depA.complementBeer(beer);
  }
}
```

Hibernate event listeners are supported too via [onEvent(EventType, Class)](/apidocs/org/jooby/hbm/Hbm.html#onEvent-org.hibernate.event.spi.EventType-java.lang.Class-):

```java
{
   use(new Hbm()
       .onEvent(EventType.POST_LOAD, MyPostLoadListener.class));
}
```

Again, ```MyPostLoadListener``` will be provided by Guice.

## persistent classes

Persistent classes must be provided at application startup time via [classes(Class...)](/apidocs/org/jooby/hbm/Hbm.html#classes-java.lang.Class...-):

```java
{
  use(new Hbm()
      .classes(Entity1.class, Entity2.class, ..., )
  );

}
```

Or via `scan`:

```java
{
  use(new Hbm()
      .scan()
  );

}
```

Which ```scan``` the application package, or you can provide where to look:


```java
{
  use(new Hbm()
      .scan("foo.bar", "x.y.z")
  );

}
```

## advanced configuration

Advanced configuration is provided via [doWith(Consumer)](/apidocs/org/jooby/hbm/Hbm.html#doWith-java.util.function.Consumer-) callbacks:

```java
{
  use(new Hbm()
      .doWith((BootstrapServiceRegistryBuilder bsrb) -> {
        // do with bsrb
      })
      .doWith((StandardServiceRegistryBuilder ssrb) -> {
        // do with ssrb
      })
  );

}
```

Or via ```hibernate.*``` property from your ```.conf``` file:

```
hibernate.hbm2ddl.auto = update
```

## life-cycle

You are free to inject a `SessionFactory` or `EntityManagerFactory` and create a new `EntityManagerFactory#createEntityManager()`, start transactions and do everything you need.

For the time being, this doesn't work for a `Session` or `EntityManager`. A `Session` and/or `EntityManager` is bound to the current request, which means you can't freely access from every single thread (like manually started thread, started by an executor service, quartz, etc...).

Another restriction, is the access from `Singleton` services. If you need access from a singleton services, you need to inject a `Provider`.

```java
@Singleton
public class MySingleton {

  @Inject
  public MySingleton(Provider<EntityManager> em) {
    this.em = em;
  }
}
```

Still, we strongly recommend to leave your services in the default scope and avoid `Singleton` objects, except of course for really expensive resources. This is also recommend approach by Guice.

Services in the default scope won't have this problem and are free to inject the `Session` or `EntityManager` directly.

## hbm.conf

```properties
hibernate.session_factory_name_is_jndi = false

hibernate.archive.autodetection = class

hibernate.current_session_context_class = managed
```
