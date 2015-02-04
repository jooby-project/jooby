package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.http.client.fluent.Request;
import org.jooby.RequestScoped;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestScopedFeature extends ServerFeature {

  @RequestScoped
  public static class RequestScopedObject {

    private static int instances = 0;

    {
      instances += 1;
    }

    @Override
    public String toString() {
      return "" + instances;
    }
  }

  @Path("/r")
  public static class DefScope {

    static int instances = 0;

    static {
      instances += 1;
    }

    private RequestScopedObject requestScoped;

    @Inject
    public DefScope(final RequestScopedObject requestScoped) {
      this.requestScoped = requestScoped;
    }

    @GET
    public String m() {
      return "d" + instances + ":" + requestScoped.toString();
    }

  }

  @Path("/s")
  @Singleton
  public static class SingletonScope {

    private Provider<RequestScopedObject> requestScoped;

    static int instances = 0;

    static {
      instances += 1;
    }

    @Inject
    public SingletonScope(final Provider<RequestScopedObject> requestScoped) {
      this.requestScoped = requestScoped;
    }

    @GET
    public String m() {
      return "s" + instances + ":" + requestScoped.get().toString();
    }

  }

  {

    get("/", (req, resp) -> {
      // ask once
        req.require(RequestScopedObject.class);
        // ask twice
        req.require(RequestScopedObject.class);
        // get it
        resp.send(req.require(RequestScopedObject.class));
      });

    use(DefScope.class);

    use(SingletonScope.class);
  }

  @Test
  public void numberOfInstances() throws Exception {
    assertEquals("1", Request.Get(uri().build()).execute().returnContent().asString());

    assertEquals("d1:2", Request.Get(uri("r").build()).execute().returnContent().asString());

    assertEquals("d1:3", Request.Get(uri("r").build()).execute().returnContent().asString());

    assertEquals("s1:4", Request.Get(uri("s").build()).execute().returnContent().asString());

    assertEquals("s1:5", Request.Get(uri("s").build()).execute().returnContent().asString());
  }

}
