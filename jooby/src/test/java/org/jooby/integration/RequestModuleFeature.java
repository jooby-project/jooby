package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.client.fluent.Request;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.multibindings.Multibinder;

public class RequestModuleFeature extends ServerFeature {

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

  @Path("/r")
  public static class Resource {

    private RequestScoped requestScoped;

    @Inject
    public Resource(final RequestScoped requestScoped) {
      this.requestScoped = requestScoped;
    }

    @GET
    public String m() {
      return requestScoped.toString();
    }

  }

  {

    use((mode, config, binder) -> {
      Multibinder<org.jooby.Request.Module> rmb = Multibinder.newSetBinder(binder,
          org.jooby.Request.Module.class);
      rmb.addBinding().toInstance(b -> b.bind(RequestScoped.class).in(Singleton.class));
    });

    get("/", (req, resp) -> {
      // ask once
        req.require(RequestScoped.class);
        // ask twice
        req.require(RequestScoped.class);
        // get it
        resp.send(req.require(RequestScoped.class));
      });

    use(Resource.class);
  }

  @Test
  public void numberOfInstances() throws Exception {
    assertEquals("1", Request.Get(uri().build()).execute().returnContent().asString());

    assertEquals("2", Request.Get(uri("r").build()).execute().returnContent().asString());
  }

}
