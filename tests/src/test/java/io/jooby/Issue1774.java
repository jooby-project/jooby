package io.jooby;

import com.typesafe.config.Config;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1774 {

  @ServerTest
  public void shouldHaveAccessToConf(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/",
          ctx -> Optional.of(ctx.require(Environment.class)).map(e -> "env;").orElse("") + Optional
              .of(ctx.require(Config.class)).map(c -> "conf").orElse(""));
    }).ready(http -> {
      http.get("/", rsp -> {
        assertEquals("env;conf", rsp.body().string());
      });
    });
  }
}
