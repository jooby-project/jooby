# request

The request object contains methods for reading parameters, headers and body (among others). In the next section we will mention the most important method of a {{request}} object, if you need more information please refer to the [javadoc]({{apidocs}}/org/jooby/Request.html).

## parameters

Retrieval of parameter is done via: {{req_param}} method.

The {{req_param}} **always** returns a {{mutant}} instance. A {{mutant}} had severals utility methods for doing type conversion:

```java
get("/", req -> {
  int iparam = req.param("intparam").intValue();

  String str = req.param("str").value();
  String defstr = req.param("str").value("def");

  // custom object type using type conversion
  MyObject object = req.param("object").to(MyObject.class);

  // file upload
  Upload upload = req.file("file");

  // multi value parameter
  List<String> strList = req.param("strList").toList(String.class);

  // custom object type using type conversion
  List<MyObject> listObj = req.param("objList").toList(MyObject.class);

  // custom object type using type conversion
  Set<MyObject> setObj = req.param("objList").toSet(MyObject.class);

  // optional parameter
  Optional<String> optStr = req.param("optional").toOptional();
});
```

Multiple parameters can be retrieved at once:

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
     Profile profile = req.params(Profile.class);
     System.out.println(profile.getName()); // print John
     System.out.println(profile.getAge()); // print 99
     System.out.println(profile.getAddress()); // print 99
  });
}
```

Bean class must have a ```default constructor``` or a constructor annotated with `javax.inject.Injext`.

### parameter type and precedence

A {{request}} parameter can be present at:

1) **path level**: `/user/:id`

2) **query query**: `/user?id=...` 

3) **form submit** encoded as {{formurlencoded}} or {{formmultipart}}

(first listed are higher precedence)

Now, let's suppose a very poor API where we have a route handler that accept an **id** parameter in the 3 location:

A call like:

```
curl -X POST -d "id=third" http://localhost:8080/user/first?id=second
```

Produces:

```java
get("/user/:id", req -> {
  // path param at idx = 0
  assertEquals("first", req.param("id").value());
  assertEquals("first", req.param("id").toList().get(0));

  // query param at idx = 1
  assertEquals("second", req.param("id").toList().get(1));

  // form param at idx = 2
  assertEquals("third", req.param("id").toList().get(2));
});
```

It is clear that an API like this should be avoided, but is a good example of parameter precedence.

### parameter type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an {{file_upload}}
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

See [parser and renderer](/doc/#parser-and-renderer).

## headers

Retrieval of request headers is done via: {{req_header}}. All the explained before for [request params](#request-parameters) apply for headers too.

## body

Retrieval of request body is done via {{req_body}} or {{req_bodyc}}.

A {{parser}} is responsible for parse or convert the HTTP request body to something else.

There are a few built-in parsers for reading body as `String`, `primitives`, ..., etc.

[Parsers](/doc/#parser-and-renderer) are explained later. For now, all you need to know is that they can read/parse the HTTP body.

Script API:

```java
{

  post("/save", req -> {
    MyObject object = req.body(MyObject.class);
    ...
  });
}
```

MVC API:

```java

public class Controller {

  @POST
  @Path("/save")
  public MyObject save(@Body MyObject object) {
    ...
  }
}
```

> **NOTE**: Don't use {{req_body}}, {{req_bodyc}} or [@Body]({{defdocs}}/mvc/Body.html) for `POST` encoded as {{formurlencoded}} or {{formmultipart}}, see next section for such requests.

## form submit

Form submit parsing is done via [req.params(Class)]({{defdocs}}/Request.html#params-java.lang.Class-) or [req.form(Class)]({{defdocs}}/Request.html#form-java.lang.Class-) method:

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
<form enctype="application/x-www-form-urlencoded" action="/save" method="post">
  <input name="id" />
  <input name="name" />
  <input name="email" />
</form>
```

Script API:

```java
{
  post("/save", req -> {
    Contact contact = req.params(Contact.class);
    // save contact...
  });
}
```

MVC API:

```java
public class Controller {

  @POST
  @Path("/save")
  public Result submit(Contact contact) {
    ...
  }
}
```

Nested paths are supported via **bracket**: ```[name]``` or **dot**: `.name` notation:

```java
public class Contact {

  private int id;

  private String name;

  private String email;

  // nested path
  private Address address;

  public Contact(int id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
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
<form enctype="application/x-www-form-urlencoded" action="/save" method="post">
  <input name="id" />
  <input name="name" />
  <input name="email" />
  <input name="address[line]" />
  <input name="address.state" />
  <input name="address[country]" />
</form>
```

Tabular data is supported too:

```java
public class Contact {

  private int id;

  private String name;

  private String email;

  // nested path
  private List<Address> address;

  public Contact(int id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }
}
```

