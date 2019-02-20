[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.jooby/jooby/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.jooby/jooby)
[![javadoc](https://javadoc.io/badge/io.jooby/jooby.svg)](https://javadoc.io/doc/io.jooby/jooby/2.0.0.M1)
[![Become a Patreon](https://img.shields.io/badge/patreon-donate-orange.svg)](https://patreon.com/edgarespina)
[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)
[![Join the chat at https://gitter.im/jooby-project/jooby](https://badges.gitter.im/jooby-project/jooby.svg)](https://gitter.im/jooby-project/jooby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# &infin; do more, more easily

[Jooby](https://jooby.io) a modular micro web framework for Java and Kotlin:

Java:

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", ctx -> "Hey Jooby!");
  }

  public static void main(final String[] args) {
    run(App::new, args);
  }
}

```

[Kotlin](http://jooby.org/doc/lang-kotlin):

```kotlin

import org.jooby.run

fun main(args: Array<String>) {
  run(args) {
    get ("/") { ctx ->
      "Welcome to Jooby!"
    }
  }
}

```

## features

* **Simple, effective and easy to learn**

* **Script routes**: annotation and reflection free programming model using lambdas. Similar to [express.js](http://expressjs.com), [Sinatra](http://www.sinatrarb.com), etc..

* **MVC routes**: annotation and reflection programming model using controller classes. Similar to [Spring](http://spring.io), [Jersey](https://jersey.java.net), etc..

* **Multi-server**: [Jetty](http://www.eclipse.org/jetty/), [Netty](http://netty.io) and [Undertow](http://undertow.io)

## found this project useful :heart:

* Support by clicking the :star: button on the upper right of this page. :v:

* [Donate](https://patreon.com/edgarespina) to support Jooby development!


want to contribute?
=====

* Fork the project on Github, and clone that to your workstation
* Read about [building Jooby](BUILDING.md)
* Wondering what to work on? See task/bug list and pick up something you would like to work on.
* Write unit tests.
* Create an issue or fix one from [issues](https://github.com/jooby-project/jooby/issues).
* If you know the answer to a question posted to our [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply.
* Share your ideas or ask questions on the [jooby group](https://github.com/jooby-project/jooby/issues) - don't hesitate to write a reply - that helps us improve javadocs/FAQ.
* If you miss a particular feature - browse or ask on the [group](https://groups.google.com/forum/#!forum/jooby-project) - don't hesitate to write a reply, show us some sample code and describe the problem.
* Write a blog post about how you use or extend [jooby](https://jooby.io).
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.


author
=====

 [Edgar Espina](https://twitter.com/edgarespina)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
