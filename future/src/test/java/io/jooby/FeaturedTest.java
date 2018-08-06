package io.jooby;

import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.test.JoobyRunner;
import io.jooby.utow.Utow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeaturedTest {

  @Test
  public void sayHiFromIO() {
    new JoobyRunner(app -> {
      app.mode(Mode.IO);

      app.get("/", ctx -> "Hello World!");

      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("12", rsp.header("content-length"));
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("12", rsp.header("content-length"));
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals("0", rsp.header("content-length"));
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>\n"
            + "Not Found (404) Not Found\n"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr><h2>message: Not Found (404)</h2>\n"
            + "<h2>status: Not Found (404)</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals("609", rsp.header("content-length"));
      });
    }, new Netty(), new Utow(), new Jetty());

  }

  @Test
  public void sayHiFromWorker() {
    new JoobyRunner(app -> {
      app.mode(Mode.WORKER);

      app.get("/", ctx -> "Hello World!");

      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).ready(client -> {
      client.get("/?foo=bar", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("12", rsp.header("content-length"));
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("12", rsp.header("content-length"));
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals("0", rsp.header("content-length"));
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>\n"
            + "Not Found (404) Not Found\n"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr><h2>message: Not Found (404)</h2>\n"
            + "<h2>status: Not Found (404)</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals("609", rsp.header("content-length"));
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void rawPath() {
    new JoobyRunner(app -> {

      app.get("/{code}", ctx -> ctx.path());

    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("4", rsp.header("content-length"));
      });

      client.get("/a+b", rsp -> {
        assertEquals("/a+b", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("4", rsp.header("content-length"));
      });

      client.get("/%2F", rsp -> {
        assertEquals("/%2F", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals("4", rsp.header("content-length"));
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void httpMethods() {
    new JoobyRunner(app -> {

      app.get("/", ctx -> ctx.method());

      app.post("/", ctx -> ctx.method());

      app.put("/", ctx -> ctx.method());

      app.delete("/", ctx -> ctx.method());

      app.patch("/", ctx -> ctx.method());

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("GET", rsp.body().string());
      });

      client.post("/", rsp -> {
        assertEquals("POST", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.delete("/", rsp -> {
        assertEquals("DELETE", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.patch("/", rsp -> {
        assertEquals("PATCH", rsp.body().string());
      });

    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void pathVarible() {
    new JoobyRunner(app -> {
      app.get("/articles/{id}", ctx -> ctx.param("id").intValue());

      app.get("/articles/*", ctx -> ctx.param("*").value());

      app.get("/file/*path", ctx -> ctx.param("path").value());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.param("nid").intValue());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.param("zid").intValue());

      app.get("/file/{file}.json", ctx -> ctx.param("file").value() + ".JSON");

      app.get("/file/{file}.{ext}",
          ctx -> ctx.param("file").value() + "." + ctx.param("ext").value());

      app.get("/profile/{pid}", ctx -> ctx.param("pid").value());

      app.get("/profile/me", ctx -> "me!");

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("tail/match", rsp.body().string());
      });

      client.get("/file/js/index.js", rsp -> {
        assertEquals("js/index.js", rsp.body().string());
      });

      client.get("/regex/678", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678/edit", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678d", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/file/foo.js", rsp -> {
        assertEquals("foo.js", rsp.body().string());
      });

      client.get("/file/foo.json", rsp -> {
        assertEquals("foo.JSON", rsp.body().string());
      });

      client.get("/profile/me", rsp -> {
        assertEquals("me!", rsp.body().string());
      });

      client.get("/profile/edgar", rsp -> {
        assertEquals("edgar", rsp.body().string());
      });
    }, new Netty(), new Utow(), new Jetty());
  }

  @Test
  public void filter() {
    new JoobyRunner(app -> {

      app.mode(Mode.IO);

      app.before(ctx -> {
        StringBuilder buff = new StringBuilder();
        buff.append("before1:" + ctx.isInIoThread()).append(";");
        ctx.set("buff", buff);
      });

      app.after((ctx, value) -> {
        StringBuilder buff = (StringBuilder) value;
        buff.append("after1:" + ctx.isInIoThread()).append(";");
        return buff;
      });

      app.dispatch(() -> {
        app.before(ctx -> {
          StringBuilder buff = ctx.get("buff");
          buff.append("before2:" + ctx.isInIoThread()).append(";");
        });

        app.after((ctx, value) -> {
          StringBuilder buff = ctx.get("buff");
          buff.append(value).append(";");
          buff.append("after2:" + ctx.isInIoThread()).append(";");
          return buff;
        });

        app.get("/", ctx -> "result:" + ctx.isInIoThread());
      });

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("before1:true;before2:false;result:false;after2:false;after1:false;", rsp.body().string());
      });
    }, new Netty(), new Utow()/* No Jetty bc always use a worker thread */);
  }
}
