package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.FilterFeature.HttpResponseValidator;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.junit.Test;

public class MvcMethodWithMultipleVerbsFeature extends ServerFeature {

  public static class Resource {
    @GET
    @POST
    @Path("/")
    public String getOrPost(final org.jooby.Request req) {
      return req.route().verb().toString();
    }
  }

  {
    use(Resource.class);
  }

  @Test
  public void get() throws Exception {
    assertEquals("GET", execute(GET(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  @Test
  public void post() throws Exception {
    assertEquals("POST", execute(POST(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Request POST(final URIBuilder uri) throws Exception {
    return Request.Post(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
