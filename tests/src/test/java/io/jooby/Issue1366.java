package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1366 {

  @ServerTest
  public void issue1366(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/", ctx -> {
        throw new IllegalArgumentException(ctx.query("n").value());
      });
    }).ready(client -> {
      client.get("/?n=<script>alert</script>", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
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
            + "<title>Bad Request (400)</title>\n"
            + "<body>\n"
            + "<h1>Bad Request</h1>\n"
            + "<hr>\n"
            + "<h2>message: &lt;script&gt;alert&lt;/script&gt;</h2>\n"
            + "<h2>status code: 400</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
      });

      client.header("Accept", "application/json");
      client.get("/?n=\"..\"", rsp -> {
        assertEquals("{\"message\":\"\\\"..\\\"\",\"statusCode\":400,\"reason\":\"Bad Request\"}", rsp.body().string());
      });
    });
  }
}
