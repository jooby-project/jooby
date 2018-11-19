package org.jooby.issues;

import org.jooby.json.Gzon;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Issue1269 extends ServerFeature {

  {
    use(new Gzon());

    get("/1269", () -> "{\"name\":\"Falco\",\"age\":4,\"bitable\":false}");

    get("/1269/obj", () -> ImmutableMap.of("name", "Falco","age",4,"bitable",false));
  }

  @Test
  public void rawJsonString() throws Exception {
    request()
        .get("/1269")
        .expect("{\"name\":\"Falco\",\"age\":4,\"bitable\":false}")
        .header("content-type", "application/json;charset=utf-8");

    request()
        .get("/1269/obj")
        .expect("{\"name\":\"Falco\",\"age\":4,\"bitable\":false}")
        .header("content-type", "application/json;charset=utf-8");
  }
}
