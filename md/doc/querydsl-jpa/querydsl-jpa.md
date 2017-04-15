# querydsl-jpa

[Querydsl](http://www.querydsl.com/) unified Queries for Java. Querydsl is compact, safe and easy to learn.

This module provides [JPA](http://www.querydsl.com/static/querydsl/4.0.9/reference/html_single/#jpa_integration) integration.

> NOTE: this module doesn't provide JPA support to your application. Check out the [hbm module](/doc/hbm) for JPA support.

## usage

* Create a ```querydsl-jpa.activator``` file in the ```src/etc/``` directory.

* Open a terminal and type: ```mvn clean compile```

* Generated classes will be placed inside the ```target/generated-sources``` directory.

Of course, you need to define some entities and have **JPA** in your classpath. The [hibernate](/doc/hbm) module gives you JPA support.

## profile activation

Just create a ```src/etc/querydsl-jpa.activator``` file. The file contents doesn't matter, it just needs to be present.

The file ```src/etc/querydsl-jpa.activator``` will trigger a maven profile that does [all this](http://www.querydsl.com/static/querydsl/4.0.9/reference/html_single/#jpa_integration) for you.

## example

Here is an example with [hibernate](/doc/hbm):

* Source code:

```java

import org.jooby.hbm.Hbm;

class App extends Jooby {
  {
    // configure Hbm
    use(new Hbm(Pet.class));
    ...
  }
}

package domain;

@Entity
class Pet {
  @Id
  private int id;

  private String name;
}

```

* Write the ```src/etc/querydsl-jpa.activator``` file.

* Open a console and type: ```mvn clean compile```

* Find the generated source code at: ```target/generated-sources```

* Using ```JPAQuery```:

```java

import static domain.QPet.pet;
import com.querydsl.jpa.impl.JPAQuery;
import javax.persistence.EntityManager;

{
  // configure Hbm
  use(new Hbm(Pet.class));

  get("/pets/:name", req -> {
    String name = req.param("name").value();
    EntityManager em = require(EntityManager.class);

    return new JPAQuery(em)
      .from(pet)
      .where(pet.name.like(name))
      .fetch();
  })
}
```

[Querydsl](http://www.querydsl.com/) will find all your entities (classes annotated with @Entity) and generates a class with the same name, but prefixed with ```Q```, like ```QPet```.
