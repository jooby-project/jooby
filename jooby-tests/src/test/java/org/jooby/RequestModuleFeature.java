package org.jooby;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.http.client.fluent.Request;
import org.jooby.Jooby;
import org.jooby.Mode;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

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

    use(new Jooby.Module() {
      @Override
      public void configure(final Mode mode, final Config config, final Binder binder)
          throws Exception {
        Multibinder<org.jooby.Request.Module> m = Multibinder.newSetBinder(binder,
            org.jooby.Request.Module.class);
        m.addBinding().toInstance(
            (b) -> b.bind(RequestScoped.class).toInstance(new RequestScoped()));
      }
    });

    get("/", (req, resp) -> {
      // ask once
        req.getInstance(RequestScoped.class);
        // ask twice
        req.getInstance(RequestScoped.class);
        // get it
        resp.send(req.getInstance(RequestScoped.class));
      });

    use(Resource.class);
  }

  @Test
  public void numberOfInstances() throws Exception {
    assertEquals("1", Request.Get(uri().build()).execute().returnContent().asString());

    assertEquals("2", Request.Get(uri("r").build()).execute().returnContent().asString());
  }

}
