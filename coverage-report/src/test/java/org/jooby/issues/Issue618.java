package org.jooby.issues;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Issue618 extends ServerFeature {

  {
    use(new Jackson().raw());

    get("/618", () -> "{\"raw\":\"json\"}");

    get("/618/obj", () -> ImmutableMap.of("raw", "json"));
  }

  @Test
  public void rawJsonString() throws Exception {
    request()
        .get("/618")
        .expect("{\"raw\":\"json\"}")
        .header("content-type", "application/json;charset=utf-8");

    request()
        .get("/618/obj")
        .expect("{\"raw\":\"json\"}")
        .header("content-type", "application/json;charset=utf-8");
  }
}
