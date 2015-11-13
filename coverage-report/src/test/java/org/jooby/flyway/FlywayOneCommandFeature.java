package org.jooby.flyway;

import org.flywaydb.core.Flyway;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlywayOneCommandFeature extends ServerFeature {

  {
    use(ConfigFactory
        .empty()
        .withValue("flyway.url",
            ConfigValueFactory.fromAnyRef("jdbc:h2:mem:14368102203;DB_CLOSE_DELAY=-1"))
        .withValue("flyway.run", ConfigValueFactory.fromAnyRef("migrate"))
        .withValue("flyway.locations", ConfigValueFactory.fromAnyRef("org/jooby/flyway")));

    use(new Flywaydb());

    get("/flyway/info", req -> req.require(Flyway.class).info().current().getDescription());
  }

  @Test
  public void dbmigration() throws Exception {
    request()
        .get("/flyway/info/")
        .expect("Schema3");
  }
}
