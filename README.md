[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby/1.6.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby.svg)](https://javadoc.io/doc/org.jooby/jooby/1.6.1)
[![Become a Patreon](https://img.shields.io/badge/patreon-donate-orange.svg)](https://patreon.com/edgarespina)
[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)
[![coveralls.io](https://img.shields.io/coveralls/jooby-project/jooby.svg)](https://coveralls.io/r/jooby-project/jooby?branch=master)
[![codecov.io](https://codecov.io/gh/jooby-project/jooby/branch/master/graph/badge.svg)](https://codecov.io/gh/jooby-project/jooby)
[![Google Group](https://img.shields.io/badge/group-joobyproject-orange.svg)](https://groups.google.com/forum/#!forum/jooby-project)
[![Join the chat at https://gitter.im/jooby-project/jooby](https://badges.gitter.im/jooby-project/jooby.svg)](https://gitter.im/jooby-project/jooby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Follow us on twitter](https://img.shields.io/badge/twitter-@joobyproject-blue.svg)](https://twitter.com/joobyproject)

# &infin; do more, more easily

[Jooby](http://jooby.org) a modular micro web framework for Java and [Kotlin](http://jooby.org/doc/lang-kotlin):

Java:

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () -> "Hey Jooby!");
  }

  public static void main(final String[] args) {
    run(App::new, args);
  }
}

```

[Kotlin](http://jooby.org/doc/lang-kotlin):

```java

import org.jooby.*

class App: Kooby({
  get {
    "Hello Jooby!"
  }
})

fun main(args: Array<String>) {
  run(::App, *args)
}

```

## features

* **Simple, effective and easy to learn**

* **Fast development cycles**: run, debug and reload your application using [devtools](http://jooby.org/doc/devtools) available for Maven and Gradle

* **Script routes**: annotation and reflection free programming model using lambdas. Similar to [express.js](http://expressjs.com), [Sinatra](http://www.sinatrarb.com), etc..

* **MVC routes**: annotation and reflection programming model using controller classes. Similar to [Spring](http://spring.io), [Jersey](https://jersey.java.net), etc..

* **Multi-server**: [Jetty](http://www.eclipse.org/jetty/), [Netty](http://netty.io) and [Undertow](http://undertow.io)

* **Multi-protocol**: `HTTP`, `HTTPS`, `HTTP 2.0`, `Server-Sent Events` and `Web-Socket`

* **Modular**. Make it **full-stack** via the extensive [module eco-system](http://jooby.org/modules)

* **Ready for the modern web** with the [asset management](http://jooby.org/doc/asset-management) tools

## found this project useful :heart:

* Support by clicking the :star: button on the upper right of this page. :v:

* Buy [Edgar](https://patreon.com/edgarespina) a beer!


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
* Write a blog post about how you use or extend [jooby](http://jooby.org).
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.


author
=====

 [Edgar Espina](https://twitter.com/edgarespina)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
