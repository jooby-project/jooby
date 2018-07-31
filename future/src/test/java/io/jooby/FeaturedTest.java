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

}
