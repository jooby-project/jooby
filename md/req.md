## request

The request object contains methods for reading params, headers and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Request.html).

### request params

The method is defined by the [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) method.

The [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) **always** returns a [Mutant]({{apidocs}}/org/jooby/Mutant.html) instance. A mutant had several utility method for doing type conversion.

Some examples:

```java
get("/", (req, rsp) -> {
  int iparam = req.param("intparam").intValue();

  String str = req.param("str").stringValue();

  // custom object type using type conversion
  MyObject object = req.param("object").to(MyObject.class);

  // file upload
  Upload upload = req.param("file").to(Upload.class);

  // multi value params
  List<String> strList = req.param("strList").toList(String.class);

  // custom object type using type conversion
  List<MyObject> listObj = req.param("objList").toList(MyObject.class);

  // custom object type using type conversion
  Set<MyObject> setObj = req.param("objList").toSet(MyObject.class);

  // optional params
  Optional<String> optStr = req.param("optional").toOptional(String.class);
});
```

#### param types and precedence

A request param can be present at (first listed are higher precedence):

1) path: */user:id*

2) query: */user?id=...* 

3) body: */user* and params are *formurlenconded* or *multipart*

Now, let's suppose a very poor API design and we have a route handler that accept an **id** param in the 3 forms:

A call like:

    curl -X POST -d "id=third" http://localhost:8080/user/first?id=second

Produces:

```java
get("/user/:id", (req, rsp) -> {
  // path param at idx = 0
  assertEquals("first", req.param("id").stringValue());
  assertEquals("first", req.param("id").toList(String.class).get(0));

  // query param at idx = 1
  assertEquals("second", req.param("id").toList(String.class).get(1));

  // body param at idx = 2
  assertEquals("third", req.param("id").toList(String.class).get(2));
});
```

An API like this should be avoided and we mention it here to say that this is possible so you can take note and figure out if something doesn't work as you expect.

#### param type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload]({{apidocs}}/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* There is custom Guice type converter for the type
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules

### request headers

Retrieval of request headers is done via: [request.header("name")]({{}}Request.html#header-java.lang.String-). All the explained before for [request params](#request params) apply for headers too.

```java
get("/", (req, rsp) -> {
  int iparam = req.header("intparam").intValue();

  String str = req.header("str").stringValue();

  // custom object type using type conversion
  MyObject object = req.header("object").to(MyObject.class);

  // file upload
  Upload upload = req.header("file").to(Upload.class);

  // multi value params
  List<String> strList = req.header("strList").toList(String.class);

  // custom object type using type conversion
  List<MyObject> listObj = req.header("objList").toList(MyObject.class);

  // custom object type using type conversion
  Set<MyObject> setObj = req.header("objList").toSet(MyObject.class);

  // optional params
  Optional<String> optStr = req.header("optional").toOptional(String.class);
});
```

### request body

Retrieval of request body is done via [request.body(type)]({{apidocs}}/org/jooby/Request.html#body-com.google.inject.TypeLiteral-).

A [body parser]({{apidocs}}/org/jooby/Body.Parser.html) is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as String or Reader objects. Once the body is read it, it can't be read it again. Jooby distribution includes a [Jackson module](http://jackson.codehaus.org/) that provides support for **json**.

A detailed explanation for body parser is covered later. For now, all you need to know is that they can read/parse the HTTP body.

A body parser is registered in one of two ways:

* with [use]({{apidocs}}/org/jooby/Jooby.html#use-org.jooby.Body.Parser-)

```java
{
   use(new Json());
}
```

* or  from inside an app module:

```java
public void configure(Mode mode, Config config, Binder binder) {
  Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding()
        .toInstance(new MyFormatter());
}
```

### guice access

In previous section we learn you can bind/wire your objects with [Guice](https://github.com/google/guice).

We also learn that a new child injector is created and binded to the current request.

You can ask [Guice](https://github.com/google/guice) to wired an object from the [request.getInstance(type)](http://jooby.org/apidocs/org/jooby/Request.html#getInstance-com.google.inject.Key-)

```java
get("/", (req, rsp) -> {
  A a = req.getInstance(A.class);
});
```
