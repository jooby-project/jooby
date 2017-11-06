[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-apitool/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-apitool)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-apitool.svg)](https://javadoc.io/doc/org.jooby/jooby-apitool/1.2.2)
[![jooby-apitool website](https://img.shields.io/badge/jooby-apitool-brightgreen.svg)](http://jooby.org/doc/apitool)
# API tool

Automatically export your HTTP API to open standards like <a href="https://swagger.io/">Swagger</a> and <a href="https://raml.org/">RAML</a>.

This module generates live documentation from your HTTP API.

## screenshots

### swagger

<img alt="Swagger ApiTool!" style="width: 800px" src="http://jooby.org/resources/images/apitool-swagger.png">

### raml

<img alt="RAML ApiTool!" style="width: 800px" src="http://jooby.org/resources/images/apitool-raml.png">

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-api tool</artifactId>
 <version>1.2.2</version>
</dependency>
```

## usage

```java
{
   use(new ApiTool()
     .swagger("/swagger")
     .raml("/raml")
   );
}
```

Those lines export your API to <a href="https://swagger.io/">Swagger</a> and <a href="https://raml.org/">RAML</a>.

## example

Suppose you have a ```Pet API``` like:

```java
{
  
  /**
   * Everything about your Pets. 
   */
  path("/api/pets", () -> {

    /** 
     * List pets ordered by name.
     *
     * @param start Start offset, useful for paging. Default is ```0```.
     * @param max Max page size, useful for paging. Default is ```200```.
     * @return Pets ordered by name.
     */
     get(req -> {
       int start = req.param("start").intValue(0);
       int max = req.param("max").intValue(200);
       DB db = req.require(DB.class);
       List<Pet> pets = db.findAll(Pet.class, start, max);
       return pets;
     });

    /**
     * Find pet by ID
     * @param id Pet ID.
     * @return Returns ```200``` with a single pet or ```404```
     */
     get("/:id",req -> {
       int id = req.param("id").intValue();
       DB db = req.require(DB.class);
       Pet pet = db.find(Pet.class, id);
       return pet;
     });

    /**
     * Add a new pet to the store.
     * @param body Pet object that needs to be added to the store.
     * @return Returns a saved pet.
     */
     post(req -> {
       Pet pet = req.body().to(Pet.class);
       DB db = req.require(DB.class);
       db.save(pet);
       return pet;
     });

    /**
     * Update an existing pet.
     * @param body Pet object that needs to be updated.
     * @return Returns a saved pet.
     */
     put(req -> {
       Pet pet = req.body().to(Pet.class);
       DB db = req.require(DB.class);
       db.save(pet);
       return pet;
    });

    /**
     * Deletes a pet by ID.
     * @param id Pet ID.
     * @return A ```204```
     */
     delete("/:id",req -> {
       int id = req.param("id").intValue();
       DB db = req.require(DB.class);
       db.delete(Pet.class, id);
       return Results.noContent();
     });
  });

  /**
   * Install API Doc and export your HTTP API:
   */ 
   use(new ApiTool()
     .swagger("/swagger")
     .raml("/raml")
   );
}
```

The [ApiTool](/apidocs/org/jooby/apitool/ApiTool.html) module automatically exports your application to <a href="https://swagger.io/">Swagger</a> and <a href="https://raml.org/">RAML</a>.

Works for ```MVC routes``` and <a href="http://jooby.org/doc/lang-kotlin">Kotlin</a>.

## keep documentation

The [ApiTool](/apidocs/org/jooby/apitool/ApiTool.html) module parses documentation from source code. It works well as long as the source code is present, but it won't work after you deploy your application.

To fix this we provide a <a href="https://maven.apache.org">Maven</a> and <a href="https://gradle.org">Gradle</a> tasks that process your API at build time and keep the documentation available for later usage.

### maven plugin

Go to the ```plugins``` section of your ```pom.xml``` and add these lines:

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>apitool</goal>
      </goals> 
    </execution> 
  </executions>
</plugin>
```

Now, compile your application the ```apitool``` plugin generates a ```.json``` file for your API.

### gradle task

Go to ```build.gradle``` and add these lines:

```gradke
buildscript {
    dependencies {
        classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '1.2.2'
    }
}
apply plugin: 'jooby'
```

Then run:

```
joobyApiTool
```

## options

### filter

The ```filter``` option controls what routes are exported.

```java
{
   use(new ApiTool()
       // Keep /api/* routes:
      .filter(route -> route.pattern().startWiths("/api/")
   );
 }
```

### disable try it

Disable the ```tryIt``` button in <a href="https://swagger.io/">Swagger</a> or <a href="https://raml.org/">RAML</a>.

```java
{
   use(new ApiTool()
      .disableTryIt()
   );
 }
```

### disable UI

Disable ```UI``` for <a href="https://swagger.io/">Swagger</a> or <a href="https://raml.org/">RAML</a>.

```java
{
   use(new ApiTool()
      .disableUI()
   );
 }
```

### theme

Set the default theme for <a href="https://swagger.io/">Swagger</a> or <a href="https://raml.org/">RAML</a>.

```java
{
   use(new ApiTool()
      .raml(
         new Options("/raml")
           .theme("dark")
      )
      .swagger(
         new Options("/swagger")
           .theme("muted")
      )
   );
 }
```

Themes can set at runtime too via ```theme``` query parameter:

```
/swagger?theme=material
/raml?theme=dark
```

Complete list of <a href="https://swagger.io/">Swagger</a> theme are available <a href="https://github.com/ostranme/swagger-ui-themes">here</a>.

Raml comes with only two themes: ```light``` and ```dark```.

## advanced usage

Sometimes the [ApiTool](/apidocs/org/jooby/apitool/ApiTool.html) module doesn't generate correct metadata like type, names, documentation, etc. When that happens you need to manually fix/provide metadata.

```java
{
    use(new ApiTool()
      .modify(r -> r.pattern().equals("/api/pet/{id}", route -> {
        // Fix java doc for id parameter
        route.param("id", param -> {
          param.description("Fixing doc for ID");
        });
        // Set response type
        route.response()
          .type(Pet.class);
      });
    );
  }

}
```

It is possible to customize Swagger/RAML objects:

```java
{
    use(new ApiTool()
      .swagger(swagger -> {
        // Modify swagger resources.
        ...
      })
      .raml(raml -> {
        // Modify raml resources.
        ...
      });
    );
  }

}
```

This option is required when you want to customize/complement Swagger/RAML objects.

# starter project

We do provide an [apitool-starter](https://github.com/jooby-project/apitool-starter) project.

That's all folks!!
