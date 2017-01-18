# tests

In this section we are going to see how to run unit and integration tests in Jooby.

## unit tests

We do offer two programming models:

* script programming model; and 
* mvc programming model 

You don't need much for ```MVC``` routes, because a route is binded to a method of some class. So it is usually very easy and simple to mock and run unit tests against a ```MVC``` route.

We can't say the same for ```script``` routes, because a route is represented by a ```lambda``` and there is no easy or simple way to get access to the lambda object.

We do provide a [MockRouter]({{defdocs}}/mvc/MockRouter.html) which simplify unit tests for ```script routes```:

### usage

```java
public class MyApp extends Jooby {

  {
    get("/test", () -> "Hello unit tests!");
  }
}
```

A unit test for this route, looks like:

```java
@Test
public void simpleTest() {
  String result = new MockRouter(new MyApp())
     .get("/test");

  assertEquals("Hello unit tests!", result);
}
```

Just create a new instance of [MockRouter]({{defdocs}}/mvc/MockRouter.html) with your application and call one of the HTTP method, like ```get```, ```post```, etc...

### mocks

You're free to choose the mock library of your choice. Here is an example using <a href="http://easymock.org">EasyMock</a>:

```java
{
  get("/mock", req -> {
    return req.path();
  });
}
```

A test with <a href="http://easymock.org">EasyMock</a> looks like:

```java
@Test
public void shouldGetRequestPath() {
  Request req = EasyMock.createMock(Request.class);

  expect(req.path()).andReturn("/mypath");

  EasyMock.replay(req);

  String result = new MockRouter(new MyApp(), req)
     .get("/mock");

  assertEquals("/mypath", result);

  EasyMock.verify(req);

}
```

You can mock a [Response]({{defdocs}}/Response.html)  object in the same way:

```java
{
  get("/mock", (req, rsp) -> {
    rsp.send("OK");
  });
}
```

A test with <a href="http://easymock.org">EasyMock</a> looks like:

```java
@Test
public void shouldUseResponseSend() {
  Request req = EasyMock.createMock(Request.class);

  Response rsp = EasyMock.createMock(Response.class);

  rsp.send("OK");

  EasyMock.replay(req, rsp);

  String result = new MockRouter(new MyApp(), req, rsp)
     .get("/mock");

  assertEquals("OK", result);

  EasyMock.verify(req, rsp);

}
```

What about external dependencies? It works in a similar way:

```java
{
  get("/", () -> {
    HelloService service = require(HelloService.class);
    return service.salute();
  });
}
```

```java
@Test
public void shouldMockExternalDependencies() {
  HelloService service = EasyMock.createMock(HelloService.class);

  expect(service.salute()).andReturn("Hola!");

  EasyMock.replay(service);

  String result = new MockRouter(new MyApp())
     .set(service)
     .get("/");

  assertEquals("Hola!", result);

  EasyMock.verify(service);

}
```

The [MockRouter#set(Object)]({{defdocs}}/mvc/MockRouter.html#set-java.lang.Object-) call push and register an external dependency (usually a mock). This make it possible to resolve services from ```require``` calls.

### deferred

Mock of promises are possible too:

```java
{
  get("/", promise(deferred -> {
    deferred.resolve("OK");
  }));

}
```

```java
@Test
public void shouldMockPromises() {
  String result = new MockRouter(new MyApp())
     .get("/");

  assertEquals("OK", result);
}
```

Previous test works for deferred routes: 

```java
{
  get("/", deferred(() -> {
    return "OK";
  }));

}
```

## integration tests

Integration tests are possible thanks to a [JUnit Rule](https://github.com/junit-team/junit4/wiki/Rules).

You can choose between `@ClassRule` or `@Rule`. The next example uses `ClassRule`:

```java

import org.jooby.test.JoobyRule;

public class MyIntegrationTest {

  @ClassRule
  private static JoobyRule bootstrap = new JoobyRule(new MyApp());
  
}
```

Here **one** and **only one** instance will be created, which means the application start before the first test and stop after the last test. Application state is **shared** between tests.

While with `Rule` a new application is created per test. If you have N test, then the application will start/stop N times:

```java

import org.jooby.test.JoobyRule;

public class MyIntegrationTest {

  @Rule
  private JoobyRule bootstrap = new JoobyRule(new MyApp());
}
```

Again you are free to choice the HTTP client of your choice, like [Fluent Apache HTTP client](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fluent.html), [REST Assured](https://github.com/rest-assured/rest-assured), etc..

Here is a full example with [REST Assured](https://github.com/rest-assured/rest-assured):

```java
import org.jooby.Jooby;

public class MyApp extends Jooby {

  {
    get("/", () -> "I'm real");
  }

}

import org.jooby.test.JoobyRyle;

public class MyIntegrationTest {

  @ClassRule
  static JoobyRule bootstrap = new JoobyRule(new MyApp());

  @Test
  public void integrationTestJustWorks() {
    get("/")
      .then()
      .assertThat()
      .body(equalTo("I'm real"));
  }
}
```

That's all for now, enjoy testing!

