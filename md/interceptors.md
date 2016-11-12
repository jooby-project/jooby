## interceptors

An interceptor allows to customize a HTTP request and response at three stages:

* **before** the actual handler is invoked
* **after** the handler was executed, but before we send the response
* when response is **complete** and sendt

### before

Allows for customized handler execution chains. It will be invoked before the actual handler. 

```java
{
  before((req, rsp) -> {
    // your code goes here
  });

}
```

You are allowed to modify the [request]({{defdocs}}/Request.html) and [response]({{defdocs}}/Response.html) objects. Please note that the ```before``` handler is just syntax sugar for {{req_filter}}. For example, the ```before``` handler was implemented as: 

```java
{
  use("*", "*", (req, rsp, chain) -> {
    before(req, rsp);
    chain.next(req, rsp);
  });
}
```

A ```before``` handler must to be registered before (of course) the actual handler you want to intercept.

```java
{
  before("/path", (req, rsp) -> {
    // your code goes here
  });

  get("/path", req -> {
    // your code goes here
    return ...;
  });

}
```

If you reverse the order, it won't work. 

> **Remember**: routes are executed in the order they are defined and the pipeline is executed as long you don't generate a response.

### after

Allows for customize response before sending it. It will be invoked at the time a response need to be send. 

```java
{
  after((req, rsp, result) -> {
    // your code goes here
    return result;
  });

}
```

You are allowed to modify the [request]({{defdocs}}/Request.html), [response]({{defdocs}}/Response.html) and [result]({{apidocs}}/org/jooby/Result.html) object. The handler returns a {{result}} which can be the same or an entirely new {{result}}. Please note that the ```after``` handler is just syntax sugar for {{req_filter}}. For example, the ```after``` handler was implemented as:

```java
{
  use("*", (req, rsp, chain) -> {
    chain.next(req, new Response.Forwarding(rsp) {
      public void send(Result result) {
        rsp.send(after(req, rsp, result);
      }
    });
  });

}
```

Due ```after``` is implemented by wrapping the [response]({{defdocs}}/Response.html) object. A ```after``` handler must to be registered before the actual handler you want to intercept.

```java
{
  after((req, rsp, result) -> {
    // your code goes here
    return result;
  });

  get("/path", req -> {
    return "hello";
  });

}
```

If you reverse the order then it won't work. 

> **Remember**: routes are executed in the order they are defined and the pipeline is executed as long you don't generate a response.

### complete

Allows for log and cleanup a request. It will be invoked after we send a response. 

```java
{
  complete("*", (req, rsp, cause) -> {
    // your code goes here
  });

}
```

You are **NOT** allowed to modify the {{request}} and {{response}} objects. The ```cause``` is an ```Optional``` with a ```Throwable``` useful to identify problems. The goal of the ```complete``` handler is to probably cleanup request object and log responses. Please note that the ```complete``` handler is just syntax sugar for {{req_filter}}. For example, the ```complete``` handler was implemented as: 

```java
{
  use("*", "*", (req, rsp, chain) -> {
    Optional<Throwable> err = Optional.empty();
    try {
      chain.next(req, rsp);
    } catch (Throwable cause) {
      err = Optional.of(cause);
      throw cause;
    } finally {
      complete(req, rsp, err);
    }
  });
}
```

A ```complete``` handler must to be registered before the actual handler you want to intercept.

```java
{
  complete("/path", (req, rsp, cause) -> {
    // your code goes here
  });

  get("/path", req -> {
    return "hello";
  });
}
```

If you reverse the order then it won't work.

> **Remember**: routes are executed in the order they are defined and the pipeline is executed as long you don't generate a response.

### example

Suppose you have a transactional resource, like a database connection. The next example shows you how to implement a simple and effective ```transaction-per-request``` pattern:

```java
{
  // start transaction
  before("/api/*", (req, rsp) -> {
    DataSource ds = require(DataSource.class);
    Connection connection = ds.getConnection();
    Transaction trx = connection.getTransaction();
    trx.begin();
    req.set("connection", connection);
    return true;
  });

  // commit/rollback transaction
  complete("/api/*", (req, rsp, cause) -> {
    // unbind connection from request
    try(Connection connection = req.unset("connection").get()) {
      Transaction trx = connection.getTransaction();
      if (cause.ifPresent()) {
        trx.rollback();
      } else {
        trx.commit();
      }
    }
  });

  // your transactional routes goes here
  get("/api/something", req -> {
    Connection connection = req.get("connection");
    // work with connection
  });

}
```
