package org.jooby.integration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
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
    request()
        .get("/s")
        .expect("1=1");

    request()
        .get("/s")
        .expect("1=2");

    request()
        .get("/s")
        .expect("1=3");

  }

  @Test
  public void proto() throws Exception {
    request()
        .get("/p")
        .expect("1=4");

    request()
        .get("/p")
        .expect("2=5");

    request()
        .get("/p")
        .expect("3=6");

  }

}
