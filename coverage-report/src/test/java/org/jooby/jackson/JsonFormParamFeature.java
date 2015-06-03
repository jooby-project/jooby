package org.jooby.jackson;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class JsonFormParamFeature extends ServerFeature {

  public static class User {

    public String name;
  }

  {

    use(new Jackson());

    post("/json/form/param", req ->
        req.param("user").to(User.class, "json"));

  }

  @Test
  public void postParam() throws Exception {
    request()
        .post("/json/form/param")
        .form()
        .add("user", "{\"name\":\"X\"}")
        .expect("{\"name\":\"X\"}");
  }
}
