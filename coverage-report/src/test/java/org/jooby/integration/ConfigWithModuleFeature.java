package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ConfigWithModuleFeature extends ServerFeature {

  {

    get("/", req -> req.require(Config.class).getString("m1.prop") + ":"
        + req.require(Config.class).getString("m1.prop0"));

    use(ConfigFactory.parseResources(getClass(), getClass().getSimpleName() + ".conf")
        .withValue("application.secret", ConfigValueFactory.fromAnyRef("123"))
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));
  }

  @Test
  public void property() throws Exception {
    request()
        .get("/")
        .expect("m1.override:m0");
  }

}
