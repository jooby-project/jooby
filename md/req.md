# request

The request object contains methods for reading params, headers and body (between others). In the next section we will mention the most important method of a request object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Request.html).

## params

Retrieval of param is done via: [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) method.

The [req.param("name")]({{apidocs}}/org/jooby/Request.html#param-java.lang.String-) **always** returns a [Mutant]({{apidocs}}/org/jooby/Mutant.html) instance. A mutant had severals utility methods for doing type conversion.

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
* Is an [Upload]({{apidocs}}/org/jooby/Upload.html)
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

## headers

Retrieval of request headers is done via: [request.header("name")]({{}}Request.html#header-java.lang.String-). All the explained before for [request params](#request-request-params) apply for headers too.

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

## body

Retrieval of request body is done via [request.body()]({{defdocs}}/Request.html#body--).

A [parser]({{apidocs}}/org/jooby/Parser.html) is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as String or Reader objects. Once the body is read it, it can't be read it again.

A detailed explanation for parser is covered later. For now, all you need to know is that they can read/parse the HTTP body.

A body parser is registered in one of two ways:

* with [parser]({{apidocs}}/org/jooby/Jooby.html#parser-org.jooby.Parser-)

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

## form submit

Form submit and parsing via Java object is also supported:

```java
public class Contact {

  private int id;

  private String name;

  private String email;

  public Contact(int id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }
}
```

```html
// enctype: x-www-form-urlencoded or multipart/form-data
<form enctype="application/x-www-form-urlencoded" action="/save" method="post">
  <input name="id" />
  <input name="name" />
  <input name="email" />
</form>
```

```java
{
  post("/save", req -> {
    Contact contact = req.params().to(Contact.class);
    // save contact...
  });
}
```

Nested paths are supported via ```[name]``` notation:

```java
public class Contact {

  private int id;

  private String name;

  private String email;

  // nested path
  private Address address;

  public Contact(int id, String name, String email, Address addres) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.address = address;
  }
}

public class Address {
  private String line;

  private String state;

  private String country;

  publuc Address(String line, String state, String country) {
    this.line = line;
    this.state = state;
    this.country = country;
  }
}
```

```html
// enctype: x-www-form-urlencoded or multipart/form-data
<form enctype="application/x-www-form-urlencoded" action="/save" method="post">
  <input name="id" />
  <input name="name" />
  <input name="email" />
  <input name="address[line]" />
  <input name="address[state]" />
  <input name="address[country]" />
</form>
```

## file upload

File uploads are accessible via: [request.file(name)]({{defdocs}}/Request.html#file-java.lang.String-):

```html
<form enctype="multipart/form-data" action="/upload" method="post">
  <input name="myfile" type="file"/>
</form>
```

```java
// Script API

{
   post("/upload", req -> {
     Upload upload = req.file("myfile");
     ...
   });
}

// MVC API

class Controller {

  @Path("/upload") @POST
  public Object upload(Upload myfile) {
  }
}
```

## locals

Local attributes are bound to the current request. They are created every time a new request comes in and destroyed at the end of the request.

```java
{
  use("*", (req, rsp) -> {
    Object value = ...;
    req.set("var", value);
  });

  get("/locals", req -> {
    // optional local
    Optional<String> ifValue = rsp.ifGet("var");

    // required local
    String value = rsp.get("var");

    // local with default value
    String defvalue = rsp.get("var", "defvalue");

    // all locals
    Map<String, Object> locals = req.attributes();
  });
}
```

In ```mvc routes``` request locals can be injected via ```@Local``` annotation:

```java

  @GET
  public Result localAttr(@Local String var) {
    ...
  }

  @GET
  public Result ifLocalAttr(@Local Optional<String> var) {
    ...
  }

  @GET
  public Result attributes(@Local Map<String, Object> attributes) {
    ...
  }

```

## flash scope

The flash scope is designed to transport success and error messages, between requests. The flash scope is similar to [Session](#session) but lifecycle is shorter: *data are kept for only one request*.

The flash scope is implemented as client side cookie, so it helps to keep application stateless.

### usage

```java

import org.jooby.FlashScope;

...

{
  use(new FlashScope());

  get("/", req -> {
    return req.ifFlash("success").orElse("Welcome!");
  });

  post("/", req -> {
    req.flash("success", "The item has been created");
    return Results.redirect("/");
  });

}
```

[FlashScope]({{defdocs}}/FlashScope.html) is also available on mvc routes via [@Flash]({{defdocs}}/mvc/Flash.html) annotation:

```java
@Path("/")
public class Controller {
  
  // Access to flashScope
  @GET
  public Object flashScope(@Flash Map<String, String> flash) {
    ...
  }

  // Access to a required flash attribute
  @GET
  public Object flashAttr(@Flash String foo) {
    ...
  }

  // Access to an optional flash attribute
  @GET
  public Object optionlFlashAttr(@Flash Optional<String> foo) {
    ... 
  }
} 
```

Worth to mention that flash attributes are accessible from template engine of your choice by prefixing flash attributes with ```flash.```. Here is a (handlebars.java)[/doc/hbs] example:

```html
{{#if flash.success}}
  {{flash.success}}

{{else}}
  Welcome!

{{/if}}
```

## require

In previous section we learnt you can bind/wire your objects with [Guice](https://github.com/google/guice).

You can ask [Guice](https://github.com/google/guice) to wired an object from the [request.require(type)]({{defdocs}}/Request.html#require-com.google.inject.Key-)

```java
get("/", req -> {
  A a = req.require(A.class);
});
```
