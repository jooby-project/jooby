package org.jooby.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Status;
import org.jooby.View;
import org.jooby.test.MockRouter;
import org.junit.Test;

import com.google.inject.Injector;

public class MockRouterTest {

  public static class HelloService {

    public String hello() {
      return "Hi";
    }

  }

  public static class MyForm {

    public String hello() {
      return "Hi";
    }

  }

  public static class HelloWorld extends Jooby {
    {

      before("/before", (req, rsp) -> {
        req.charset();
      });

      after("/before", (req, rsp, result) -> {
        req.charset();
        return result;
      });

      after("/afterResult", (req, rsp, result) -> {
        return Results.with(result.get() + ":unit");
      });

      get("/afterResult", () -> {
        return Results.with("hello");
      });

      get("/injector", () -> {
        return require(Injector.class).getParent();
      });

      get("/async", promise(deferred -> {
        deferred.resolve("async");
      }));

      get("/deferred", deferred(() -> {
        return "deferred";
      }));

      get("/deferred-err", deferred(() -> {
        throw new IllegalStateException("intentional err");
      }));

      get("/deferred-executor", deferred("executor", () -> {
        return "deferred";
      }));

      get("/before", req -> req.charset());

      post("/form", req -> req.form(MyForm.class).hello());

      put("/form", req -> req.form(MyForm.class).hello());

      patch("/form", req -> req.form(MyForm.class).hello());

      delete("/item/:id", req -> req.param("id").intValue());

      get("/result", req -> Results.html("index"));

      get("/hello", () -> "Hello world!");

      get("/request", req -> req.path());

      get("/rsp.send", (req, rsp) -> {
        rsp.send("Response");
      });

      get("/rsp.send.result", (req, rsp) -> {
        rsp.send(Results.with("Response"));
      });

      AtomicInteger inc = new AtomicInteger(0);
      get("/chain", (req, rsp) -> inc.incrementAndGet());
      get("/chain", (req, rsp) -> inc.incrementAndGet());
      get("/chain", () -> inc.incrementAndGet());

      get("/require", () -> {
        return require(HelloService.class).hello();
      });

      get("/requirenamed", () -> {
        return require("foo", HelloService.class).hello();
      });

      get("/params", req -> {
        return req.param("foo").value("bar");
      });

      get("/rsp.committed", (req, rsp) -> {
        rsp.send("committed");
      });

      get("/rsp.committed", (req, rsp) -> {
        rsp.send("ignored");
      });

      get("/before", req -> req.charset());
    }
  }

  @Test
  public void basicCall() throws Exception {
    new MockUnit()
        .run(unit -> {
          String result = new MockRouter(new HelloWorld())
              .get("/hello");
          assertEquals("Hello world!", result);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fakedInjector() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/injector");
        });
  }

  @Test
  public void post() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.form(MyForm.class)).andReturn(new MyForm());
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .post("/form");
          assertEquals("Hi", result);
        });
  }

  @Test
  public void put() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.form(MyForm.class)).andReturn(new MyForm());
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .put("/form");
          assertEquals("Hi", result);
        });
  }

  @Test
  public void patch() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.form(MyForm.class)).andReturn(new MyForm());
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .patch("/form");
          assertEquals("Hi", result);
        });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Mutant id = unit.mock(Mutant.class);
          expect(id.intValue()).andReturn(123);

          Request req = unit.get(Request.class);
          expect(req.param("id")).andReturn(id);
        })
        .run(unit -> {
          Integer result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .delete("/item/123");
          assertEquals(123, result.intValue());
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void requestAccessEmptyRequest() throws Exception {
    new MockUnit()
        .run(unit -> {
          new MockRouter(new HelloWorld())
              .get("/request");
        });
  }

  @Test
  public void requestAccess() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.path()).andReturn("/mock-path");
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/request");
          assertEquals("/mock-path", result);
        });
  }

  @Test
  public void routeChain() throws Exception {
    new MockUnit(Request.class)
        .run(unit -> {
          Integer result = new MockRouter(new HelloWorld(),
              unit.get(Request.class))
                  .get("/chain");
          assertEquals(3, result.intValue());
        });
  }

  @Test
  public void responseSend() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("Response");
        })
        .run(unit -> {
          Object result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/rsp.send");

          assertEquals("Response", result);
        });
  }

  @Test
  public void responseSendResult() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(isA(Result.class));
        })
        .run(unit -> {
          Result result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/rsp.send.result");

          assertEquals("Response", result.get());
        });
  }

  @Test
  public void requireService() throws Exception {
    new MockUnit(Request.class, Response.class, HelloService.class)
        .expect(unit -> {
          HelloService rsp = unit.get(HelloService.class);
          expect(rsp.hello()).andReturn("Hola");
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .set(unit.get(HelloService.class))
                  .get("/require");

          assertEquals("Hola", result);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void serviceNotFound() throws Exception {
    new MockUnit(Request.class, Response.class, HelloService.class)
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/require");

          assertEquals("Hola", result);
        });
  }

  @Test
  public void requireNamedService() throws Exception {
    new MockUnit(Request.class, Response.class, HelloService.class)
        .expect(unit -> {
          HelloService rsp = unit.get(HelloService.class);
          expect(rsp.hello()).andReturn("Named");
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .set("foo", unit.get(HelloService.class))
                  .get("/requirenamed");

          assertEquals("Named", result);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void requireNamedServiceNotFound() throws Exception {
    new MockUnit(Request.class, Response.class, HelloService.class)
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/requirenamed");

          assertEquals("Named", result);
        });
  }

  @Test
  public void requestMockParam() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Mutant foo = unit.mock(Mutant.class);
          expect(foo.value("bar")).andReturn("mock");
          Request req = unit.get(Request.class);
          expect(req.param("foo")).andReturn(foo);
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/params");

          assertEquals("mock", result);
        });
  }

  @Test
  public void beforeAfterRequest() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.charset()).andReturn(StandardCharsets.US_ASCII).times(3);
        })
        .run(unit -> {
          Charset result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/before");

          assertEquals(StandardCharsets.US_ASCII, result);
        });
  }

  @Test
  public void afterResult() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          Result result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/afterResult");

          assertEquals("hello:unit", result.get());
        });
  }

  @Test
  public void resultResponse() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          View result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/result");

          assertEquals("index", result.name());
        });
  }

  @Test
  public void responseCommitted() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("committed");
        })
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/rsp.committed");

          assertEquals("committed", result);
        });
  }

  @Test
  public void async() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/async");

          assertEquals("async", result);
        });
  }

  @Test
  public void deferred() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/deferred");

          assertEquals("deferred", result);
        });

    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          String result = new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/deferred-executor");

          assertEquals("deferred", result);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void deferredReject() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          new MockRouter(new HelloWorld(),
              unit.get(Request.class),
              unit.get(Response.class))
                  .get("/deferred-err");
        });
  }

  @Test
  public void notFound() throws Exception {
    new MockUnit(Request.class, Response.class)
        .run(unit -> {
          try {
            new MockRouter(new HelloWorld(),
                unit.get(Request.class),
                unit.get(Response.class))
                    .get("/notFound");
            fail();
          } catch (Err x) {
            assertEquals(Status.NOT_FOUND.value(), x.statusCode());
          }
        });
  }

}
