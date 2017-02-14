## exports

* ```/swagger``` rout with SwaggerUI
* ```/swagger/swagger.json``` route
* ```/swagger/swagger.yml``` route

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-swagger</artifactId>
  <version>1.0.3</version>
</dependency>
```

## usage

```java
{
  // define your API... via script or MVC:
  /**
   * Everything about your pets
   */
   use("/api/pets")
     /**
      * Get a pet by ID.
      * @param id Pet ID
      */
     .get("/:id", req -> {
       int id = req.param("id").intValue();
       DB db = require(DB.class);
       Pet pet = db.find(Pet.class, id);
       return pet;
     })
     ...;

  new SwaggerUI().install(this);

}
```

Now the ```jooby:spec``` maven plugin:

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

Start your app and try:

* The Swagger-UI at ```/swagger```
* The ```swagger.json``` at ```/swagger/swagger.json```
* The ```swagger.yml``` at ```/swagger/swagger.yml```

## options

There are a few options available, let's see what they are:

### path

The ```path``` option controls where to mount the [Swagger](http://swagger.io) routes:

```java
{
  ...

  new SwaggerUI("docs").install(this);

}
```

Produces: ```/docs``` for swagger-ui, ```/docs/swagger.json``` and ```/docs/swagger.yml```. Default path is: ```/swagger```.

### filter

The ```filter``` option controls what is exported to [Swagger](http://swagger.io):

```java
{
  ...

  new SwaggerUI()
    .filter(route -> {
      return route.pattern().startsWith("/api");
    })
    .install(this);
}
```

Default filter keeps ```/api/*``` routes.

### tags

One ore more routes are grouped by a tag. The default tag provider produces ```pets``` for a route at ```/api/pets``` or ```/pets```. You can specify your own tag provider via:

```java
{
  new SwaggerUI()
    .tag(route -> route.name())
    .install(this);
}
```

### noUI

This option turn off the swagger-ui:

```java
{
  ...

  new SwaggerUI()
    .noUI()
    .install(this);
}
```

## live demo

Check out the a live demo for [Swagger](https://jooby-spec.herokuapp.com/swagger).

Source code available at [github](https://github.com/jooby-guides/route-spec)

## swagger.conf

```properties
swagger {

  swagger: "2.0"

  info {

    title: ${application.name} API

    version: ${application.version}

  }

  basePath: ${application.path}

  consumes: ["application/json"]

  produces: ["application/json"]

  schemes: ["http"]

}
```
