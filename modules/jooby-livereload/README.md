[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-livereload/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-livereload/1.6.2)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-livereload.svg)](https://javadoc.io/doc/org.jooby/jooby-livereload/1.6.2)
[![jooby-livereload website](https://img.shields.io/badge/jooby-livereload-brightgreen.svg)](http://jooby.org/doc/livereload)
# liveReload

<a href="http://livereload.com">LiveReload</a> monitors changes in the file system. As soon as you save a file, it is preprocessed as needed, and the browser is refreshed.

Even cooler, when you change a CSS file or an image, the browser is updated instantly without reloading the page.

## exports

* `livereload.js` route
* `livereload` websocket

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-livereload</artifactId>
 <version>1.6.2</version>
</dependency>
```

## usage

```java
{
  use(new Jackson());

  use(new LiveReload());
}
```

This module is available as long you run in development: `application.env=dev`.

## configuration

### browser extension

Install the <a href="http://livereload.com/extensions/">LiveReload browser extension.</a>

### livereload.js

Add the ```livereload.js``` to your web page.

You can do this manually:

```
<script src="/livereload.js"></script>
```

Or use the ```liveReload``` local variable with your preferred template engine. Here is an example using ```Handlebars```:

```java
{{liveReload}}
```

### json module

The <a href="http://feedback.livereload.com/knowledgebase/articles/86174-livereload-protocol">LiveReload Protocol</a> run on top of a ```WebSocket``` and uses ```JSON``` as protocol format. That's is why we also need to install a ```JSON module```, like <a href="http://jooby.org/doc/jackson">Jackson</a>.

## watcher

It automatically reload static resources from ```public```, ```target``` (Maven projects) or ```build``` folders (Gradle projects).

Every time a change is detected the websocket send a ```reload command```.

## starter project

We do provide a [livereload-starter](https://github.com/jooby-project/livereload-starter) project. Go and [fork it](https://github.com/jooby-project/livereload-starter).

That's all folks!!
