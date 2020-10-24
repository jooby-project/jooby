package io.jooby.i2069;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.i2068.Bean2068;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2069 {

  @ServerTest
  public void shouldSupportCollectionAsQueryParameter(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/i2069", ctx -> {
        Bean2069 bean = ctx.query(Bean2069.class);
        return bean.getId();
      });
    }).ready(http -> {
      http.get("/i2069?id=foo.id", rsp -> {
        assertEquals("[foo.id]", rsp.body().string());
      });
    });
  }

}
