---
layout: index
title: js
version: 0.9.1
---

# javascript

[Jooby](http://jooby.org) is available in JavaScript via [Nashorn](http://openjdk.java.net/projects/nashorn/).

## hello world

```js

var app = jooby();

app.get('/', function () 'Hey Jooby!');

```

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

Another minor but useful feature is the: **import of classes** and **packages** when you go with the functional version:

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

Import of packages is done via: ```importPackage``` function from in ```nashorn:mozilla_compat.js```.

## routes

Routes work as in Java, but it is worth to mention what are the available alternatives at the time you need to write a route in JavaScript:

```js
jooby(function () {

  // returns a constant value, using Function expression closures
  this.get('/', function () 'Hey Jooby!');

  // returns a constant value
  this.get('/', function () {
    return 'Hey Jooby!';
  });

  // returns a value but access to request object
  this.get('/', function (req) {
    var x = req.require(X)
    return x.doSomething();
  });

  // access to request and rsp (like in express.js)
  this.get('/', function (req, rsp) {
    rsp.send('Hey Jooby!');
  });

  // access to request and rsp
  this.get('/', function (req, rsp, chain) {
    chain.next();
  });

})();
```

## running a javascript app

* via maven: ```mvn jooby:run```
* java: ```java org.jooby.Jooby```. The ```app.js``` file must be present in the app directory
