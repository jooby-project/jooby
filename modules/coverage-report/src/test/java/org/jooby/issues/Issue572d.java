package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue572d extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/572d")));

    get("/", () -> require(Config.class).getString("contextPath"));
  }

  @Test
  public void shouldGetEmptyContextPath() throws Exception {
    request()
        .get("/572d")
        .expect("/572d");
  }

}
