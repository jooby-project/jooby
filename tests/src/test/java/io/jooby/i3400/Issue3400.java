package io.jooby.i3400;

import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3400 {

  static class AppA extends Jooby {
    {
        post("/pets", ctx -> ctx.body(Pet.class));
    }
  }

  @ServerTest
  public void shouldShareDecodersOnMountedResources(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.mount(new AppA());
            })
        .ready(
            http -> {
                http.post(
                "/pets",
                  RequestBody.create("{\"id\": 1, \"name\": \"Cheddar\"}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals("{\"id\":1,\"name\":\"Cheddar\"}", rsp.body().string());
                    assertEquals("application/json;charset=UTF-8", rsp.header("Content-Type"));
                  });
            });
  }
}
