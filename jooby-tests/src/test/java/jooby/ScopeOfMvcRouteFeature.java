package jooby;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class ScopeOfMvcRouteFeature extends ServerFeature {

  public static class RequestScoped {

    private static int instances = 0;

    {
      instances += 1;
    }

    @Override
    public String toString() {
      return "" + instances;
    }
  }

  @Path("/s")
  @Singleton
  public static class Single {

    private static int instances = 0;

    private Provider<RequestScoped> requestScoped;

    @Inject
    public Single(final Provider<RequestScoped> requestScoped) {
      instances += 1;
      this.requestScoped = requestScoped;
    }

    @GET
    public Object m() {
      return instances + "=" + requestScoped.get();
    }

  }

  @Path("/p")
  public static class Proto {

    private static int instances = 0;
    private RequestScoped requestScoped;

    @Inject
    public Proto(final RequestScoped requestScoped) {
      instances += 1;
      this.requestScoped = requestScoped;
    }

    @GET
    public Object m() {
      return instances + "=" + requestScoped.toString();
    }

  }

  {

    use(Single.class);

    use(Proto.class);
  }

  @Test
  public void singleton() throws Exception {
    assertEquals("1=1", Request.Get(uri("s").build()).execute().returnContent().asString());

    assertEquals("1=2", Request.Get(uri("s").build()).execute().returnContent().asString());

    assertEquals("1=3", Request.Get(uri("s").build()).execute().returnContent().asString());
  }

  @Test
  public void proto() throws Exception {
    assertEquals("1=4", Request.Get(uri("p").build()).execute().returnContent().asString());

    assertEquals("2=5", Request.Get(uri("p").build()).execute().returnContent().asString());

    assertEquals("3=6", Request.Get(uri("p").build()).execute().returnContent().asString());
  }

}
