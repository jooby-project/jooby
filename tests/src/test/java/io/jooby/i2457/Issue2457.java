package io.jooby.i2457;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.guice.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2457 {

  @ServerTest
  public void shouldFindServiceOnMountedApp(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new GuiceModule());

      app.mvc(HealthController2457.class);

      app.mount("/api/v1", new ControllersAppV12457());
      app.mount("/api/v2", new ControllersAppV22457());

    }).ready(http -> {
      http.get("/healthcheck", rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("{status=Ok, welcome=[API healthcheck] Welcome Jooby!}", rsp.body().string());
      });

      http.get("/api/v1/welcome", rsp -> {
            assertEquals(200, rsp.code());
            assertEquals("[API v1] Welcome Jooby!", rsp.body().string());
          });

      http.get("/api/v2/welcome", rsp -> {
            assertEquals(200, rsp.code());
            assertEquals("[API v2] Welcome Jooby!", rsp.body().string());
          });
    });
  }
}
