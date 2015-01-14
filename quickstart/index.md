---
layout: index
title: quickstart
version: 0.4.0
---

quickstart
=====

Just paste this into a terminal (make sure Java 8 and Maven 3.x are installed):

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app 
    -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype 
    -DarchetypeGroupId=org.jooby -DarchetypeVersion=0.4.0

You might want to edit/change:

* -DgroupId: A Java package's name

* -DartifactId: A project's name in lower case and without spaces

* -Dversion: A project's version, like ```1.0-SNAPSHOT``` or ```1.0.0-SNAPSHOT```


Let's try it!:

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app 
    -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype 
    -DarchetypeGroupId=org.jooby -DarchetypeVersion=0.4.0
    cd my-app
    mvn jooby:run

You should see something similar to this at the end of the output:

    INFO  [2015-01-12 17:22:52,193] XNIO version 3.3.0.Final
    INFO  [2015-01-12 17:22:52,237] XNIO NIO Implementation Version 3.3.0.Final
    INFO  [2015-01-12 17:22:52,525] Server started in 650ms
    GET /favicon.ico    [*/*]     [*/*]    (anonymous)
    GET /assets/**/*    [*/*]     [*/*]    (anonymous)
    GET /               [*/*]     [*/*]    (anonymous)

    listening on:
      http://localhost:8080

Jooby! is up and running!!!

getting started
=====

exploring the newly created project
-----

A new directory was created: ```my-app```. Now, let's see how it looks like:

    /public
           /assets/js/index.js
           /assets/css/style.css
           /images
          welcome.html
    /config
           application.conf
           logback.xml
    /src/main/java
                  /com/mycompany/App.java

The **public** directory contains ```*.html```, ```*.js```, ```*.css```, ```*.png```, ... etc., files.

The **config** directory contains ```*.conf```, ```*.properties```, ```*.json```, ... etc., files.

The **src/main/java** contains ```*.java``` (of course) files.

**NOTE**: The three directory are part of the classpath.

**NOTE**: So this is Maven, Why don't use the default directory layout?

Good question, in a Java project not all the team members are backend developers. The **public** folder
was specially created for frontend developer or web designers with no experience in Java. 

This is a matter of taste and if you find it problematic, you can use the default directory layout of Maven.


App.java
-----

```java

import org.jooby.Jooby;

public class App extends Jooby { // 1

  {
    // 2 routes
    get("/favicon.ico");

    assets("/assets/**");

    get("/", file("welcome.html"));
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args); // 3. start the application.
  }

}

```

Steps involved are:

1) extends Jooby

2) define some routes

3) call the ```start``` method

running
-----

Just open a console and type:

    mvn jooby:run

The maven plugin will compile the code (if necessary) and startup the application.

Of course, you can generate the IDE metadata from Maven and/or import as a Maven project on your favorite IDE.
Then all you have to do is run the: ```App.java``` class. After all, this is plain Java with a ```main``` method.

