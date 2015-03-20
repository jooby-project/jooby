[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)
[![Coverage Status](https://img.shields.io/coveralls/jooby-project/jooby.svg)](https://coveralls.io/r/jooby-project/jooby?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby)

jooby
=====

A minimalist web framework for Java 8, inspired by [express.js](http://expressjs.com/) and others ;)

```java

import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () ->
      "Hey Jooby!"
    );
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

versioning
=====

Jooby uses [semantic versioning](http://semver.org/) for releases.

API is considered unstable while release version is: ```0.x.x``` and it might changes and/or broke without previous notification.
This might sounds terrible but isn't. Jooby is plain Java there, then any change on the API will be reported by the Java Compiler :)

{{quickstart.md}}

table of content
=====

{{toc.md}}

{{overview.md}}

{{modules.md}}

{{config.md}}

{{logging.md}}

{{routes.md}}

{{req.md}}

{{rsp.md}}

{{working-with-data.md}}

{{web-sockets.md}}

{{mvc-routes.md}}

{{available-modules.md}}

{{faq.md}}

want to contribute?
=====

* Fork the project on Github.
* Wondering what to work on? See task/bug list and pick up something you would like to work on.
* Write unit tests.
* Create an issue or fix one from [issues](https://github.com/jooby-project/jooby/issues).
* If you know the answer to a question posted to our [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply.
* Share your ideas or ask questions on the [jooby group](https://github.com/jooby-project/jooby/issues) - don't hesitate to write a reply - that helps us improve javadocs/FAQ.
* If you miss a particular feature - browse or ask on the [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply, show us some sample code and describe the problem.
* Write a blog post about how you use or extend [jooby](http://jooby.org).
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.

help and support
=====

* [jooby.org](http://jooby.org)
* [google group](https://groups.google.com/forum/#!forum/jooby-project)
* [issues](https://github.com/jooby-project/jooby/issues)

related projects
=====

 * [Netty](http://netty.io/)
 * [Jetty](http://eclipse.org/jetty)
 * [Undertow](http://undertow.io/)
 * [Guice](https://github.com/google/guice)
 * [Type Safe](https://github.com/typesafehub/config)
 * [Logback](http://logback.qos.ch)

author
=====

 [Edgar Espina] (https://twitter.com/edgarespina)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
