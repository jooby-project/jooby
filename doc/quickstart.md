# welcome

Welcome to the getting started guide, you will find here a step by step guide using a **Maven archetype**. Optionally, you might want to try the one of the starter projects:

 - [Starter projects](https://github.com/search?q=topic%3Astarter+org%3Ajooby-project&type=Repositories)
 - [Gradle starter](https://github.com/jooby-project/gradle-starter)
 - [Kotlin starter](https://github.com/jooby-project/kotlin-starter)
 - [Kotlin gradle starter](https://github.com/jooby-project/kotlin-gradle-starter)


requirements
=====

* Install {{java}}
* Install {{maven}}

quickstart
=====

Just paste this into a terminal (make sure you are in an empty folder, and [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Maven 3.x](http://maven.apache.org/download.cgi) are installed):

```bash
mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
```

You might want to edit/change:

* -DgroupId: A Java package's name

* -DartifactId: A project's name in lower case and without spaces

* -Dversion: A project's version, like ```1.0-SNAPSHOT``` or ```1.0.0-SNAPSHOT```


Let's try it!:

```bash
mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
cd my-app
mvn jooby:run
```

You should see something similar to this at the end of the output:

```bash
INFO  [2015-03-19 21:34:00,365] Hotswap available on: [my-app/public, my-app/conf, my-app/target/classes]
INFO  [2015-03-19 21:34:00,368]   includes: [**/*.class,**/*.conf,**/*.properties]
INFO  [2015-03-19 21:34:00,369]   excludes: []
INFO  [2015-03-19 21:34:00,937] [dev@netty]: App server started in 502ms

GET /             [*/*]     [*/*]    (anonymous)

listening on:
  http://0.0.0.0:8080/
```

**Jooby**! is up and running!

getting started
=====

exploring the newly created project
-----

A new directory was created: ```my-app```. Let's see what it looks like:

```bash
.
├── public
|   └── (empty)
├── conf
|   ├── application.conf
|   └── logback.xml
└── src
    ├── main
    |   └── java
    |       └── com
    |           └── mycompany
    |               └── App.java
    └── test
        └── java
            └── com
                └── mycompany
                    └── AppTest.java
```

The **public** folder contains static content like ```*.html```, ```*.js```, ```*.css```, ..., ```*.png``` files.

The **conf** folder contains ```*.conf```.

The **src/main/java** folder contains ```*.java``` files (of course).

The **src/test/java** folder contains unit and integration tests.

> **NOTE**: The ```public``` and ```conf``` folders are part of the classpath.

### App.java


```java

import org.jooby.Jooby;

public class App extends Jooby { // 1

  {
    // 2
    get("/", () -> "Hello World!");
  }

  public static void main(final String[] args) {
    run(App::new, args); // 3. start the application.
  }

}

```

Steps involved are:

1) extend Jooby

2) define some routes

3) call the ```run``` method

running
-----

Open a console and type:

    mvn jooby:run

The maven plugin will compile the code (if necessary) and start the application.

Of course, you can generate the IDE metadata from Maven or import as a Maven project in your favorite IDE.
Afterwards, all you have to do is run the: ```App.java``` class. After all, this is a plain Java application with a ```main``` method.

where to go now?
-----

* read the [documentation](/doc)
* check out one of the {{templates}}
