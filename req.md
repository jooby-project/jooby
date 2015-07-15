# request

The request object contains methods for reading params, headers and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc](/apidocs/org/jooby/Request.html).

## request params

Retrieval of param is done via: [req.param("name")](/apidocs/org/jooby/Request.html#param-java.lang.String-) method.

The [req.param("name")](/apidocs/org/jooby/Request.html#param-java.lang.String-) **always** returns a [Mutant](/apidocs/org/jooby/Mutant.html) instance. A mutant had severals utility methods for doing type conversion.

Some examples:

```java
get("/", req -> {
  int iparam = req.param("intparam").intValue();

  String str = req.param("str").value();

  // custom object type using type conversion
  MyObject object = req.param("object").to(MyObject.class);

  // file upload
  Upload upload = req.param("file").toUpload();

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

Multiple params can be retrieved at once:

```
GET /search?name=John&age=99&address[country]=AR&address[city]=BA
```

```java
public class Profile {
  String name;

  int age;

  Address address;

}

public class Address {
  String country;
  String city;
  ...
}
```

```java
{
  get("/search", req -> {
     Profile profile = req.params().to(Profile.class);
     System.out.println(profile.getName()); // print John
     System.out.println(profile.getAge()); // print 99
     System.out.println(profile.getAddress()); // print 99
  });
}
```

Binding is done at ```field``` level (no need of getter/setters). Field should not be ```static```, ```transient``` or ```final```. Also root or embedded classes must have a ```default constructor```.


### param types and precedence

A request param can be present at:

1) path: GET */user/:id*

2) query: GET */user?id=...* 

3) body: POST to */user* as *formurlenconded* or *multipart*

(first listed are higher precedence)

Now, let's suppose a very poor API where we have a route handler that accept an **id** param in the 3 forms:

A call like:

```bash
curl -X POST -d "id=third" http://localhost:8080/user/first?id=second
```

Produces:

```java
get("/user/:id", req -> {
  // path param at idx = 0
  assertEquals("first", req.param("id").value());
  assertEquals("first", req.param("id").toList(String.class).get(0));

  // query param at idx = 1
  assertEquals("second", req.param("id").toList(String.class).get(1));

  // body param at idx = 2
  assertEquals("third", req.param("id").toList(String.class).get(2));
});
```

It is clear that an API like this should be avoided.

### param type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an [Upload](/apidocs/org/jooby/Upload.html)
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* It is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of previous rules

Custom type conversion is also possible:

```java

parser((type, ctx) -> {
  if (type.getRawType() == MyType.class) {
    // convert the type here
    return ctx.param(values -> new MyType(values.get(0)));
  }
  // no luck! move to next converter
  return next.next();
});

get("/", req -> {
  MyType myType = req.param("value").to(MyType.class);
});
```

## request headers

Retrieval of request headers is done via: [request.header("name")]({{}}Request.html#header-java.lang.String-). All the explained before for [request params](#request params) apply for headers too.

```java
get("/", req -> {
  int iparam = req.header("intparam").intValue();

  String str = req.header("str").value();

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

## request body

Retrieval of request body is done via [request.body()](/apidocs/org/jooby/Request.html#body).

A [parser](/apidocs/org/jooby/Parser.html) is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as String or Reader objects. Once the body is read it, it can't be read it again.

A detailed explanation for parser is covered later. For now, all you need to know is that they can read/parse the HTTP body.

A body parser is registered in one of two ways:

* with [parser](/apidocs/org/jooby/Jooby.html#parser-org.jooby.Parser-)

```java
{
   parser(new MyParser());
}
```

* or  from inside a module:

```java
public void configure(Mode mode, Config config, Binder binder) {
  Multibinder.newSetBinder(binder, Parser.class)
        .addBinding()
        .toInstance(new MyParser());
}
```

## local variables
Local variables are bound to the current request. They are created every time a new request is processed and destroyed at the end of the request.

```java
  req.set("var", var);
  String var = rsp.get("var");
```

## guice access

In previous section we learnt you can bind/wire your objects with [Guice](https://github.com/google/guice).

You can ask [Guice](https://github.com/google/guice) to wired an object from the [request.require(type)](/apidocs/org/jooby/Request.html#require-com.google.inject.Key-)

```java
get("/", req -> {
  A a = req.require(A.class);
});
```
