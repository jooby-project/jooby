---
layout: home
title: home
version: 0.1.0
---


```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", (req, rsp) ->
      rsp.send("Hey Jooby!")
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

quickstart
=====

Just paste this into a terminal (make sure Java 8 and Maven 3.x are installed):

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app \
    -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype \
    -DarchetypeGroupId=org.jooby -DarchetypeVersion=0.1.0

You might want to edit/change:

* -DgroupId: A Java package's name

* -DartifactId: A project's name in lower case and without spaces

* -Dversion: A project's version, like ```1.0-SNAPSHOT``` or ```1.0.0-SNAPSHOT```


Let's try it!:

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app \
    -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype \
    -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}
    cd my-app
    mvn jooby:run

You should see something similar to this at the end of the output:

    INFO  [2014-11-04 09:20:12,526] Logging initialized @645ms
    INFO  [2014-11-04 09:20:12,574] jetty-9.2.3.v20140905
    INFO  [2014-11-04 09:20:12,599] Started o.e.j.s.h.ContextHandler@26b3fd41{/,null,AVAILABLE}
    INFO  [2014-11-04 09:20:12,612] Started ServerConnector@53e8321d{HTTP/1.1}{0.0.0.0:8080}
    INFO  [2014-11-04 09:20:12,736] Started ServerConnector@74ea2410{SSL-HTTP/1.1}{0.0.0.0:8443}
    INFO  [2014-11-04 09:20:12,736] Started @859ms
    INFO  [2014-11-04 09:20:12,736] 
    Routes:
      GET /favicon.ico    [*/*]     [*/*]    (anonymous)
      GET /assets/**/*    [*/*]     [*/*]    (static files)
      GET /               [*/*]     [*/*]    (anonymous)

Open a browser and type:

    http://localhost:8080/

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
           logback.dev.xml
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

