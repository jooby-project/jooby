package org.jooby.flyway;

import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlywayCommandsFeature extends ServerFeature {

  {
    use(ConfigFactory
        .empty()
        .withValue("flyway.url",
            ConfigValueFactory.fromAnyRef("jdbc:h2:mem:143645810203;DB_CLOSE_DELAY=-1"))
        .withValue(
            "flyway.run",
            ConfigValueFactory.fromAnyRef(Arrays.asList("clean", "baseline", "migrate", "repair",
                "validate", "info")))
        .withValue("flyway.locations", ConfigValueFactory.fromAnyRef("org/jooby/flyway")));

    use(new Flywaydb());

    get("/flyway/info", req -> req.require(Flyway.class).info().current().getDescription());
  }

  @Test
  public void dbmigration() throws Exception {
    request()
        .get("/flyway/info/")
        .expect("<< Flyway Baseline >>");
  }
}
