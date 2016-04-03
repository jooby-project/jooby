[![Build Status](https://travis-ci.org/jooby-guides/{{guide}}.svg?branch=master)](https://travis-ci.org/jooby-guides/{{guide}})

# restful API with JDBI

In this guide you will learn how to build a rest API for ```Pets``` and persist them in a **relational database**. We are going to save our pets using [jdbi](https://github.com/jooby-project/jooby/tree/master/jooby-jdbi), a fluent SQL API.

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

> **TIP**: If you are using an IDE that automatically compiles your source code while you save it... ```mvn jooby:run``` will detects those changes and restart the application for you!! more at {{joobyrun}}.

# getting dirty

## json

Our API will use JSON, so let's add the [jackson](https://github.com/jooby-project/jooby/tree/master/jooby-jdbi) dependency and import into our ```App.java```:

```java
import org.jooby.json.Jackson;
...
{
  use(new Jackson());
}
```

## pet class

Let's create a simple ```Pet``` class with an ```id```, ```name``` and getters/setters for them.

```java
package restjdbi;

public class Pet {

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

}
```

## connecting to a database

Now, go to your ```pom.xml``` and add the [jdbi](https://github.com/jooby-project/jooby/tree/master/jooby-jdbi) dependency.

Import and use it into your ```App.java```:

```java
...
import org.jooby.jdbi.Jdbi;
...
{
  use(new Jdbi());
}
```

The [jdbi module](https://github.com/jooby-project/jooby/tree/master/jooby-jdbi) extends the [jdbc module](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc). The [jdbc module](https://github.com/jooby-project/jooby/tree/master/jooby-jdbc) give us access to relational databases and exports a {{hikari}} **high performance connection pool**.

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

## creating a database

We are going to create a database schema at application startup time:

* Define a ```schema``` property in ```conf/application.conf``` like:

```
schema = """

  create table if not exists pets (

    id int not null auto_increment,

    name varchar(255) not null,

    primary key (id)

  );
"""
```

* Execute the script in ```App.java```:

```java
import org.skife.jdbi.v2.Handle;
...
{
  use(new Jdbi().doWith((dbi, conf) -> {
    try (Handle handle = dbi.open()) {
      handle.execute(conf.get("schema"));
    }
  }));
}
```

We add a multi-line string property (triple quotes) with our schema in ```conf/application.conf```.

The ```.doWith(DBI, Config)``` is a callback method and call it when ```DBI``` is ready.

A call to ```dbi.open()``` creates and opens a ```Handle``` for running our script. A ```Handle``` need to be release it, by calling ```handle.close()```, we are doing that with the ```try-with-resources``` statement available since Java 7.

With a database ready, we are going to build our REST API.

## listing pets

Find ```App.java``` and add a new route:

```java
import org.skife.jdbi.v2.Query;
...
{
  get("/pets", req -> {
    try (Handle h = req.require(Handle.class)) {
      Query<Pet> q = h.createQuery("select * from pets")
          .map(Pet.class);
      return q.list();
    }
  });
}
```

The ```req.require(Handle.class)``` give us a new ```Handle```, we create a new ```Query``` map the result to our ```Pet``` class and list all the results.

> NOTE: A call to ```req.require(Handle.class)``` is identical too ```req.require(DBI.class).open()```, so don't forget to close the ```Handle``` once you are done.

Let's add a ```start``` and ```max``` parameters to our list service:

```java
import org.skife.jdbi.v2.Query;
...
{
  get("/pets", req -> {
    try (Handle h = req.require(Handle.class)) {
      Query<Pet> q = h.createQuery("select * from pets limit :start, :max")
          .bind("start", req.param("start").intValue(0))
          .bind("max", req.param("max").intValue(20))
          .map(Pet.class);
      return q.list();
    }
  });
}
```

Now, our listing service will returns a ```max``` of ```20``` results by default, from the beginning ```start=0```. This two calls are exactly the same:

```
http://localhost:8080/pets
http://localhost:8080/pets?start=0&max=20
```

## get a pet by ID

Let's add a new route to get a single pet by ID:

```java
...
{
  ...
  get("/pets/:id", req -> {
    try (Handle h = req.require(Handle.class)) {
      Query<Pet> q = h.createQuery("select * from pets p where p.id = :id")
          .bind("id", req.param("id").intValue())
          .map(Pet.class);
      Pet pet = q.first();
      if (pet == null) {
        throw new Err(Status.NOT_FOUND);
      }
      return pet;
    }
  });
}
```

The SQL query defines an ```id``` parameter which that we bind to the path variable: ```id```.

We execute the query by calling ```q.first()``` and returns the pet or a ```404``` error response.

> **NOTE**: The ```Err``` exception is a special exception that carry an HTTP status code. More at [err handling](/doc/#error-handling).

Try it:

```
http://localhost:8080/pets/1
```

You'll see a ```404``` err page because we didn't persist any pet yet. Let's see how to save one.

## saving a pet

So far, we see how to query pets by ID or listing all them, it is time to see how to creates a new pet:

```java
{
  post("/pets", req -> {
    try (Handle handle = req.require(Handle.class)) {
      // read post from HTTP body
      Pet pet = req.body().to(Pet.class);
  
      GeneratedKeys<Map<String, Object>> keys = handle
          .createStatement("insert into pets (name) values (:name)")
          .bind("name", pet.getName())
          .executeAndReturnGeneratedKeys();
      Map<String, Object> key = keys.first();
      // get and set the auto-increment key
      Number id = (Number) key.values().iterator().next();
      pet.setId(id.intValue());
      return pet;
    }
  });
}
```

* We open a ```Handle``` with ```req.require(Handle.class)```
* We read the pet from the JSON HTTP body: ```req.body().to(Pet.class)```
* Insert a new pet with ```executeAndReturnGeneratedKeys()```
* Get the generated keys (ID is an auto-increment column)
* Lastly, we update the pet ID with the generated keys and returns the pet as HTTP response

## updating a pet

Updating a pet is quite similar:

```java
...
{
  ...
  put("/pets", req -> {
    try (Handle handle = req.require(Handle.class)) {
      // read from HTTP body
      Pet pet = req.body().to(Pet.class);
  
      int rows = handle
          .createStatement("update pets p set p.name = :name where p.id = :id")
          .bind("id", pet.getId())
          .execute();
  
      if (rows <= 0) {
        throw new Err(Status.NOT_FOUND);
      }
      return pet;
    }
  });
}
```

* We open a ```Handle``` with ```req.require(Handle.class)```
* We read the pet from the JSON HTTP body: ```req.body().to(Pet.class)```
* We update a pet by ID.
* If no rows were updated, then we returns a ```404```
* Otherwise, we return the updated pet

## delete a pet by ID

Again, delete operation is similar to update:

```java
...
{
  ...
  delete("/pets/:id", req -> {
    try (Handle handle = req.require(Handle.class)) {
      int rows = handle
          .createStatement("delete pets where p.id = :id")
          .bind("id", req.param("id").intValue())
          .execute();
  
      if (rows <= 0) {
        throw new Err(Status.NOT_FOUND);
      }
      return pet;
    }
  });
}
```

* We open a ```Handle``` with ```req.require(Handle.class)```
* We read ```id``` parameter with ```req.param("id").intValue()```
* We delete a pet by ID.
* If no rows were deleted, then we returns a ```404```
* Otherwise, we return a 204 response with ```Results.noContent()```

## reviewing API

We are done with our API, let's review how it looks:

```java
{
  /** JSON supports . */
  use(new Jackson());

  /** Create db schema. */
  use(new Jdbi().doWith((dbi, conf) -> {
    try (Handle handle = dbi.open()) {
      handle.execute(conf.getString("schema"));
    }
  }));

  /** List pets. */
  get("/pets", req -> {
    try (Handle h = req.require(Handle.class)) {
      Query<Pet> q = h.createQuery("select * from pets limit :start, :max")
          .bind("start", req.param("start").intValue(0))
          .bind("max", req.param("max").intValue(20))
          .map(Pet.class);
      return q.list();
    }
  });

  /** Get a pet by ID. */
  get("/pets/:id", req -> {
    try (Handle h = req.require(Handle.class)) {
      Query<Pet> q = h.createQuery("select * from pets p where p.id = :id")
          .bind("id", req.param("id").intValue())
          .map(Pet.class);
      Pet pet = q.first();
      if (pet == null) {
        throw new Err(Status.NOT_FOUND);
      }
      return pet;
    }
  });

  /** Create a pet. */
  post("/pets", req -> {
    try (Handle handle = req.require(Handle.class)) {
      // read from HTTP body
      Pet pet = req.body().to(Pet.class);

      GeneratedKeys<Map<String, Object>> keys = handle
          .createStatement("insert into pets (name) values (:name)")
          .bind("name", pet.getName())
          .executeAndReturnGeneratedKeys();
      Map<String, Object> key = keys.first();
      // get and set the autogenerated key
      Number id = (Number) key.values().iterator().next();
      pet.setId(id.intValue());
      return pet;
    }
  });

  /** Update a pet. */
  put("/pets", req -> {
    try (Handle handle = req.require(Handle.class)) {
      // read from HTTP body
      Pet pet = req.body().to(Pet.class);

      int rows = handle
          .createStatement("update pets p set p.name = :name where p.id = :id")
          .bind("id", pet.getId())
          .execute();

      if (rows <= 0) {
        throw new Err(Status.NOT_FOUND);
      }
      return pet;
    }
  });

  /** Delete a pet by ID. */
  delete("/pets/:id", req -> {
    try (Handle handle = req.require(Handle.class)) {
      // read from HTTP body
      Pet pet = req.body().to(Pet.class);

      int rows = handle
          .createStatement("delete pets where p.id = :id")
          .bind("id", pet.getId())
          .execute();

      if (rows <= 0) {
        throw new Err(Status.NOT_FOUND);
      }
      return Results.noContent();
    }
  });
}
```

Not bad, ugh?

Isn't, but did you see we have to repeat the ```/pets``` pattern for each of our REST operations?

Let's fix that with ```use("/path")```:

```java
{
  ...

  /** Pet API. */
  use("/pets")
      /** List pets. */
      .get(req -> {
        try (Handle h = req.require(Handle.class)) {
          Query<Pet> q = h.createQuery("select * from pets limit :start, :max")
              .bind("start", req.param("start").intValue(0))
              .bind("max", req.param("max").intValue(20))
              .map(Pet.class);
          return q.list();
        }
      })
      /** Get a pet by ID. */
      .get("/:id", req -> {
        try (Handle h = req.require(Handle.class)) {
          Query<Pet> q = h.createQuery("select * from pets p where p.id = :id")
              .bind("id", req.param("id").intValue())
              .map(Pet.class);
          Pet pet = q.first();
          if (pet == null) {
            throw new Err(Status.NOT_FOUND);
          }
          return pet;
        }
      })
      /** Create a pet. */
      .post(req -> {
        try (Handle handle = req.require(Handle.class)) {
          // read from HTTP body
          Pet pet = req.body().to(Pet.class);

          GeneratedKeys<Map<String, Object>> keys = handle
              .createStatement("insert into pets (name) values (:name)")
              .bind("name", pet.getName())
              .executeAndReturnGeneratedKeys();
          Map<String, Object> key = keys.first();
          // get and set the autogenerated key
          Number id = (Number) key.values().iterator().next();
          pet.setId(id.intValue());
          return pet;
        }
      })
      /** Update a pet. */
      .put(req -> {
        try (Handle handle = req.require(Handle.class)) {
          // read from HTTP body
          Pet pet = req.body().to(Pet.class);

          int rows = handle
              .createStatement("update pets p set p.name = :name where p.id = :id")
              .bind("id", pet.getId())
              .execute();

          if (rows <= 0) {
            throw new Err(Status.NOT_FOUND);
          }
          return pet;
        }
      })
      /** Delete a pet by ID. */
      .delete("/:id", req -> {
        try (Handle handle = req.require(Handle.class)) {
          // read from HTTP body
          Pet pet = req.body().to(Pet.class);

          int rows = handle
              .createStatement("delete pets where p.id = :id")
              .bind("id", pet.getId())
              .execute();

          if (rows <= 0) {
            throw new Err(Status.NOT_FOUND);
          }
          return Results.noContent();
        }
      });
}
```

Better now! The ```use``` method has many meanings in **Jooby**, If we use pass a ```String``` we can group route under a same path pattern.

# conclusion

As you already see, building an API that saves data in a **database** is very simple. Code looks clean and simple thanks to [jdbi]({{gh-prefix}}-jdbi).

[Jdbi]({{gh-prefix}}-jdbi) makes perfect sense if you want to have full control on your SQL queries, or if you don't like **ORM** tools too.

{{guides/guide.footer.md}}
