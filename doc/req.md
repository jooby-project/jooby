# request

The request object contains methods for reading parameters, headers and body (amongst others). In the next section, the most important methods of a {{request}} object will be explored. If you need more information, please refer to the [javadoc]({{apidocs}}/org/jooby/Request.html).

## parameters

Retrieval of a parameter is done via the: {{req_param}} method.

The {{req_param}} method **always** returns a {{mutant}} instance. A {{mutant}} has several utility methods for doing type conversion:

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

Bean classes must have a ```default constructor``` or a constructor annotated with `javax.inject.Inject`.

### parameter type and precedence

A {{request}} parameter can be present in:

1) **path**: `/user/:id`

2) **query string**: `/user?id=...` 

3) **form submit** encoded as {{formurlencoded}} or {{formmultipart}}

(parameters take precedence in the order listed)

For example purposes, let's consider a poorly constructed API where we have a route handler that accepts an **id** parameter in all three locations:

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

While clearly bad API design, this is a good example of how parameter precedence works.

### parameter type conversion

Automatic type conversion is provided when a type:

* Is a primitive, primitive wrapper or String
* Is an enum
* Is an {{file_upload}}
* Has a public **constructor** that accepts a single **String** argument
* Has a static method **valueOf** that accepts a single **String** argument
* Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```
* Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```
* Is an Optional<T>, List<T>, Set<T> or SortedSet<T> where T satisfies one of the previous rules

Custom type conversion is also possible:

```java

parser((type, ctx) -> {
  if (type.getRawType() == MyType.class) {
    // convert the type here
    return ctx.param(values -> new MyType(values.get(0)));
  }
  // no luck! move to next converter
  return ctx.next();
});

get("/", req -> {
  MyType myType = req.param("value").to(MyType.class);
});
```

See [parser and renderer](/doc/#parser-and-renderer).

## headers

Retrieval of request headers is done via: {{req_header}}. What goes for the [request params](#request-parameters) applies for the headers as well.

## body

Retrieval of the request body is done via {{req_body}} or {{req_bodyc}}.

A {{parser}} is responsible for either parsing or converting the HTTP request body to something else.

There are a few built-in parsers for reading the body as `String`, `primitives`, ..., etc.

[Parsers](/doc/#parser-and-renderer) are explained later. For now, all you need to know is that they can read or parse the HTTP body.

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

> **NOTE**: Don't use {{req_body}}, {{req_bodyc}} or [@Body]({{defdocs}}/mvc/Body.html) for a `POST` request encoded as {{formurlencoded}} or {{formmultipart}}, see the next section for such requests.

## form submit

Form submit parsing is done via [req.params(Class)]({{defdocs}}/Request.html#params-java.lang.Class-) or the [req.form(Class)]({{defdocs}}/Request.html#form-java.lang.Class-) method:

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

  public Address(String line, String state, String country) {
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

Tabular data is supported as well:

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

> **NOTE**: Constructor injection of nested or tabular objects isn't supported. Nested/tabular object are injected via either method or field.

The injection rules are defined as follows (higher priority first):

* There is a only one constructor (nested/tabular data can't be injected, just simple values).
* There are more than a single constructor but only one annotated with `javax.inject.Inject` (nested/tabular data can't be injected, just simple values).
* There is a setter like method that matches the parameter name, like `name(String)`, `setName(String)`, etc...
* There is a field that matches the parameter name

## file upload

File uploads are accessible via the: [request.file(name)]({{defdocs}}/Request.html#file-java.lang.String-) method:

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

Local attributes (a.k.a request attributes) are bound to the current request. They are created every time a new request is received and destroyed at the end of the request cycle.

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

In ```mvc routes``` request locals can be injected via the ```@Local``` annotation by defining a method parameter with the same name as a request local:

```java

  @GET
  public Result localAttr(@Local String var) {
    // either var will be set to a previously configured value or an error is thrown when this method is requested
  }

  @GET
  public Result ifLocalAttr(@Local Optional<String> var) {
    // var will contain an optional that may be empty if no value was previously configured
  }

  @GET
  public Result attributes(@Local Map<String, Object> attributes) {
    // attributes contains the map of local attributes
  }

```

Locals that are available by default include:

* `path`: the request path, i.e. `/myroute`
* `contextPath`: application path (a.k.a context path). It is the value defined by: `application.path`.
* When the `Assets` module is enabled: your asset filesets postfixed with `_css` and `_js`
* When the `Flash` module is enabled: `flash`, the map of flash key/values

## flash scope

The flash scope is designed to transport success and error messages between requests. It is similar to a [Session](#session) but the lifecycle is shorter: *data is kept for only one request*.

The flash scope is implemented as a client side cookie, keeping the application stateless.

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

The [FlashScope]({{defdocs}}/FlashScope.html) is also available on mvc routes via the [@Flash]({{defdocs}}/mvc/Flash.html) annotation:

```java
@Path("/")
public class Controller {
  
  // Access to the flashScope
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

Flash attributes are accessible from templates by prefixing the attribute's name with ```flash.```. Here's a (handlebars.java)[/doc/hbs] example:

```html
{{#if flash.success}}
  {{flash.success}}

{{else}}
  Welcome!

{{/if}}
```

## require

The {{request}} object has access to the application [registry]({{defdocs}}/Registry.html) which give you access to application services.

Access to the registry is available via: [request.require(type)]({{defdocs}}/Request.html#require-com.google.inject.Key-) methods:

```java
get("/", req -> {
  Foo foo = require(Foo.class);
  return foo.bar();
});
```

Of course, the `require` method doesn't make sense on `MVC routes`, because in that case you can inject dependencies:

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

Log all matching incoming requests using the <a href="https://en.wikipedia.org/wiki/Common_Log_Format">NCSA format</a> (a.k.a common log format).

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

Since authentication is provided via a module or custom filter, there is no concept of a logged in or authenticated user. You can still log the current user by setting a user id provider at construction time:

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

In this example, an application filter sets a ```userId``` request attribute and then that ```userId``` is provided to the {@link RequestLogger}.

### custom log function

By default all logging uses <a href="http://logback.qos.ch">logback</a> which is why the ```org.jooby.RequestLogger``` was configured in ```logback.xml```.

If you want to log somewhere else, or want to use a different logging implementation:

```java
{
  use("*", new ResponseLogger()
    .log(line -> {
      System.out.println(line);
    }));
}
```

Rather than printing the ```NCSA``` line to stdout, it can of course be written to a database, a JMS queue, etc.

### latency

```java
{
  use("*", new RequestLogger()
      .latency());
}
```

This will append an entry at the end of the ```NCSA``` output representing the number of ```ms``` it took to process the request.

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

Override the default formatter for the request arrival time defined by: [Request#timestamp()]({{defdocs}}/Request.html#timestamp----). You can provide a function or an instance of `DateTimeFormatter`.

The default formatter uses the default server time zone, provided by `ZoneId#systemDefault()`. It's possible to simply override the time zone as well:

```java
{
  use("*", new RequestLogger()
     .dateFormatter(ZoneId.of("UTC"));
}
```
