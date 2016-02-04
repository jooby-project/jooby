# spec

The spec module allows you to export your API/microservices outside a {{jooby}} application.

The goal of this module is to define a common way to write APIs and provide you API tools like live doc and testing for **FREE**. By **FREE** we mean:

> You aren't force to learn any other tool or annotated your code special annotations. All you have to do is: **write your application** following a few/minor suggestions.

This module process, collect and compile **routes** from your application. It extracts HTTP method/pattern, parameter, responses, types and doc.

You will find here the basis and the necessary documentation to build and expose rich APIs for free, but keep in mind this module isn't intended for direct usage. It is the basis for tools like {{swagger}} or {{raml}}.

# api def

The goal of this module is to define a common way to write APIs and provide you API tools like live doc and testing for **FREE**. By **FREE** we mean:

> You aren't force to learn any other tool or annotated your code special annotations. All you have to do is: **write your application** following a few/minor suggestions.

Cool, isn't?

Let's review how to build rich APIs using the ```spec``` module via ```script``` or ```mvc``` way:

## script API

```java
{
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
    })
    /**
      * Find pet by ID.
      *
      * @param id Pet ID.
      * @return Returns a single pet
      */
    .get("/:id", req -> {
      int id = req.param("id").intValue();
      DB db = req.require(DB.class);
      Pet pet = db.find(Pet.class, id);
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
      Pet pet = db.save(pet);
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
      Pet pet = db.save(pet);
      return pet;
    })
    /**
      * Deletes a pet by ID.
      *
      * @param id Pet ID.
      */
    .delete("/:id", req -> {
      int id = req.param("id").intValue();
      DB db = req.require(DB.class);
      Pet pet = db.delete(Pet.class, id);
      return Results.noContent();
    });
}
```

## mvc API

```java

/**
 * Everything about your Pets.
 */
@Path("/api/pets")
public class Pets {

    /**
      * List pets ordered by name.
      *
      * @param start Start offset, useful for paging. Default is <code>0</code>.
      * @param max Max page size, useful for paging. Default is <code>200</code>.
      * @return Pets ordered by name.
      */
    @GET
    public List<Pet> list(Optional<Integer> start, Optional<Integer> max) { 
      DB db = req.require(DB.class);
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
    public Pet get(int id) {
      DB db = req.require(DB.class);
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
    public Pet post(@Body Pet pet) {
      DB db = req.require(DB.class);
      Pet pet = db.save(pet);
      return pet;
    }

    /**
      * Update an existing pet.
      *
      * @param body Pet object that needs to be updated.
      * @return Returns a saved pet.
      */
    @PUT
    public Pet put(@Body Pet pet) {
      DB db = req.require(DB.class);
      Pet pet = db.save(pet);
      return pet;
    }

    /**
      * Deletes a pet by ID.
      *
      * @param id Pet ID.
      */
    public void delete(int id) {
      DB db = req.require(DB.class);
      Pet pet = db.delete(Pet.class, id);
    }
}
```

Previous examples are feature identical, but they were written in very different way. Still they produces the same output:

