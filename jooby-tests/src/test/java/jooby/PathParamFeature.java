package jooby;

import static org.junit.Assert.assertEquals;
import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
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
      String name = req.param("name").stringValue();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    get("/fancy/:name/:age", (req, resp) -> {
      String name = req.param("name").stringValue();
      int age = req.param("age").intValue();
      resp.send(name + " " + age);
    });

    route(Resource.class);
  }

  @Test
  public void variables() throws Exception {
    assertEquals("edgar 34", Request.Get(uri("vars", "edgar", "34").build()).execute()
        .returnContent().asString());

    assertEquals("edgar 33", Request.Get(uri("r", "vars", "edgar", "33").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void fancy() throws Exception {
    assertEquals("edgar 34", Request.Get(uri("fancy", "edgar", "34").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void notFound() throws Exception {

    assertStatus(HttpStatus.NOT_FOUND,
        () -> Request.Get(uri("vars", "edgar").build()).execute().returnContent().asString());

    assertStatus(HttpStatus.NOT_FOUND,
        () -> Request.Get(uri("vars", "edgar", "1", "2").build()).execute().returnContent()
            .asString());
  }

}
