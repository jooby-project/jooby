package org.jooby;

import java.util.ArrayList;
import java.util.List;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class EnvOnStartFeature extends ServerFeature {

  {

    List<String> order = new ArrayList<>();

    use((env, conf, binder) -> {
      env.onStart(() -> order.add("m1"));
    });

    use((env, conf, binder) -> {
      env.onStart(() -> order.add("m2"));
    });

    get("/onStartOrder", () -> order);
  }

  @Test
  public void onStartOrder() throws Exception {
    request().get("/onStartOrder").expect("[m1, m2]");
  }
}
