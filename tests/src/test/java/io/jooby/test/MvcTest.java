/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static io.jooby.MediaType.xml;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;

import edu.umd.cs.findbugs.annotations.NonNull;
import examples.*;
import io.jooby.Context;
import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.MessageDecoder;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MvcTest {

  @ServerTest
  public void routerInstance(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new InstanceRouter_());
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    Assertions.assertEquals("some", rsp.body().string());
                  });

              client.post(
                  "/",
                  rsp -> {
                    Assertions.assertEquals("some", rsp.body().string());
                  });

              client.get(
                  "/subpath",
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                  });

              client.delete(
                  "/void",
                  rsp -> {
                    Assertions.assertEquals("", rsp.body().string());
                    Assertions.assertEquals(204, rsp.code());
                  });

              client.get(
                  "/voidwriter",
                  rsp -> {
                    Assertions.assertEquals("writer", rsp.body().string().trim());
                    Assertions.assertEquals(200, rsp.code());
                  });
            });
  }

  @ServerTest
  public void routerImporting(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              Jooby sub = new Jooby();
              sub.mvc(new InstanceRouter_());
              app.mount("/sub", sub);
            })
        .ready(
            client -> {
              client.get(
                  "/sub",
                  rsp -> {
                    Assertions.assertEquals("some", rsp.body().string());
                  });
              client.post(
                  "/sub",
                  rsp -> {
                    Assertions.assertEquals("some", rsp.body().string());
                  });

              client.get(
                  "/sub/subpath",
                  rsp -> {
                    Assertions.assertEquals("OK", rsp.body().string());
                  });

              client.delete(
                  "/sub/void",
                  rsp -> {
                    Assertions.assertEquals("", rsp.body().string());
                    Assertions.assertEquals(204, rsp.code());
                  });

              client.get(
                  "/sub/voidwriter",
                  rsp -> {
                    Assertions.assertEquals("writer", rsp.body().string().trim());
                    Assertions.assertEquals(200, rsp.code());
                  });
            });
  }

  @ServerTest
  public void producesAndConsumes(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.encoder(
                  io.jooby.MediaType.json,
                  (@NonNull Context ctx, @NonNull Object value) ->
                      ctx.getBufferFactory()
                          .wrap(("{" + value + "}").getBytes(StandardCharsets.UTF_8)));

              app.encoder(
                  io.jooby.MediaType.xml,
                  (@NonNull Context ctx, @NonNull Object value) ->
                      ctx.getBufferFactory()
                          .wrap(("<" + value + ">").getBytes(StandardCharsets.UTF_8)));

              app.decoder(
                  io.jooby.MediaType.json,
                  new MessageDecoder() {
                    @NonNull @Override
                    public Message decode(@NonNull Context ctx, @NonNull Type type)
                        throws Exception {
                      return new Message("{" + ctx.body().value("") + "}");
                    }
                  });

              app.decoder(
                  xml,
                  new MessageDecoder() {
                    @NonNull @Override
                    public Message decode(@NonNull Context ctx, @NonNull Type type)
                        throws Exception {
                      return new Message("<" + ctx.body().value("") + ">");
                    }
                  });

              app.mvc(new ProducesConsumes_());
            })
        .ready(
            client -> {
              client.header("Accept", "application/json");
              client.get(
                  "/produces",
                  rsp -> {
                    Assertions.assertEquals("{MVC}", rsp.body().string());
                  });

              client.header("Accept", "application/xml");
              client.get(
                  "/produces",
                  rsp -> {
                    Assertions.assertEquals("<MVC>", rsp.body().string());
                  });

              client.header("Accept", "text/html");
              client.get(
                  "/produces",
                  rsp -> {
                    Assertions.assertEquals(406, rsp.code());
                  });

              client.header("Content-Type", "application/json");
              client.get(
                  "/consumes",
                  rsp -> {
                    Assertions.assertEquals("{}", rsp.body().string());
                  });

              client.header("Content-Type", "application/xml");
              client.get(
                  "/consumes",
                  rsp -> {
                    Assertions.assertEquals("<>", rsp.body().string());
                  });

              client.header("Content-Type", "text/plain");
              client.get(
                  "/consumes",
                  rsp -> {
                    Assertions.assertEquals(415, rsp.code());
                  });

              client.get(
                  "/consumes",
                  rsp -> {
                    Assertions.assertEquals(415, rsp.code());
                  });
            });
  }

  @ServerTest
  public void jaxrs(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new JAXRS_());
            })
        .ready(
            client -> {
              client.get(
                  "/jaxrs",
                  rsp -> {
                    Assertions.assertEquals("Got it!", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void noTopLevelPath(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new NoTopLevelPath_());
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    Assertions.assertEquals("root", rsp.body().string());
                  });

              client.get(
                  "/subpath",
                  rsp -> {
                    Assertions.assertEquals("subpath", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void provisioning(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new Provisioning_());
            })
        .ready(
            client -> {
              client.get(
                  "/args/ctx",
                  rsp -> {
                    Assertions.assertEquals("/args/ctx", rsp.body().string());
                  });

              client
                  .header("Cookie", "jooby.flash=success=OK")
                  .get(
                      "/args/flash",
                      rsp -> {
                        Assertions.assertEquals("{success=OK}OK", rsp.body().string());
                      });

              client.get(
                  "/args/sendStatusCode",
                  rsp -> {
                    Assertions.assertEquals("", rsp.body().string());
                    Assertions.assertEquals(201, rsp.code());
                  });

              client.get(
                  "/args/file/foo.txt",
                  rsp -> {
                    Assertions.assertEquals("foo.txt", rsp.body().string());
                  });

              client.get(
                  "/args/int/678",
                  rsp -> {
                    Assertions.assertEquals("678", rsp.body().string());
                  });

              client.get(
                  "/args/foo/678/9/3.14/6.66/true",
                  rsp -> {
                    Assertions.assertEquals("GET/foo/678/9/3.14/6.66/true", rsp.body().string());
                  });

              client.get(
                  "/args/long/678",
                  rsp -> {
                    Assertions.assertEquals("678", rsp.body().string());
                  });

              client.get(
                  "/args/float/67.8",
                  rsp -> {
                    Assertions.assertEquals("67.8", rsp.body().string());
                  });

              client.get(
                  "/args/double/67.8",
                  rsp -> {
                    Assertions.assertEquals("67.8", rsp.body().string());
                  });

              client.get(
                  "/args/bool/false",
                  rsp -> {
                    Assertions.assertEquals("false", rsp.body().string());
                  });
              client.get(
                  "/args/str/*",
                  rsp -> {
                    Assertions.assertEquals("*", rsp.body().string());
                  });
              client.get(
                  "/args/list/*",
                  rsp -> {
                    Assertions.assertEquals("[*]", rsp.body().string());
                  });
              client.get(
                  "/args/custom/3.14",
                  rsp -> {
                    Assertions.assertEquals("3.14", rsp.body().string());
                  });

              client.get(
                  "/args/search?q=*",
                  rsp -> {
                    Assertions.assertEquals("*", rsp.body().string());
                  });

              client.get(
                  "/args/querystring?q=*",
                  rsp -> {
                    Assertions.assertEquals("{q=*}", rsp.body().string());
                  });

              client.get(
                  "/args/search-opt",
                  rsp -> {
                    Assertions.assertEquals("Optional.empty", rsp.body().string());
                  });

              client.header("foo", "bar");
              client.get(
                  "/args/header",
                  rsp -> {
                    Assertions.assertEquals("bar", rsp.body().string());
                  });

              client.post(
                  "/args/form",
                  new FormBody.Builder().add("foo", "ab").build(),
                  rsp -> {
                    Assertions.assertEquals("ab", rsp.body().string());
                  });

              client.post(
                  "/args/formdata",
                  new FormBody.Builder().add("foo", "ab").build(),
                  rsp -> {
                    Assertions.assertEquals("{foo=ab}", rsp.body().string());
                  });

              client.post(
                  "/args/multipart",
                  new FormBody.Builder().add("foo", "ab").build(),
                  rsp -> {
                    Assertions.assertEquals("{foo=ab}", rsp.body().string());
                  });

              client.post(
                  "/args/multipart",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("foo", "...")
                      .build(),
                  rsp -> {
                    Assertions.assertEquals("{foo=...}", rsp.body().string());
                  });

              client.post(
                  "/args/form",
                  new MultipartBody.Builder()
                      .setType(MultipartBody.FORM)
                      .addFormDataPart("foo", "...")
                      .build(),
                  rsp -> {
                    Assertions.assertEquals("...", rsp.body().string());
                  });

              client.header("Cookie", "foo=bar");
              client.get(
                  "/args/cookie",
                  rsp -> {
                    Assertions.assertEquals("bar", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void nullinjection(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.mvc(new NullInjection_());

              app.error(
                  (ctx, cause, statusCode) -> {
                    app.getLog().info("{} {}", ctx.getMethod(), ctx.getRequestPath());
                    ctx.setResponseCode(statusCode).send(cause.getMessage());
                  });
            })
        .ready(
            client -> {
              //              client.get(
              //                  "/nonnull",
              //                  rsp -> {
              //                    Assertions.assertEquals("Missing value: 'v'",
              // rsp.body().string());
              //                  });
              //              client.get(
              //                  "/nullok",
              //                  rsp -> {
              //                    Assertions.assertEquals("null", rsp.body().string());
              //                  });

              client.get(
                  "/nullbean",
                  rsp -> {
                    Assertions.assertEquals(
                        "Unable to provision parameter: 'foo: int', require by: constructor"
                            + " examples.NullInjection.QParam(int, java.lang.Integer)",
                        rsp.body().string());
                  });

              client.get(
                  "/nullbean?foo=foo",
                  rsp -> {
                    Assertions.assertEquals(
                        "Unable to provision parameter: 'foo: int', require by: constructor"
                            + " examples.NullInjection.QParam(int, java.lang.Integer)",
                        rsp.body().string());
                  });

              client.get(
                  "/nullbean?foo=0&baz=baz",
                  rsp -> {
                    Assertions.assertEquals(
                        "Unable to provision parameter: 'baz: int', require by: method"
                            + " examples.NullInjection.QParam.setBaz(int)",
                        rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void mvcBody(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.mvc(new MvcBody_());

              app.error(
                  (ctx, cause, statusCode) -> {
                    app.getLog()
                        .info(
                            "{} {} {}", ctx.getMethod(), ctx.getRequestPath(), statusCode.value());
                    ctx.setResponseCode(statusCode).send(cause.getMessage());
                  });
            })
        .ready(
            client -> {
              client.header("Content-Type", "application/json");
              client.post(
                  "/body/json",
                  RequestBody.create("{\"foo\": \"bar\"}", MediaType.get("application/json")),
                  rsp -> {
                    Assertions.assertEquals("{foo=bar}null", rsp.body().string());
                  });

              client.header("Content-Type", "text/plain");
              client.post(
                  "/body/str",
                  RequestBody.create("...", MediaType.get("text/plain")),
                  rsp -> {
                    Assertions.assertEquals("...", rsp.body().string());
                  });
              client.header("Content-Type", "text/plain");
              client.post(
                  "/body/int",
                  RequestBody.create("8", MediaType.get("text/plain")),
                  rsp -> {
                    Assertions.assertEquals("8", rsp.body().string());
                  });
              client.post(
                  "/body/int",
                  RequestBody.create("8x", MediaType.get("text/plain")),
                  rsp -> {
                    Assertions.assertEquals(
                        "Cannot convert value: 'body', to: 'int'", rsp.body().string());
                  });
              client.header("Content-Type", "application/json");
              client.post(
                  "/body/json",
                  RequestBody.create("{\"foo\"= \"bar\"}", MediaType.get("application/json")),
                  rsp -> {
                    Assertions.assertEquals(400, rsp.code());
                  });

              client.header("Content-Type", "application/json");
              client.post(
                  "/body/json?type=x",
                  RequestBody.create("{\"foo\": \"bar\"}", MediaType.get("application/json")),
                  rsp -> {
                    Assertions.assertEquals("{foo=bar}x", rsp.body().string());
                  });
            });
  }

  @ServerTest
  public void beanConverter(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              var factory = app.getValueFactory();
              factory.put(MyValue.class, new MyValueBeanConverter());
              app.mvc(new MyValueRouter_());
            })
        .ready(
            client -> {
              client.get(
                  "/myvalue?string=query",
                  rsp -> {
                    Assertions.assertEquals("query", rsp.body().string());
                  });
            });
  }

  @ServerTest(executionMode = ExecutionMode.EVENT_LOOP)
  public void mvcDispatch(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.executor(
                  "single", Executors.newSingleThreadExecutor(r -> new Thread(r, "single")));

              app.mvc(new TopDispatch_());
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    String body = rsp.body().string();
                    Assertions.assertTrue(body.startsWith("worker"), body);
                  });

              client.get(
                  "/method",
                  rsp -> {
                    Assertions.assertEquals("single", rsp.body().string());
                  });
            });
  }

  @ServerTest(executionMode = ExecutionMode.EVENT_LOOP)
  public void mvcLoopDispatch(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.executor(
                  "single", Executors.newSingleThreadExecutor(r -> new Thread(r, "single")));

              app.mvc(new LoopDispatch_());
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    String body = rsp.body().string();
                    Assertions.assertTrue(runner.matchesEventLoopThread(body), body);
                  });

              client.get(
                  "/method",
                  rsp -> {
                    Assertions.assertEquals("single", rsp.body().string());
                  });
            });
  }
}
