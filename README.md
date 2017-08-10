[![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)
[![Coverage Status](https://img.shields.io/coveralls/jooby-project/jooby.svg)](https://coveralls.io/r/jooby-project/jooby?branch=master)
[![Google Group](https://img.shields.io/badge/google-group-orange.svg)](https://groups.google.com/forum/#!forum/jooby-project)
[![Join the chat at https://gitter.im/jooby-project/jooby](https://badges.gitter.im/jooby-project/jooby.svg)](https://gitter.im/jooby-project/jooby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby)
[![@joobyproject](https://img.shields.io/badge/twitter--blue.svg)](https://twitter.com/joobyproject)

# &infin; do more, more easily

[Jooby](http://jooby.org) a scalable, fast and modular micro web framework for Java, [JavaScript](http://jooby.org/doc/lang-js) and [Kotlin](http://jooby.org/doc/lang-kotlin).

* **Simple, effective and easy to learn**. Ideal for small but also large scale applications.

* **Scalable**. Stateless application development.

* **Fast**. Thanks to the most popular [NIO web servers](http://jooby.org/doc/servers).

* **Modular**. Make it **full-stack** via the extensive [module eco-system](http://jooby.org/modules).

* **Ready for the modern web**, with the awesome and powerful [asset module](https://github.com/jooby-project/jooby/tree/master/jooby-assets)

## hello world!

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

```kotlin

import org.jooby.*

fun main(args: Array<String>) {
  run(*args) {
    get {
      "Hello Kotlin"
    }
  }
}

```

[JavaScript](http://jooby.org/doc/lang-js):

```js

var app = jooby();

app.get('/', function () 'Hey Jooby!');

```

## found this project useful :heart:

* Support by clicking the :star: button on the upper right of this page. :v:

## killer features

* **Multi-language**. Write your application in Java, [Kotlin](https://github.com/jooby-project/jooby/tree/master/jooby-lang-kotlin) or [JavaScript](https://github.com/jooby-project/jooby/tree/master/jooby-lang-js)
* **Scripting programming model**. Like [express.js](http://expressjs.com), [Sinatra](http://www.sinatrarb.com), etc.. but also
* **MVC programming model**. Like [Spring](http://spring.io) controllers or [Jersey](https://jersey.java.net) resources
* **Multi-server**. Including [Netty](http://netty.io), [Jetty](http://www.eclipse.org/jetty/) and [Undertow](http://undertow.io)
* **HTTPS**
* **HTTP/2**
* **Server-Sent Events**
* **Web-Socket**
* **Dependency Injection**
* **Hot reload** for development

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

 [Edgar Espina] (https://twitter.com/edgarespina)

license
=====

[Apache License 2](http://www.apache.org/licenses/LICENSE-2.0.html)
