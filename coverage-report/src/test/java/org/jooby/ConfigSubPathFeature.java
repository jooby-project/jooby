package org.jooby;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ConfigSubPathFeature extends ServerFeature {

  @Path("/subpath")
  public static class Resource {

    private Config config;

    @Inject
    public Resource(@Named("subpath") final Config config) {
      this.config = config;
    }

    @GET
    public Object config() {
      return config.root().unwrapped();
    }
  }

  {

    use(ConfigFactory.empty()
        .withValue("subpath.x", ConfigValueFactory.fromAnyRef("x"))
        .withValue("subpath.y", ConfigValueFactory.fromAnyRef("y")));

    use(Resource.class);

  }

  @Test
  public void subpath() throws Exception {
    request()
        .get("/subpath")
        .expect("{x=x, y=y}");
  }

}
