package org.jooby.gson;

import org.jooby.MediaType;
import org.jooby.json.Gzon;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class GsonFormParamFeature extends ServerFeature {

  public static class User {

    public String name;
  }

  {

    use(new Gzon());

    post("/json/form/param", req ->
        req.param("user").to(User.class, MediaType.json));

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
