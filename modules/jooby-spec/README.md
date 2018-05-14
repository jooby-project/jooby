[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-spec/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-spec)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-spec.svg)](https://javadoc.io/doc/org.jooby/jooby-spec/1.3.0)
[![jooby-spec website](https://img.shields.io/badge/jooby-spec-brightgreen.svg)](http://jooby.org/doc/spec)
# route spec

> **DEPRECATED**: This module has been deprecated. Please use the new [apitool](https://github.com/jooby-project/jooby/tree/master/jooby-apitool) module.

The spec module allows you to export your API/microservices outside a [Jooby](http://jooby.org) application.

The goal of this module is to define a common way to write APIs and provide tools like live doc and testing **with no extra effort**.

> You won't be forced to learn any other tool or annotate your code with special annotations. All you have to do is **write your application** following a few simple suggestions.

This module will process, collect and compile **routes** from your application. It extracts HTTP method, path patterns, parameters, response types and documentation.

You will find the basics and the necessary documentation to build and expose rich APIs for free, but keep in mind that this module isn't intended to be used directly. It is the base for tools like [Swagger](http://swagger.io) or [RAML](http://raml.org).

# definition

Let's review how to build rich APIs using the ```spec``` module using either the ```script``` or the ```mvc``` programming styles:

## script API

```java
{
  /**
   * Everything about your Pets.
   */
  use("/api/pets")
      /**
       * List pets ordered by id.
       *
       * @param start Start offset, useful for paging. Default is <code>0</code>.
       * @param max Max page size, useful for paging. Default is <code>50</code>.
       * @return Pets ordered by name.
       */
      .get(req -> {
        int start = req.param("start").intValue(0);
        int max = req.param("max").intValue(50);
        DB db = require(DB.class);
        List<Pet> pets = db.findAll(start, max);
        return pets;
      })
      /**
       * Find pet by ID
       *
       * @param id Pet ID.
       * @return Returns <code>200</code> with a single pet or <code>404</code>
       */
      .get("/:id", req -> {
        int id = req.param("id").intValue();
        DB db = require(DB.class);
        Pet pet = db.find(id);
        if (pet == null) {
          throw new Err(Status.NOT_FOUND);
        }
        return pet;
      })
      /**
       * Add a new pet to the store.
       *
       * @param body Pet object that needs to be added to the store.
       * @return Returns a saved pet.
       */
      .post(req -> {
        Pet pet = req.body().to(Pet.class);
        DB db = require(DB.class);
        db.save(pet);
        return pet;
      })
      /**
       * Update an existing pet.
       *
       * @param body Pet object that needs to be updated.
       * @return Returns a saved pet.
       */
      .put(req -> {
        Pet pet = req.body().to(Pet.class);
        DB db = require(DB.class);
        db.save(pet);
        return pet;
      })
      /**
       * Deletes a pet by ID.
       *
       * @param id Pet ID.
       * @return A <code>204</code>
       */
      .delete("/:id", req -> {
        int id = req.param("id").intValue();
        DB db = require(DB.class);
        db.delete(id);
        return Results.noContent();
      })
      .produces("json")
      .consumes("json");
}
```

## mvc API

```java

/**
 * Everything about your Pets.
 */
@Path("/api/pets")
@Consumes("json")
@Produces("json")
public class Pets {

  private DB db;

  @Inject
  public Pets(final DB db) {
    this.db = db;
  }

  /**
   * List pets ordered by name.
   *
   * @param start Start offset, useful for paging. Default is <code>0</code>.
   * @param max Max page size, useful for paging. Default is <code>200</code>.
   * @return Pets ordered by name.
   */
  @GET
  public List<Pet> list(final Optional<Integer> start, final Optional<Integer> max) {
    List<Pet> pets = db.findAll(Pet.class, start.orElse(0), max.orElse(200));
    return pets;
  }

  /**
   * Find pet by ID.
   *
   * @param id Pet ID.
   * @return Returns a single pet
   */
  @Path("/:id")
  @GET
  public Pet get(final int id) {
    Pet pet = db.find(Pet.class, id);
    return pet;
  }

  /**
   * Add a new pet to the store.
   *
   * @param pet Pet object that needs to be added to the store.
   * @return Returns a saved pet.
   */
  @POST
  public Pet post(@Body final Pet pet) {
    db.save(pet);
    return pet;
  }

  /**
   * Update an existing pet.
   *
   * @param body Pet object that needs to be updated.
   * @return Returns a saved pet.
   */
  @PUT
  public Pet put(@Body final Pet pet) {
    db.save(pet);
    return pet;
  }

  /**
   * Deletes a pet by ID.
   *
   * @param id Pet ID.
   */
  @DELETE
  public void delete(final int id) {
    db.delete(Pet.class, id);
  }
}
```

The previous examples, while feature identical, are written very differently. The API Spec for both is still the same:

```yaml

GET /api/pets
  summary: Everything about your Pets.
  doc: List pets ordered by name.
  consumes: [application/json]
  produces: [application/json]
  params: 
    start: 
      paramType: QUERY
      type: int
      value: 0
      doc: Start offset, useful for paging. Default is <code>0</code>.
    max: 
      paramType: QUERY
      type: int
      value: 200
      doc: Max page size, useful for paging. Default is <code>200</code>.
  response: 
    type: java.util.List<apps.model.Pet>
    doc: Pets ordered by name.
GET /api/pets/:id
  summary: Everything about your Pets.
  doc: Find pet by ID
  consumes: [application/json]
  produces: [application/json]
  params: 
    id: 
      paramType: PATH
      type: int
      doc: Pet ID.
  response: 
    type: apps.model.Pet
    doc: Returns <code>200</code> with a single pet or <code>404</code>
POST /api/pets
  summary: Everything about your Pets.
  doc: Add a new pet to the store.
  consumes: [application/json]
  produces: [application/json]
  params: 
    <body>: 
      paramType: BODY
      type: apps.model.Pet
      doc: Pet object that needs to be added to the store.
  response: 
    type: apps.model.Pet
    doc: Returns a saved pet.
PUT /api/pets
  summary: Everything about your Pets.
  doc: Update an existing pet.
  consumes: [application/json]
  produces: [application/json]
  params: 
    <body>: 
      paramType: BODY
      type: apps.model.Pet
      doc: Pet object that needs to be updated.
  response: 
    type: apps.model.Pet
    doc: Returns a saved pet.
DELETE /api/pets/:id
  summary: Everything about your Pets.
  doc: Deletes a pet by ID.
  consumes: [application/json]
  produces: [application/json]
  params: 
    id: 
      paramType: PATH
      type: int
      doc: Pet ID.
  response: 
    type: void
    doc: A <code>204</code>
```

> NOTE: We use a ```textual``` representation here for simplicity and readability, but keep in mind that the output is compiled into a binary format.

## script rules

Consider the previous examples again. Do you notice anything special? No, right?

There are however some minor issues that you need to keep in mind for getting or collecting route metadata from **script** routes:

### params

Parameters need to be in one sentence/statement, like:

```java
req -> {
  int id = req.param("id").intValue();
}
```

not like: 

```java
req -> {
  Mutant p = req.param("id");
  int id = p.intValue();
}
```

### response type (a.k.a return type)

There should be **ONLY one** return statement and the return type needs to be declared as a variable, like this:

```java
req -> {
  ...
  Pet pet = db.find(id); // variable pet
  ...
  return pet;  // single return statement
}
```

not like this: 

```java
req -> {
  ...
  return db.find(id); // we aren't able to tell what type returns db.find
}
```

or

```java
req -> {
  ...
  // multiple returns statement
  if (...) {
    return ...; // here is one
  } else {
    return ...; // here is another
  }
}
```

If these rules don't make sense to you or if the algorithm fails to resolve the correct type, there is a workaround outlined in the next section.

## writing documentation

By taking a few minutes to write quality documentation for your code, you will reap huge benefits!

The spec tool will export the doc as part of your API!

Here is an example on how to document script routes:

```java
  /**
   * Everything about your Pets.
   */
  use("/api/pets")
     /**
      * List pets ordered by name.
      *
      * @param start Start offset, useful for paging. Default is <code>0</code>.
      * @param max Max page size, useful for paging. Default is <code>200</code>.
      * @return Pets ordered by name.
      */
    .get(req -> {
      int start = req.param("start").intValue(0);
      int max = req.param("max").intValue(200);
      DB db = require(DB.class);
      List<Pet> pets = db.findAll(Pet.class, start, max);
      return pets;
    });
```

The route params for ```/api/pets``` looks like:

```yaml
  params: 
    start: 
      paramType: QUERY
      type: int
      value: 0
      doc: Start offset, useful for paging. Default is <code>0</code>.
    max: 
      paramType: QUERY
      type: int
      value: 200
      doc: Max page size, useful for paging. Default is <code>200</code>.
  response: 
    type: java.util.List<apps.model.Pet>
    doc: Pets ordered by name.
```

### response

With JavaDoc, you can control the default type returned by the route and/or the status codes. For example:

```java

  /**
   * Find pet by ID.
   *
   * @param id Pet ID.
   * @return Returns a {@link Pet} with <code>200</code> status or <code>404</code> 
   */
  get(req -> {
    DB db = require(DB.class);
    return db.find(Pet.class, id);
  });
```

Produces:

```yaml
response:
    type: apps.model.Pet
    statusCodes: 
      200: Success
      404: Not Found
    doc: Returns <code>200</code> with a single pet or <code>404</code>
```

In this example you tell the spec tool that the response is a ```Pet``` via *JavaDoc* type references: ```{@link Type}```.

This is useful when the tool isn't able to detect the type for you and/or you aren't able to follow the return rules described earlier.

The status codes section has entries for ```200``` and ```404``` with default messages. You can override the default message by using ```code = message``` like:

```html
@return Returns a {@link Pet} with <code>200 = Success</code> status or <code>404 = Missing</code>
```

## how does it work?

The spec module scans and parses the source code files: ```*.java``` and produces a list of ```RouteSpec```.

This is required for getting information from ```script routes``` and to extract ```JavaDoc```.

However, this is not needed for ```mvc routes``` because all the information is available through *Reflection* and the ```java.lang.Method```.

Since the spec tool needs the source code in order to work properly you must set up the ```jooby:spec``` maven plugin in order to use the spec tool at deploy time:

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>spec</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

or the [gradle](/doc/gradle-plugin/#gradle-plugin-joobySpec) plugin:

```js
buildscript {

  dependencies {
    /** joobyRun */
    classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '1.3.0'
  }
}

apply plugin: 'jooby'

```

```bash
gradle joobySpec
```

The maven and gradle plugins export the API to a binary format: ```.spec``` which can be parsed at a later time.

That's all about the spec, as you've seen there are some minor rules to follow while writing ```script routes```. Now it's time see what tools and integrations are available!
