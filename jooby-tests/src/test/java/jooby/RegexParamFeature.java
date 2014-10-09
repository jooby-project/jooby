package jooby;

import static org.junit.Assert.assertEquals;
import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class RegexParamFeature extends ServerFeature {

  @Path("/r")
  public static class Resource {

    @Path("/regex/{id:\\d+}")
    @GET
    public Object regex(final int id) {
      return id;
    }

  }

  {
    get("/regex/{id:\\d+}", (req, resp) -> {
      int id = req.param("id").intValue();
      resp.send(id);
    });

    use(Resource.class);
  }

  @Test
  public void regex() throws Exception {
    assertEquals("678", Request.Get(uri("regex", "678").build()).execute().returnContent()
        .asString());

    assertEquals("678", Request.Get(uri("r", "regex", "678").build()).execute().returnContent()
        .asString());
  }

  @Test
  public void notFound() throws Exception {

    assertStatus(Response.Status.NOT_FOUND,
        () -> Request.Get(uri("r", "regex", "678x").build()).execute().returnContent().asString());

  }

}
