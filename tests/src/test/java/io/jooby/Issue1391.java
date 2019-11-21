package io.jooby;

import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1391 {

  @ServerTest
  public void issue1391(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new JacksonModule());
      app.mvc(new Controller1391());

    }).ready(client -> {
      client.post("/1391", RequestBody.create("[{\"name\" : \"1392\"}]", MediaType.get("application/json")), rsp -> {
        assertEquals("[{\"name\":\"1392\"}]", rsp.body().string());
      });
    });
  }
}
