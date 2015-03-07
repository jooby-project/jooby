package org.jooby.integration;

import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class FormParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/form")
    @POST
    public String text(final String name, final int age) {
      return name + " " + age;
    }

  }

  {
    post("/form", (req, resp) -> {
      String name = req.param("name").value();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    use(Resource.class);
  }

  @Test
  public void form() throws Exception {
    request()
        .post("/form")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("edgar 34");

    request()
        .post("/r/form")
        .form()
        .add("name", "edgar")
        .add("age", 34)
        .expect("edgar 34");

  }

}
