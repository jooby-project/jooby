# jooby-swagger

[Swagger](http://swagger.io) is a simple yet powerful representation of your RESTful API.

This module generate [Swagger](http://swagger.io) spec file: ```.json``` or ```.yml``` but also UI for MVC routes.

## exposes

* A ```/swagger``` route that renders a Swagger UI
* A ```/swagger.json``` route that renders a [Swagger](http://swagger.io) spec in ```json``` format
* A ```/swagger.yml``` route that renders a [Swagger](http://swagger.io) spec in ```yaml``` format

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-swagger</artifactId>
  <version>0.7.0</version>
</dependency>
```

## usage

```java
@Path("/api/pets")
public class Pets {

  @Path("/:id")
  @GET
  public Pet get(String id) {...}

  @POST
  public Pet post(Pet pet) {...}
}
```


```java
{
  use(new SwaggerUI());

  // Swagger will generate a swagger spec for the Pets MVC routes.
  use(Pets.class);
}
```

By default, [Swagger](http://swagger.io) will be mounted at ```/swagger```, ```/swagger/swagger.json``` and ```/swagger/swagger.yml```. Go and try it!

Or if you want to mount Swagger somewhere else...:

```java
{
  use(new SwaggerUI("/api/docs"));
}
```

It is also possible to use Swagger (ui, .json or .yml) on specific resources.

For example, suppose we have a ```Pets.java``` resource mounted at ```/pets```. The following
URL will be available too:

```bash
   /swagger/pets (UI)
   /swagger/pets/swagger.json (JSON)
   /swagger/pets/swagger.yml (YML)
```

It is a small feature, but very useful if you have a medium-size API.

## swagger.conf

[Jooby](http://jooby.org) creates a [Swagger](http://swagger.io) model dynamically from MVC routes. But also, defines some defaults inside the ```swagger.conf``` (see appendix).

For example, ```swagger.info.title``` is set to ```application.name```. If you want
to provide a more friendly name, description or API version... you can do it via your ```application.conf``` file:

```properties

swagger.info.title = My Awesome API
swagger.info.version = v0.1.0

```

## limitations (future implementation)

* Sadly, ONLY MVC routes are supported. Inline/lambda routes has no supports for now.
* It might be nice to generate API docs (via markdown or similar) at built time using [Maven](http://maven.apache.org/).

# appendix: swagger.conf

```properties
swagger {
  swagger: "2.0"
  info {
    title: ${application.name}
  }
  basePath: ${application.path}
  consumes: ["application/json"]
  produces: ["application/json"]
  schemes: ["http"]
}

```
