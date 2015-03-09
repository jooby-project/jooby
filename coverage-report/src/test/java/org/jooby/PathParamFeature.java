package org.jooby;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class PathParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/vars/:name/:age")
    @GET
    public String vars(final String name, final int age) {
      return name + " " + age;
    }

  }

  {

    get("/vars/{name}/{age}", (req, resp) -> {
      String name = req.param("name").value();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    get("/fancy/:name/:age", (req, resp) -> {
      String name = req.param("name").value();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    use(Resource.class);
  }

  @Test
  public void variables() throws Exception {
    request()
        .get("/vars/edgar/34")
        .expect("edgar 34");

    request()
        .get("/r/vars/edgar/33")
        .expect("edgar 33");
  }

  @Test
  public void fancy() throws Exception {
    request()
        .get("/fancy/edgar/34")
        .expect("edgar 34");
  }

  @Test
  public void notFound() throws Exception {

    request()
        .get("/vars/edgar")
        .expect(404);

    request()
        .get("/vars/edgar/1/2")
        .expect(404);
  }

}
