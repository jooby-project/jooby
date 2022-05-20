package io.jooby.i2572;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2572 {

  private static class App2572 extends Jooby {
    Map<String, Object> state = new LinkedHashMap<>();

    {
      install(new JacksonModule());

      get("/2572/state", ctx -> state);

      decorator(next -> ctx -> {
        state.put("caller", Thread.currentThread().getName());
        ctx.onComplete(context -> {
          state.put("onComplete", Thread.currentThread().getName());
        });
        return next.apply(ctx);
      });

      get("/2572/init", ctx -> "Initialized");
    }
  }

  @ServerTest(executionMode = {ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER})
  public void onCompleteShouldRunOnCallerThread(ServerTestRunner runner) {
    runner.use(App2572::new).ready(http -> {
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
