# tests

This section will show you how to run unit and integration tests with Jooby.

## unit tests

There are two available programming models:

* the script programming model; and 
* the mvc programming model 

Testing ```MVC``` routes is pretty straightforward since a route is bound to a method of some class. This makes it simple to mock and run unit tests against these kinds of routes.

To test ```script``` routes is more involved since a route is represented by a ```lambda``` and there is no easy or simple way to get access to the lambda object.

For this reason there's a [MockRouter]({{defdocs}}/test/MockRouter.html) provided to simplify unit testing of ```script routes```:

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

Just create a new instance of [MockRouter]({{defdocs}}/test/MockRouter.html) with your application and call one of the HTTP method, like ```get```, ```post```, etc...

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

You can mock a [Response]({{defdocs}}/Response.html) object similarly:

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

What about external dependencies? This is handled similarly:

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

The [MockRouter#set(Object)]({{defdocs}}/test/MockRouter.html#set-java.lang.Object-) calls push and will register an external dependency (usually a mock). This makes it possible to resolve services from ```require``` calls.

### deferred

Mocking of promises is possible as well:

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

The previous test works for deferred routes: 

```java
{
  get("/", deferred(() -> {
    return "OK";
  }));

}
```

## integration tests

Integration tests are possible thanks to a [JUnit Rule](https://github.com/junit-team/junit4/wiki/Rules).

You can choose between `@ClassRule` or `@Rule`. The following example uses `ClassRule`:

```java

import org.jooby.test.JoobyRule;

public class MyIntegrationTest {

  @ClassRule
  private static JoobyRule bootstrap = new JoobyRule(new MyApp());
  
}
```

Here **one** and **only one** instance will be created, which means the application will start before the first test and stop after the last test. Application state is **shared** between tests.

With `Rule` on the other hand, a new application is created per test. If you have N tests, the application will start and stop N times:

```java

import org.jooby.test.JoobyRule;

public class MyIntegrationTest {

  @Rule
  private JoobyRule bootstrap = new JoobyRule(new MyApp());
}
```

Again you are free to select the HTTP client of your choice, like [Fluent Apache HTTP client](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/fluent.html), [REST Assured](https://github.com/rest-assured/rest-assured), etc.

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

