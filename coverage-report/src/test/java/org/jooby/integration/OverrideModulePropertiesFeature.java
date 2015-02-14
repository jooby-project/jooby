package org.jooby.integration;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class OverrideModulePropertiesFeature extends ServerFeature {

  public enum Letter {
    A,
    B;
  }

  public static class M1 implements Jooby.Module {

    @Override
    public void configure(final Env mode, final Config config, final Binder binder) {
    }

    @Override
    public Config config() {
      return ConfigFactory.parseResources(getClass(), getClass().getSimpleName() + ".m1.conf");
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
    use(ConfigFactory.parseResources(getClass(), getClass().getSimpleName() + ".conf"));
    use(new M1());

    use(Resource.class);
  }

  @Test
  public void property() throws Exception {
    request()
        .get("/r/property")
        .expect("m1.1")
        .expect(200);
  }

}
