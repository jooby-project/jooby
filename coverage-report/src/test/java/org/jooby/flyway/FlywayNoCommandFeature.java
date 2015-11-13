package org.jooby.flyway;

import java.util.ArrayList;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.jooby.flyway.Flywaydb;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class FlywayNoCommandFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("flyway.url",
            ConfigValueFactory.fromAnyRef("jdbc:h2:mem:1;DB_CLOSE_DELAY=-1"))
        .withValue("flyway.run", ConfigValueFactory.fromAnyRef(new ArrayList<String>()))
        .withValue("flyway.locations", ConfigValueFactory.fromAnyRef("org/jooby/flyway")));

    use(new Flywaydb());

    get("/flyway/info", req -> {
      Flyway flyway = req.require(Flyway.class);
      MigrationInfoService info = flyway.info();
      MigrationInfo current = info.current();
      return current + "";
    });
  }

  @Test
  public void dbmigration() throws Exception {
    request()
        .get("/flyway/info/")
        .expect("null");
  }
}
