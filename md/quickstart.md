quickstart
=====

Just paste this into a terminal (make sure Java 8 and Maven 3.x are installed):

    mvn archetype:generate -B -DgroupId=com.mycompany -DartifactId=my-app \
    -Dversion=1.0-SNAPSHOT -DarchetypeArtifactId=jooby-archetype \
    -DarchetypeGroupId=org.jooby -DarchetypeVersion={{version}}

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

    INFO  [2015-03-19 21:34:00,365] Hotswap available on: [/Users/edgar/Source/dmox/public, /Users/edgar/Source/dmox/config, /Users/edgar/Source/dmox/target/classes]
    INFO  [2015-03-19 21:34:00,368]   unlimited runtime class redefinition: yes
    INFO  [2015-03-19 21:34:00,368]   includes: [**/*.class,**/*.conf,**/*.properties]
    INFO  [2015-03-19 21:34:00,369]   excludes: []
    INFO  [2015-03-19 21:34:00,937] [dev@netty]: App server started in 502ms

    GET /assets/**    [*/*]     [*/*]    (anonymous)
    GET /             [*/*]     [*/*]    (anonymous)

    listening on:
      http://0.0.0.0:8080/


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

**NOTE**: The directories: ```public``` and ```confi``` are part of the classpath.

App.java
-----

```java

import org.jooby.Jooby;

public class App extends Jooby { // 1

  {
    // 2
    assets("/assets/**");

    assets("/", "/welcome.html");
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
Then all you have to do is run the: ```App.java``` class. After all, this is plain Java application with a ```main``` method.