```json
[ {
  "summary" : "Everything about your Pets.",
  "method" : "GET",
  "response" : {
    "statusCodes" : { },
    "doc" : "Pets ordered by name.",
    "type" : "java.util.List<apps.model.Pet>"
  },
  "pattern" : "/api/pets",
  "produces" : [ "*/*" ],
  "doc" : "List pets ordered by name.",
  "params" : [ {
    "paramType" : "QUERY",
    "name" : "start",
    "doc" : "Start offset, useful for paging. Default is <code>0</code>.",
    "type" : "int",
    "value" : 0
  }, {
    "paramType" : "QUERY",
    "name" : "max",
    "doc" : "Max page size, useful for paging. Default is <code>200</code>.",
    "type" : "int",
    "value" : 200
  } ],
  "consumes" : [ "*/*" ]
}, {
  "summary" : "Everything about your Pets.",
  "method" : "GET",
  "response" : {
    "statusCodes" : { },
    "doc" : "Returns a single pet",
    "type" : "apps.model.Pet"
  },
  "pattern" : "/api/pets/:id",
  "produces" : [ "*/*" ],
  "doc" : "Find pet by ID.",
  "params" : [ {
    "paramType" : "PATH",
    "name" : "id",
    "doc" : "Pet ID.",
    "type" : "int",
    "value" : null
  } ],
  "consumes" : [ "*/*" ]
}, {
  "summary" : "Everything about your Pets.",
  "method" : "POST",
  "response" : {
    "statusCodes" : { },
    "doc" : "Returns a saved pet.",
    "type" : "apps.model.Pet"
  },
  "pattern" : "/api/pets",
  "produces" : [ "*/*" ],
  "doc" : "Add a new pet to the store.",
  "params" : [ {
    "paramType" : "BODY",
    "name" : "<body>",
    "doc" : "Pet object that needs to be added to the store.",
    "type" : "apps.model.Pet",
    "value" : null
  } ],
  "consumes" : [ "*/*" ]
}, {
  "summary" : "Everything about your Pets.",
  "method" : "PUT",
  "response" : {
    "statusCodes" : { },
    "doc" : "Returns a saved pet.",
    "type" : "apps.model.Pet"
  },
  "pattern" : "/api/pets",
  "produces" : [ "*/*" ],
  "doc" : "Update an existing pet.",
  "params" : [ {
    "paramType" : "BODY",
    "name" : "<body>",
    "doc" : "Pet object that needs to be updated.",
    "type" : "apps.model.Pet",
    "value" : null
  } ],
  "consumes" : [ "*/*" ]
}, {
  "summary" : "Everything about your Pets.",
  "method" : "DELETE",
  "response" : {
    "statusCodes" : { },
    "doc" : null,
    "type" : "java.lang.Object"
  },
  "pattern" : "/api/pets/:id",
  "produces" : [ "*/*" ],
  "doc" : "Deletes a pet by ID.",
  "params" : [ {
    "paramType" : "PATH",
    "name" : "id",
    "doc" : "Pet ID.",
    "type" : "int",
    "value" : null
  } ],
  "consumes" : [ "*/*" ]
} ]
```

> NOTE: We use ```json```  here for simplicity and easy read, but keep in mind the output is compiled in binary format.

## how it works?

The spec module scan and parse the source code: ```*.java``` and produces a list of ```RouteSpec```.

### Why do we parse the source code?

It is required for getting information from ```script routes```. We don't need that for ```mvc routes``` because all the information is available via *Reflection* and ```java.lang.Method```.

But also, you can write clean and useful JavaDoc in your source code that later are added to the API information.

### Why don't parse byte-code with ASM?

Good question, the main reason is that we lost generic type information and we aren't able to tell if the route response is for example a ```List<Pet>```.

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
  return pet;
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
  if (...) {
    return ...;
  } else {
    return ...;
  }
}
```

There is a workaround if these rules doesn't make sense to you and/or the algorithm fails to resolve the correct type. Please checkout next section.

## API doc

If you take a few minutes and write good quality doc the prize will be huge!

The tool takes the doc and export it as part of your API!!

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

The spec for ```/api/pets``` will have the following doc:

```json
{
  summary: "Everything about your Pets.",
  "doc": "List pets ordered by name",
  params: [
    {
      name: "start",
      doc: "Start offset, useful for paging. Default is <code>0</code>."
    },
    {
      name: "max",
      doc: "Max page size, useful for paging. Default is <code>200</code>."
    }
  ],
  response: {
    type: "List<Pet>",
    doc: "Pets ordered by name"
  }
}
```

#### response

From documentation, you can control the default type returned by the route and/or the status codes. For example:

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

Here you tell the tool that this route produces a ```Pet```, response looks like:

```json
response: {
  type: "Pet",
  statusCodes: {
    200: "OK",
    404: "Not Found"
  }
```

You can override the default message of the status code with:

```
@return Returns a {@link Pet} with <code>200 = Success</code> status or <code>404 = Missing</code>
```

Finally, you can specify the response type via JavaDoc type references: ```{@link Pet}```. This is useful when the tool isn't able to detect the type for you and/or you aren't able to follow the return rules described before.

# how to use it?

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-spec</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
RouteProcessor processor = new RouteProcessor();

List<RouteSpec> specs = processor.process(new App(), "path/to/source/code");
```

Usage is for documentation propose, you will never have to deal with this API or use it. Instead you should try one of the high level modules/tools like {{swagger}} or {{raml}}.
