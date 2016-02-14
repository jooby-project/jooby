# swagger

{{swagger}} is a simple yet powerful representation of your RESTful API.

It generates {{swagger}} spec file: ```.json```, ```.yml``` or UI from your application.

This module extends [spec](/doc/spec) module, before going forward, make sure you read the doc of the [spec](/doc/spec) module first.

## exposes

* A ```/swagger``` route that renders a Swagger UI
* A ```/swagger.json``` route that renders a {{swagger}} spec in ```json``` format
* A ```/swagger.yml``` route that renders a {{swagger}} spec in ```yaml``` format

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-swagger</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

Add the ```jooby:spec``` maven plugin:

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

Then the module into your ```App```:

```java
{
  ...

  new SwaggerUI().install(this);
}
```

Finally, just write your API:

### via script API

There are some minor details you need to known to successfully write API and export them to {{swagger}}. Please refer to the [spec module](/doc/spec) for such details.

```java
{
  /**
   * Everything about your Pets.
   */
  use("/api/pets")
    /**
     * Find pet by ID.
     *
     * @param id Pet ID.
     * @return Returns a single pet
     */
    .get("/:id", req -> {
      int id = req.param("id").intValue();
      ...
      return pet;
    })
    /**
     * Add a new pet to the store.
     *
     * @param pet Pet object that needs to be added to the store.
     * @return Returns a saved pet.
     */
    .post(req -> {
      Pet pet = req.body().to(Pet.class);
      ...
      return pet;
    });

  new SwaggerUI().install(this);
}
```

## mvc API

```java
{
  use(Pets.class);

  new SwaggerUI().install(this);
}
```

```java
/**
 * Everything about your Pets.
 */
@Path("/api/pets")
public class Pets {

  /**
   * Find pet by ID.
   *
   * @param id Pet ID.
   * @return Returns a single pet
   */
  @Path("/:id")
  @GET
  public Pet get(String id) {...}

  /**
   * Add a new pet to the store.
   *
   * @param pet Pet object that needs to be added to the store.
   * @return Returns a saved pet.
   */
  @POST
  public Pet post(Pet pet) {...}
}
```

## options

### swagger path

By default, {{swagger}} will be mounted at ```/swagger```, ```/swagger/swagger.json``` and ```/swagger/swagger.yml```.

If you want to mount Swagger somewhere else:

```java
{
  new SwaggerUI("/api/doc").install(this);
}
```

It is also possible to use Swagger (ui, .json or .yml) on specific resources.

For example, suppose you have a API for pets at ```/api/pets```. The following URL will be available too:

```bash
   /swagger/pets (UI)
   /swagger/pets/swagger.json (JSON)
   /swagger/pets/swagger.yml (YML)
```

It is a small feature, but very useful if you have a medium-size API.

### swagger filter

By convention, only routes mounted at ```/api/*``` will be exported to swagger and NOT all the available routes. You can control what to publish with a filter:

```java
{
  new Swagger()
    .filter(route -> route.pattern().startsWith("my filter"))
    .install(this);
}
```

### swagger tags

One ore more routes are grouped by a tag. The default tag provider produces ```pets``` for a route at ```/api/pets``` or ```/pets```. You can specify your own tag provider via:

```java
{
  new Swagger()
    .tag(route -> route.name())
    .install(this);
}
```

### swagger.conf

{{jooby}} creates a {{swagger}} model dynamically from MVC routes. But also, defines some defaults inside the ```swagger.conf``` (see appendix).

For example, ```swagger.info.title``` is set to ```application.name```. If you want
to provide a more friendly name, description or API version... you can do it via your ```application.conf``` file:

```properties

swagger.info.title = My Awesome API
swagger.info.version = v0.1.0

```

{{appendix}}
