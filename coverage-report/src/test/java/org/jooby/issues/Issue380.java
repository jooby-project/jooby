package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue380 extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));

    on("dev", () -> {
      get("/", () -> "dev");
    }).orElse(() -> {
      get("/", () -> "prod");
    });
  }

  @Test
  public void orElseEnvPredicate() throws Exception {
    request()
        .get("/")
        .expect("prod");
  }

}
