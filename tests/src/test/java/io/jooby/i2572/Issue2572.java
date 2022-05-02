package io.jooby.i2572;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import io.jooby.ExecutionMode;
import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2572 {
  @ServerTest(executionMode = {ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER})
  public void onCompleteShouldRunOnCallerThread(ServerTestRunner runner) {
    runner.define(app -> {
      Map<String, Object> state = new LinkedHashMap<>();

      app.install(new JacksonModule());

      app.get("/2572/state", ctx -> state);

      app.decorator(next -> ctx -> {
        state.put("caller", Thread.currentThread().getName());
        ctx.onComplete(context -> {
          state.put("onComplete", Thread.currentThread().getName());
        });
        return next.apply(ctx);
      });

      app.get("/2572/init", ctx -> "Initialized");

    }).ready(http -> {
      http.get("/2572/init", rsp -> {
        assertEquals("Initialized", rsp.body().string());
      });

      http.get("/2572/state", rsp -> {
        JSONObject json = new JSONObject(rsp.body().string());
        assertEquals(json.get("caller"), json.get("onComplete"));
      });
    });
  }
}
