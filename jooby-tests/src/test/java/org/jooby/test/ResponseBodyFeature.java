package org.jooby.test;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.FilterFeature.HttpResponseValidator;
import org.junit.Test;

public class ResponseBodyFeature extends ServerFeature {

  public static class Resource {

    @GET
    @Path("/200")
    public Body ok() {
      return Body.ok();
    }

    @GET
    @Path("/200/body")
    public Body okWithBody() {
      return Body.ok("***");
    }

    @GET
    @Path("/204")
    public Body noContent() {
      return Body.noContent();
    }

    @GET
    @Path("/headers")
    public Body headers() {
      return Body.ok().header("x", "y");
    }

  }

  {
    use(Resource.class);
  }

  @Test
  public void ok() throws Exception {
    assertEquals("", execute(GET(uri("/200")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));

    assertEquals("***", execute(GET(uri("/200/body")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void notContent() throws Exception {
    assertEquals(null, execute(GET(uri("/204")), (response) -> {
      assertEquals(204, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void headers() throws Exception {
    assertEquals("", execute(GET(uri("/headers")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
      assertEquals("y", response.getFirstHeader("x").getValue());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    HttpEntity entity = resp.getEntity();
    return entity == null ? null : EntityUtils.toString(entity);
  }
}
