package org.jooby;

import java.util.ArrayList;
import java.util.List;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EnvOnStart2Feature extends ServerFeature {

  {

    List<String> order = new ArrayList<>();

    onStart(() -> order.add("a"));

    use((env, conf, binder) -> {
      env.onStart(() -> order.add("m1"));
    });

    use((env, conf, binder) -> {
      env.onStart(() -> order.add("m2"));
    });

    onStart(() -> order.add("b"));

    get("/onStartOrder", () -> order);
  }

  @Test
  public void onStartOrder() throws Exception {
    request().get("/onStartOrder").expect("[m1, m2, a, b]");
  }
}
