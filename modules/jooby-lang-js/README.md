# javascript

[Jooby](http://jooby.org) is available in JavaScript via [Nashorn](http://openjdk.java.net/projects/nashorn/).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-lang-js</artifactId>
  <version>1.1.3</version>
</dependency>
```

## usage

Create an `app.js` file in your project directory: 

```js

var app = jooby();

app.get('/', function () 'Hey Jooby!');

```

In your `pom.xml`, set the `application.class` property to `org.jooby.JoobyJs` like this:

```xml
<properties>
  <application.class>org.jooby.JoobyJs</application.class>
</properties>
```

The `org.jooby.JoobyJs` class will set up [Nashorn](http://openjdk.java.net/projects/nashorn/) and start `Jooby`.

## jooby function

The ```jooby``` function always returns a new application:

```js
var app = jooby();
```

or pass a function to ```jooby```:

```js
jooby(function () {

  this.get('/', function () 'Hey Jooby!');

})();
```

Another minor, but useful feature is the: **import of classes** and **packages** when you go with the functional version:

```js
jooby(function (Jackson) {

  use(new Jackson());

  this.get('/', function () 'Hey Jooby!');

})(org.jooby.json.Jackson);
```

Or import an entire package:

```js
jooby(function (Jackson) {

  use(new Jackson());

  this.get('/', function () 'Hey Jooby!');

})(org.jooby.json);
```

Import of packages is done via: ```importPackage``` function from ```nashorn:mozilla_compat.js```.

## routes

Routes work as in Java, but it is worth mentioning the available alternatives when you write a route in JavaScript:

```js
jooby(function () {

  // returns a constant value, using Function expression closures
  this.get('/', function () 'Hey Jooby!');

  // returns a constant value
  this.get('/', function () {
    return 'Hey Jooby!';
  });

  // returns a value, but with access to the request object
  this.get('/', function (req) {
    var x = require(X)
    return x.doSomething();
  });

  // access to the request and the response (as in express.js)
  this.get('/', function (req, rsp) {
    rsp.send('Hey Jooby!');
  });

  // access to the request and the response
  this.get('/', function (req, rsp, chain) {
    chain.next();
  });

})();
```

## running a javascript app

* via maven: ```mvn jooby:run```
* java: ```java org.jooby.JoobyJs```. The ```app.js``` file must be present in the app directory
