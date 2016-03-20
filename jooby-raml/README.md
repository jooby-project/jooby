# raml

RESTful API Modeling Language (RAML) makes it easy to manage the whole API lifecycle from design to sharing. It's concise - you only write what you need to define - and reusable. It is machine readable API design that is actually human friendly. More at <a href="http://raml.org/">http://raml.org</a>


> NOTE: This module depends on [route spec](/doc/spec/spec.html), please read the [route spec](/doc/spec/spec.html) documentation to learn how to write powerful APIs. 


## exposes

* The [api-console](https://github.com/mulesoft/api-console) at ```/raml```
* The ```.raml``` file at ```/raml/api.raml```

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-raml</artifactId>
 <version>0.16.0</version>
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
       DB db = req.require(DB.class);
       Pet pet = db.find(Pet.class, id);
       return pet;
     })
     ...;

  new Raml().install(this);

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

* The [api-console](https://github.com/mulesoft/api-console) at ```/raml```
* The ```.raml``` file at ```/raml/api.raml```

## options

There are a few options available, let's see what they are:

### path

The ```path``` option controls where to mount the [RAML](http://raml.org) routes:

```java
{
  ...

  new Raml("docs").install(this);

}
```

Produces: ```/docs``` for api-console and ```/docs/api.raml```. Default path is: ```/raml```.

### filter

The ```filter``` option controls what is exported to [RAML](http://raml.org):

```java
{
  ...

  new Raml()
    .filter(route -> {
      return route.pattern().startsWith("/api");
    })
    .install(this);
}
```

Default filter keeps ```/api/*``` routes.

### noConsole

This option turn off the api-console:

```java
{
  ...

  new Raml()
    .noConsole()
    .install(this);
}
```

### theme

Set the ui-theme for api-console. Available options are ```light``` and ```dark```. Default is: ```light```.

```java
{
  ...

  new Raml()
    .theme("dark")
    .install(this);
}
```

### clientGenerator

Shows/hide the client generator button from api-console.

### tryIt

Expand/collapse the try it panel from api-console.

## live demo

Check out the a live demo for [RAML](https://jooby-spec.herokuapp.com/raml).

Source code available at [github](https://github.com/jooby-guides/route-spec)

## raml.conf

```properties
raml {

  title: ${application.name} API

  version: ${application.version}

  baseUri: "http://"${application.host}":"${application.port}${application.path}

  mediaType: "application/json"

  protocols: [HTTP]

}
```
