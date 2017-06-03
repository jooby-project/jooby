[![Build Status](https://travis-ci.org/jooby-guides/{{guide}}.svg?branch=master)](https://travis-ci.org/jooby-guides/{{guide}})

# restful API with Hibernate

In this guide you will learn how to build a rest API for ```Pets``` and persist them in a **relational database**. We are going to save our pets using [hibernate](https://github.com/jooby-project/jooby/tree/master/jooby-hbm).

* List pets in from store:

```
GET http://localhost:8080/pets
```

* Get a pet by ID:

```
GET http://localhost:8080/pets/:id
```

* Creates a new pet from HTTP json body:

```
POST http://localhost:8080/pets
```

* Updates a pet from HTTP json body:

```
PUT http://localhost:8080/pets
```

* Delete a pet by ID:

```
DELETE http://localhost:8080/pets/:id
```

# requirements

Make sure you have all these software installed it in your computer:

* A text editor or IDE
* {{java}} or later
* {{maven}}

# ready

Open a terminal (console for Windows users) and paste:

```bash
mvn archetype:generate -B -DgroupId={{pkgguide}} -DartifactId={{guide}} -Dversion=1.0 -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
```

An almost empty application is ready to run, you can try now with:

```
cd {{guide}}

mvn jooby:run
```

Open a browser and type:

```
http://localhost:8080
```

# getting dirty

## json

Our API will use JSON, so let's add the [jackson]({{gh-prefix}}-jackson) dependency and import into our ```App.java```:

```java
import org.jooby.json.Jackson;
...
{
  use(new Jackson());
}
```

## connecting to a database

Now, go to your ```pom.xml``` and add the [hbm]({{gh-prefix}}-hbm) dependency.

Import and use it into your ```App.java```:

```java
...
import org.jooby.jdbi.Jdbi;
...
{
  use(new Hbm());
}
```

The [hbm module]({{gh-prefix}}-hbm) extends the [jdbc module]({{gh-prefix}}-jdbc). The [jdbc module]({{gh-prefix}}-jdbc) give us access to relational databases and exports a {{hikari}} **high performance connection pool**.

To connect to a database, we have to define our database connection properties in ```conf/application.conf```:

```
db = mem
```

The ```mem``` or ```fs``` are special databases. In order to use them we need the {{h2}} driver, so let's add it:

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
</dependency>
```

> **NOTE**: If you want to connect to ```mySQL``` database (or any other), then you'll have to add the ```mySQL Java Driver``` to your ```pom.xml``` and define the connection properties like:
>
> ```
> db.url = "jdbc:mysql//localhost/pets"
> db.user = "user"
> db.password = "password"
> ```

Open a console and run the application:

```
mvn jooby:run
```

> **TIP**: If you are using an IDE that automatically compiles your source code while you save it... ```mvn jooby:run``` will detects those changes and restart the application for you!! more at {{joobyrun}}.

Look at the console, you should see something like:

```
INFO  org.hibernate.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [
  name: jdbc:h2:mem:1451864078757;DB_CLOSE_DELAY=-1
  ...]
INFO  org.hibernate.Version - HHH000412: Hibernate Core {4.3.9.Final}
INFO  org.hibernate.cfg.Environment - HHH000206: hibernate.properties not found
INFO  org.hibernate.cfg.Environment - HHH000021: Bytecode provider name : javassist
INFO  com.zaxxer.hikari.HikariDataSource - h2.db - is starting.
INFO  com.zaxxer.hikari.HikariDataSource -   jdbc:h2:mem:1451864078757;DB_CLOSE_DELAY=-1
INFO  org.hibernate.annotations.common.Version - HCANN000001: Hibernate Commons Annotations {4.0.5.Final}
INFO  org.hibernate.dialect.Dialect - HHH000400: Using dialect: org.hibernate.dialect.H2Dialect
INFO  org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory - HHH000397: Using ASTQueryTranslatorFactory
INFO  org.hibernate.tool.hbm2ddl.SchemaUpdate - HHH000228: Running hbm2ddl schema update
INFO  org.hibernate.tool.hbm2ddl.SchemaUpdate - HHH000102: Fetching database metadata
INFO  org.hibernate.tool.hbm2ddl.SchemaUpdate - HHH000396: Updating schema
INFO  org.hibernate.tool.hbm2ddl.SchemaUpdate - HHH000232: Schema update complete
INFO  restfulhbm.App - [dev@netty]: Server started in 996ms

  * /**    [*/*]     [*/*]    (/hbm)

listening on:
  http://localhost:8080/
```

* Hibernate startup and connect to our ```mem``` database
* There is no persistent classes, but still try to update the schema. The ```hibernate.hbm2ddl``` is set to ```update``` while running in ```dev``` (a **Jooby** app by defaults run in dev mode).

## pet entity

Let's create a simple ```Pet``` class with an ```id```, ```name``` and getters/setters:

```java
package restfulhbm;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Pet {

  @Id @GeneratedValue
  private Integer id;

  private String name;

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public void setName(final String name) {
    this.name = name;
  }

  ... equals and hashcode

}
```

A **JPA** entity must be annotated with ```@Entity``` and define an ```@Id```. More at [Introduction to the Java Persistence API](http://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html).

**Entities** are not automatically discovered at runtime. Instead you must explicitly tell what entities are persistent:

```java
...
{
  ...
  use(new Hbm(Pet.class));
}
```

By doing this, you have full control of what entities should be managed by {{hibernate}} but also, your **application start as quick as possible**.

Of course, if you don't care about application startup time or you have a medium/complex app with a lots of entities you can **enable package scanning** with:

```java
package restfulhbm;
...
{
  use(new Hbm().scan());
}
```

The ```scan()``` method will discover any ```@Entity``` defined in the ```restfulhbm.*``` package (or sub-package).

## listing pets

Find ```App.java``` and add a new route:

```java
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
...
{
  ...
  /** List pets. */
  get("/pets", req -> {
    EntityManager em = require(EntityManager.class);
    TypedQuery<Pet> query = em.createQuery("from Pet", Pet.class);
    return query.getResultList();
  });
}
```

* We require an ```EntityManager```, this service is provided by the [hbm module]({{gh-prefix}}-hbm)
* We create a query with: ```em.createQuery("from Pet", Pet.class)```
* We execute and return the query result

Let’s add a start and max parameters to our listing service:

```java
{
  /** List pets. */
  get("/pets", req -> {
    EntityManager em = require(EntityManager.class);
    TypedQuery<Pet> query = em.createQuery("from Pet", Pet.class)
          .setFirstResult(req.param("start").intValue(0))
          .setMaxResults(req.param("max").intValue(20));
    return query.getResultList();
  });
}
```

Now, our listing service will returns a ```max``` of ```20``` results by default, from the beginning ```start=0```. This two calls are exactly the same:

```
http://localhost:8080/pets
http://localhost:8080/pets?start=0&max=20
```

## get a pet by ID

```java
...
{
  ...
  /** Get a pet by ID. */
  get("/pets/:id", req -> {
    EntityManager em = require(EntityManager.class);
    Pet pet = em.find(Pet.class, req.param("id").intValue());
    if (pet == null) {
      throw new Err(Status.NOT_FOUND);
    }
    return pet;
  });
}
```

* We define ID path variable: ```/pets/:id```
* We require an ```EntityManager```
* We find a pet by ID: ```em.find(Pet, id)```
* If there is no pet, service response with a ```404```
* Otherwise, we return the pet.

## saving a pet

So far, we see how to query pets by ID or listing all them, it is time to see how to creates a new pet:

```java
...
{
  ...
  /** Create new pet. */
  post("/pets", req -> {
    EntityManager em = require(EntityManager.class);
    Pet pet = req.body().to(Pet.class);
    em.persist(pet);
    return pet;
  });
}
```

* We define a new route: ```POST /pets```
* We require an ```EntityManager```
* We read a pet from a JSON HTTP body.
* We insert a pet by calling: ```em.persist(Object)```
* We return the inserted pet

Cool! But what about transactions?

Transactions are bound to the **HTTP Request**, if you have a closer look at the application logs, you will see something like:

```
  *    /**          [*/*]     [*/*]    (/hbm)
  GET  /pets        [*/*]     [*/*]    (/anonymous)
  GET  /pets/:id    [*/*]     [*/*]    (/anonymous)
  POST /pets        [*/*]     [*/*]    (/anonymous)
```


