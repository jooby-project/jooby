[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-filewatcher/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-filewatcher)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-filewatcher.svg)](https://javadoc.io/doc/org.jooby/jooby-filewatcher/1.2.3)
[![jooby-filewatcher website](https://img.shields.io/badge/jooby-filewatcher-brightgreen.svg)](http://jooby.org/doc/filewatcher)
# file watcher

Watches for file system changes or event. It uses a watch service to monitor a directory for changes so that it can update its display of the list of files when files are created or deleted.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-file watcher</artifactId>
 <version>1.2.3</version>
</dependency>
```

## usage

Watch ```mydir``` and listen for create, modify or delete event on all files under it:

```java
{
  use(new FileWatcher()
    .register(Paths.get("mydir"), (kind, path) -> {
      log.info("found {} on {}", kind, path);
    });
  );
}
```

## file handler

You can specify a {@link FileEventHandler} instance or class while registering a path:

Instance:

```java
{
  use(new FileWatcher()
    .register(Paths.get("mydir"), (kind, path) -> {
      log.info("found {} on {}", kind, path);
    });
  );
}
```

Class reference:

```java
{
  use(new FileWatcher()
    .register(Paths.get("mydir"), MyFileEventHandler.class)
  );
}
```

Worth to mention that ```MyFileEventHandler``` will be provided by Guice.

## options

You can specify a couple of options at registration time:

```java
{
  use(new FileWatcher(
    .register(Paths.get("mydir"), MyFileEventHandler.class, options -> {
      options.kind(StandardWatchEventKinds.ENTRY_MODIFY)
             .recursive(false)
             .includes("*.java");
    });
  ));
}
```

1. Here we listen for {@link StandardWatchEventKinds#ENTRY_MODIFY} (we don't care about create or delete).

2. We turn off recursive watching, only direct files are detected.

3. We want ```.java``` files and we ignore any other files.

## configuration file

In addition to do it programmatically, you can do it via configuration properties:

```java
{
  use(new FileWatcher()
    .register("mydirproperty", MyEventHandler.class)
  );
}
```

The ```mydirproperty``` property must be present in your ```.conf``` file.

But of course, you can entirely register paths from ```.conf``` file:

```
filewatcher {
  register {
    path: "mydir"
    handler: "org.example.MyFileEventHandler"
    kind: "ENTRY_MODIFY"
    includes: "*.java"
    recursive: false
  }
}
```

Multiple paths are supported using array notation:

```java
filewatcher {
  register: [{
    path: "mydir1"
    handler: "org.example.MyFileEventHandler"
  }, {
    path: "mydir2"
    handler: "org.example.MyFileEventHandler"
  }]
}
```

Now use the module:

```java
{
  use(new FileWatcher());
}
```

The [FileWatcher](/apidocs/org/jooby/filewatcher/FileWatcher.html) module read the ```filewatcher.register``` property and setup everything.
