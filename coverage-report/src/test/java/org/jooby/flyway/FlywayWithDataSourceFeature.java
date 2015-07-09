package org.jooby.flyway;

import org.flywaydb.core.Flyway;
import org.jooby.flyway.Flywaydb;
import org.jooby.jdbc.Jdbc;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlywayWithDataSourceFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("flyway.locations", ConfigValueFactory.fromAnyRef("org/jooby/flyway")));

    use(new Jdbc());

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
