package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseHandlerTest {
  @Test
  public void responseHandlerShouldExecuteAfterDecorator() {
    new JoobyRunner(app -> {
      app.after((ctx, result) -> "<" + result + ">");
      app.after((ctx, result) -> "{" + result + "}");
      app.get("/", ctx -> "OK");
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("<{OK}>", rsp.body().string());
      });
    });
  }
}
