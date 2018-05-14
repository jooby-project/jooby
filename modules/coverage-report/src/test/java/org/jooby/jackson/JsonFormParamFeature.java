package org.jooby.jackson;

import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.TypeLiteral;

public class JsonFormParamFeature extends ServerFeature {

  public static class User {

    public String name;
  }

  {

    use(new Jackson());

    post("/json/form/param", req ->
        req.param("user").to(User.class, "json"));

    post("/json/form/param/typeliteral", req ->
        req.param("user").to(TypeLiteral.get(User.class), "json"));

  }

  @Test
  public void postParam() throws Exception {
    request()
        .post("/json/form/param")
        .form()
        .add("user", "{\"name\":\"X\"}")
        .expect("{\"name\":\"X\"}");

    request()
        .post("/json/form/param/typeliteral")
        .form()
        .add("user", "{\"name\":\"X\"}")
        .expect("{\"name\":\"X\"}");
  }
}