```html
<form enctype="application/x-www-form-urlencoded" action="/save" method="post">
  <input name="id" />
  <input name="name" />
  <input name="email" />
  <input name="address[0]line" />
  <input name="address[0]state" />
  <input name="address[0]country" />
  <input name="address[1]line" />
  <input name="address[1]state" />
  <input name="address[1]country" />
</form>
```

> **NOTE**: Constructor injection of nested or tabular objects isn't supported. Nested/tabular object are injected via method or field.

The injection rules are defined as follow (first listed are higher priority):

* There is a only one constructor (nested/tabular data can't be injected, just simple values).
* There are more than a single constructor but only one annotated with `javax.inject.Inject` (nested/tabular data can't be injected, just simple values).
* There is a setter like method that matches the parameter name, like `name(String)`, `setName(String)`, etc...
* There is a field that matches the parameter name

## file upload

File uploads are accessible via: [request.file(name)]({{defdocs}}/Request.html#file-java.lang.String-) method:

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
     upload.close();
   });
}

// MVC API
class Controller {

  @Path("/upload") @POST
  public Object upload(Upload myfile) {
    ...
    myfile.close();
  }
}
```

You must close a {{file_upload}} in order to release resources:

## locals

Local attributes (a.k.a request attributes) are bound to the current request. They are created every time a new request comes in and destroyed at the end of the request.

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

Worth to mention that flash attributes are accessible from template engine of your choice by prefixing flash attribute's name with ```flash.```. Here is a (handlebars.java)[/doc/hbs] example:

```html
{{#if flash.success}}
  {{flash.success}}

{{else}}
  Welcome!

{{/if}}
```

## require

The {{request}} object has access to the application [registry]({{defdocs}}/Registry.html) which give you access to application services.

Access to registry is available via: [request.require(type)]({{defdocs}}/Request.html#require-com.google.inject.Key-) methods:

```java
get("/", req -> {
  Foo foo = req.require(Foo.class);
  return foo.bar();
});
```

Of course the `require` method doesn't make sense on `MVC routes`, because you can inject dependencies:

```java
@Path("/")
public class Controller {

  private Foo foo;

  @Inject
  public Controller(Foo foo) {
    this.foo = foo;
  }

  @GET
  public String doSomething() {
    return foo.bar();
  }
}
```

## access log

Log all the matched incoming requested using the <a href="https://en.wikipedia.org/wiki/Common_Log_Format">NCSA format</a> (a.k.a common log format).

### usage

```java
{
  use("*", new RequestLog());

  ...
}
```

Output looks like:

```
127.0.0.1 - - [04/Oct/2016:17:51:42 +0000] "GET / HTTP/1.1" 200 2
```

You probably want to configure the ```RequestLog``` logger to save output into a new file:

```xml
<appender name="ACCESS" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>access.log</file>

  <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>access.%d{yyyy-MM-dd}.log</fileNamePattern>
  </rollingPolicy>

  <encoder>
    <pattern>%msg%n</pattern>
  </encoder>
</appender>

<logger name="org.jooby.RequestLogger" additivity="false">
  <appender-ref ref="ACCESS" />

</logger>
```

Due that authentication is provided via module or custom filter, there is no concept of logged/authenticated user. Still you can log the current user by setting an user id provider at construction time:

```java
{
  use("*", (req, rsp) -> {

    // authenticate user and set local attribute
    String userId = ...;
    req.set("userId", userId);
  });

  use("*", new RequestLogger(req -> {
    return req.get("userId");
  }));

}
```

Here an application filter set an ```userId``` request attribute and then we provide that ```userId``` to {@link RequestLogger}.

### custom log function

By default it uses the underlying logging system: <a href="http://logback.qos.ch">logback</a>. That's why we previously show how to configure the ```org.jooby.RequestLogger``` in ```logback.xml```.

If you want to log somewhere else and/or use a different technology then:

```java
{
  use("*", new ResponseLogger()
    .log(line -> {
      System.out.println(line);
    }));
}
```

This is just an example but of course you can log the ```NCSA``` line to database, jms queue, etc...

### latency

```java
{
  use("*", new RequestLogger()
      .latency());
}
```

It add a new entry at the end of the ```NCSA``` output that represents the number of ```ms``` it took to process the request.

### extended

```java
{
  use("*", new RequestLogger()
      .extended());
}
```

Extend the ```NCSA``` by adding the ```Referer``` and ```User-Agent``` headers to the output.

### dateFormatter

```java
{
  use("*", new RequestLogger()
      .dateFormatter(ts -> ...));

  // OR
  use("*", new RequestLogger()
      .dateFormatter(DateTimeFormatter...));
}
```

Override, the default formatter for the request arrival time defined by: [Request#timestamp()]({{defdocs}}/Request.html#timestamp----). You can provide a function or an instance of `DateTimeFormatter`.

The default formatter use the default server time zone, provided by `ZoneId#systemDefault()`. It's possible to just override the time zone too:

```java
{
  use("*", new RequestLogger()
     .dateFormatter(ZoneId.of("UTC"));
}
```
