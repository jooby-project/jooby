package io.jooby;

import examples.InstanceRouter;
import examples.JAXRS;
import examples.LoopDispatch;
import examples.Message;
import examples.MvcBody;
import examples.NoTopLevelPath;
import examples.NullInjection;
import examples.ProducesConsumes;
import examples.Provisioning;
import examples.TopDispatch;
import io.jooby.json.JacksonModule;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import static io.jooby.MediaType.xml;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MvcTest {

  @Test
  public void routerInstance() {
    new JoobyRunner(app -> {

      app.mvc(new InstanceRouter());

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Got it!", rsp.body().string());
      });

      client.post("/", rsp -> {
        assertEquals("Got it!", rsp.body().string());
      });

      client.get("/subpath", rsp -> {
        assertEquals("OK", rsp.body().string());
      });

      client.get("/void", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(204, rsp.code());
      });
    });
  }

  @Test
  public void producesAndConsumes() {
    new JoobyRunner(app -> {

      app.renderer(io.jooby.MediaType.json, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("{" + value.toString() + "}").getBytes(StandardCharsets.UTF_8)
      );

      app.renderer(io.jooby.MediaType.xml, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("<" + value.toString() + ">").getBytes(StandardCharsets.UTF_8)
      );

      app.parser(io.jooby.MediaType.json, new MessageDecoder() {
        @Nonnull @Override public Message parse(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return new Message("{" + ctx.body().value() + "}");
        }
      });

      app.parser(xml, new MessageDecoder() {
        @Nonnull @Override public Message parse(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return new Message("<" + ctx.body().value() + ">");
        }
      });

      app.mvc(new ProducesConsumes());

    }).ready(client -> {
      client.header("Accept", "application/json");
      client.get("/produces", rsp -> {
        assertEquals("{MVC}", rsp.body().string());
      });

      client.header("Accept", "application/xml");
      client.get("/produces", rsp -> {
        assertEquals("<MVC>", rsp.body().string());
      });

      client.header("Accept", "text/html");
      client.get("/produces", rsp -> {
        assertEquals(406, rsp.code());
      });

      client.header("Content-Type", "application/json");
      client.get("/consumes", rsp -> {
        assertEquals("{}", rsp.body().string());
      });

      client.header("Content-Type", "application/xml");
      client.get("/consumes", rsp -> {
        assertEquals("<>", rsp.body().string());
      });

      client.header("Content-Type", "text/plain");
      client.get("/consumes", rsp -> {
        assertEquals(415, rsp.code());
      });

      client.get("/consumes", rsp -> {
        assertEquals(415, rsp.code());
      });
    });
  }

  @Test
  public void jaxrs() {
    new JoobyRunner(app -> {

      app.mvc(new JAXRS());

    }).ready(client -> {
      client.get("/jaxrs", rsp -> {
        assertEquals("Got it!", rsp.body().string());
      });
    });
  }

  @Test
  public void noTopLevelPath() {
    new JoobyRunner(app -> {

      app.mvc(new NoTopLevelPath());

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("root", rsp.body().string());
      });

      client.get("/subpath", rsp -> {
        assertEquals("subpath", rsp.body().string());
      });
    });
  }

  @Test
  public void provisioning() {
    new JoobyRunner(app -> {

      app.mvc(new Provisioning());

    }).ready(client -> {
      client.get("/args/ctx", rsp -> {
        assertEquals("/args/ctx", rsp.body().string());
      });

      client.header("Cookie", "jooby.flash=success=OK").get("/args/flash", rsp -> {
        assertEquals("{success=OK}OK", rsp.body().string());
      });

      client.get("/args/sendStatusCode", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(201, rsp.code());
      });

      client.get("/args/file/foo.txt", rsp -> {
        assertEquals("foo.txt", rsp.body().string());
      });

      client.get("/args/int/678", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/args/foo/678/9/3.14/6.66/true", rsp -> {
        assertEquals("/args/foo/678/9/3.14/6.66/true", rsp.body().string());
      });

      client.get("/args/long/678", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/args/float/67.8", rsp -> {
        assertEquals("67.8", rsp.body().string());
      });

      client.get("/args/double/67.8", rsp -> {
        assertEquals("67.8", rsp.body().string());
      });

      client.get("/args/bool/false", rsp -> {
        assertEquals("false", rsp.body().string());
      });
      client.get("/args/str/*", rsp -> {
        assertEquals("*", rsp.body().string());
      });
      client.get("/args/list/*", rsp -> {
        assertEquals("[*]", rsp.body().string());
      });
      client.get("/args/custom/3.14", rsp -> {
        assertEquals("3.14", rsp.body().string());
      });

      client.get("/args/search?q=*", rsp -> {
        assertEquals("*", rsp.body().string());
      });

      client.get("/args/querystring?q=*", rsp -> {
        assertEquals("{q=*}", rsp.body().string());
      });

      client.get("/args/search-opt", rsp -> {
        assertEquals("Optional.empty", rsp.body().string());
      });

      client.header("foo", "bar");
      client.get("/args/header", rsp -> {
        assertEquals("bar", rsp.body().string());
      });

      client.post("/args/form", new FormBody.Builder().add("foo", "ab").build(), rsp -> {
        assertEquals("ab", rsp.body().string());
      });

      client.post("/args/formdata", new FormBody.Builder().add("foo", "ab").build(), rsp -> {
        assertEquals("{foo=ab}", rsp.body().string());
      });

      client.post("/args/multipart", new FormBody.Builder().add("foo", "ab").build(), rsp -> {
        assertEquals("{foo=ab}", rsp.body().string());
      });

      client.post("/args/multipart",
          new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("foo", "...")
              .build(), rsp -> {
            assertEquals("{foo=...}", rsp.body().string());
          });

      client.post("/args/form",
          new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("foo", "...")
              .build(), rsp -> {
            assertEquals("...", rsp.body().string());
          });

      client.header("Cookie", "foo=bar");
      client.get("/args/cookie", rsp -> {
        assertEquals("bar", rsp.body().string());
      });
    });
  }

  @Test
  public void nullinjection() {
    new JoobyRunner(app -> {

      app.mvc(new NullInjection());

      app.error((ctx, cause, statusCode) -> {
        app.getLog().error("{} {}", ctx.getMethod(), ctx.pathString(), cause);
        ctx.setResponseCode(statusCode)
            .send(cause.getMessage());
      });

    }).ready(client -> {
      client.get("/nonnull", rsp -> {
        assertEquals("Unable to provision parameter: 'v: int'", rsp.body().string());
      });
      client.get("/nullok", rsp -> {
        assertEquals("null", rsp.body().string());
      });

      client.get("/nullbean", rsp -> {
        assertEquals(
            "Unable to provision parameter: 'foo: int', require by: constructor examples.NullInjection.QParam(int, java.lang.Integer)",
            rsp.body().string());
      });

      client.get("/nullbean?foo=foo", rsp -> {
        assertEquals(
            "Unable to provision parameter: 'foo: int', require by: constructor examples.NullInjection.QParam(int, java.lang.Integer)",
            rsp.body().string());
      });

      client.get("/nullbean?foo=0&baz=baz", rsp -> {
        assertEquals(
            "Unable to provision parameter: 'baz: int', require by: method examples.NullInjection.QParam.setBaz(int)",
            rsp.body().string());
      });
    });
  }

  @Test
  public void mvcBody() {
    new JoobyRunner(app -> {

      app.install(new JacksonModule());

      app.mvc(new MvcBody());

      app.error((ctx, cause, statusCode) -> {
        app.getLog().error("{} {}", ctx.getMethod(), ctx.pathString(), cause);
        ctx.setResponseCode(statusCode)
            .send(cause.getMessage());
      });

    }).ready(client -> {
      client.header("Content-Type", "text/plain");
      client.post("/body/str", create(MediaType.get("text/plain"), "..."), rsp -> {
        assertEquals("...", rsp.body().string());
      });
      client.header("Content-Type", "text/plain");
      client.post("/body/int", create(MediaType.get("text/plain"), "8"), rsp -> {
        assertEquals("8", rsp.body().string());
      });
      client.post("/body/int", create(MediaType.get("text/plain"), "8x"), rsp -> {
        assertEquals("Unable to provision parameter: 'body: int'", rsp.body().string());
      });
      client.header("Content-Type", "application/json");
      client.post("/body/json", create(MediaType.get("application/json"), "{\"foo\"= \"bar\"}"),
          rsp -> {
            assertEquals(
                "Unable to provision parameter: 'body: java.util.Map<java.lang.String, java.lang.Object>'",
                rsp.body().string());
          });
      client.header("Content-Type", "application/json");
      client.post("/body/json", create(MediaType.get("application/json"), "{\"foo\": \"bar\"}"),
          rsp -> {
            assertEquals("\"{foo=bar}null\"", rsp.body().string());
          });
      client.header("Content-Type", "application/json");
      client.post("/body/json?type=x",
          create(MediaType.get("application/json"), "{\"foo\": \"bar\"}"), rsp -> {
            assertEquals("\"{foo=bar}x\"", rsp.body().string());
          });
    });
  }

  @Test
  public void mvcDispatch() {
    new JoobyRunner(app -> {
      app.executor("single", Executors.newSingleThreadExecutor(r ->
          new Thread(r, "single")
      ));

      app.mvc(new TopDispatch());

    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.get("/", rsp -> {
        String body = rsp.body().string();
        assertTrue(body.startsWith("application"), body);
      });

      client.get("/method", rsp -> {
        assertEquals("single", rsp.body().string());
      });
    });

    LinkedList<String> names = new LinkedList<>(
        Arrays.asList("netty", "application I/O", "application-"));
    new JoobyRunner(app -> {
      app.executor("single", Executors.newSingleThreadExecutor(r ->
          new Thread(r, "single")
      ));

      app.mvc(new LoopDispatch());

    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.get("/", rsp -> {
        String body = rsp.body().string();
        assertTrue(body.startsWith(names.removeFirst()), body);
      });

      client.get("/method", rsp -> {
        assertEquals("single", rsp.body().string());
      });
    });
  }

}
