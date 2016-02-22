The spec module allows you to export your API/microservices outside a {{jooby}} application.

The goal of this module is to define a common way to write APIs and provide you tools like live doc and testing for **FREE**. By **FREE** we mean:

> You aren't force to learn any other tool or annotated your code special annotations. All you have to do is **write your application** following a few/minor suggestions.

This module process, collect and compile **routes** from your application. It extracts HTTP method/pattern, parameter, responses, types and doc.

You will find here the basis and the necessary documentation to build and expose rich APIs for free, but keep in mind this module isn't intended for direct usage. It is the basis for tools like {{swagger}} or {{raml}}.

# definition

Let's review how to build rich APIs using the ```spec``` module via ```script``` or ```mvc``` way:

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
        DB db = req.require(DB.class);
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
        DB db = req.require(DB.class);
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
        DB db = req.require(DB.class);
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
        DB db = req.require(DB.class);
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
        DB db = req.require(DB.class);
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

Previous examples are feature identical, but they were written in very different way. Still API Spec for them look likes:

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

> NOTE: We use ```textual``` representation  here for simplicity and easy read, but keep in mind the output is compiled into a binary format.

## script rules

Have a look at the previous examples again? Do you see anything special? No, right?

Well there are some minor things you need to keep in mind for getting or collecting route metadata from **script** routes:

### params

Params need to be in one sentence/statement, like:

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

There should be **ONLY one** return statement and return type needs to be declared as variable, like:

```java
req -> {
  ...
  Pet pet = db.find(id); // variable pet
  ...
  return pet;  // single return statement
}
```

not like: 

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

There is a workaround if these rules doesn't make sense to you and/or the algorithm fails to resolve the correct type. Please checkout next section.

## writing doc

If you take a few minutes and write good quality doc the prize will be huge!

The tool takes and export the doc as part of your API!!

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
      DB db = req.require(DB.class);
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
    DB db = req.require(DB.class);
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

Here you tell the tool response is a ```Pet``` via *JavaDoc* type references: ```{@link Type}```.

This is useful when the tool isn't able to detect the type for you and/or you aren't able to follow the return rules described before.

The status codes section have a ```200``` and ```404``` entries with default messages. You can override the default message by using ```code = message``` like:

```
@return Returns a {@link Pet} with <code>200 = Success</code> status or <code>404 = Missing</code>
```

## how it works?

The spec module scan and parse the source code: ```*.java``` and produces a list of ```RouteSpec```.

It is required for getting information from ```script routes``` and extract ```JavaDoc```.

We don't need that for ```mvc routes``` because all the information is available via *Reflection* and ```java.lang.Method```.

So, the tool needs the source code in order to work properly. In order to use the tool at deploy time you have to setup the ```jooby:spec``` maven plugin:

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

The maven plugin exports the API into a binary format: ```.spec``` which can be parse it later.

That's all about the spec, as you see there are some minor rules to follow while writing ```script routes```. Now it's time see what tools and integrations are available!!!
