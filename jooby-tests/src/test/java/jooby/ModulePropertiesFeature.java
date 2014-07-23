package jooby;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import jooby.mvc.GET;
import jooby.mvc.Path;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ModulePropertiesFeature extends ServerFeature {

  public enum Letter {
    A,
    B;
  }

  public static class M1 implements JoobyModule {

    @Override
    public void configure(final Mode mode, final Config config, final Binder binder)
        throws Exception {
    }

    @Override
    public Config config() {
      return ConfigFactory.parseResources("m1.conf");
    }

  }

  @Path("/r")
  public static class Resource {

    private String property;

    @Inject
    public Resource(@Named("m1.prop") final String property) {
      this.property = requireNonNull(property, "The property is required.");
    }

    @GET
    @Path("/property")
    public Object property() {
      return property;
    }
  }

  {
    {

      // don't use application.conf
      use(ConfigFactory.empty());

      use(new M1());

      route(Resource.class);
    }
  }

  @Test
  public void property() throws Exception {
    assertEquals("m1", Request.Get(uri("r", "property").build())
        .execute().returnContent()
        .asString());
  }
}
