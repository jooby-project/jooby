package org.jooby.flyway;

import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.jooby.jdbc.Jdbc;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlywayFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("flyway.locations", ConfigValueFactory
            .fromAnyRef(Arrays.asList("org/jooby/flyway"))));

    use(new Jdbc("jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1"));

    use(new Flywaydb("foo"));

    get("/flyway/info", req -> req.require(Flyway.class).info().current().getDescription());
  }

  @Test
  public void dbmigration() throws Exception {
    request()
        .get("/flyway/info/")
        .expect("Schema3");
  }
}
