package io.jooby.i2068;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2068 {

  @ServerTest
  public void shouldFavorEmptyConstructor(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/i2068", ctx -> {
        Bean2068 bean = ctx.query(Bean2068.class);
        return bean.getName();
      });
    }).ready(http -> {
      http.get("/i2068?name=foo", rsp -> {
        assertEquals("foo", rsp.body().string());
      });
    });
  }

}
