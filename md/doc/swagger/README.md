# jooby-swagger

{{swagger}} is a simple yet powerful representation of your RESTful API.

This module generate {{swagger}} spec file: ```.json``` or ```.yml``` but also UI for MVC routes.

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
  SwaggerUI.install(this);

  // Swagger will generate a swagger spec for the Pets MVC routes.
  use(Pets.class);
}
```

By default, {{swagger}} will be mounted at ```/swagger```, ```/swagger/swagger.json``` and ```/swagger/swagger.yml```. Go and try it!

Or if you want to mount Swagger somewhere else...:

```java
{
  SwaggerUI.install("/api/docs", this);
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

{{jooby}} creates a {{swagger}} model dynamically from MVC routes. But also, defines some defaults inside the ```swagger.conf``` (see appendix).

For example, ```swagger.info.title``` is set to ```application.name```. If you want
to provide a more friendly name, description or API version... you can do it via your ```application.conf``` file:

```properties

swagger.info.title = My Awesome API
swagger.info.version = v0.1.0

```

## limitations (future implementation)

* Sadly, ONLY MVC routes are supported. Inline/lambda routes has no supports for now.
* It might be nice to generate API docs (via markdown or similar) at built time using {{maven}}.

{{appendix}}
