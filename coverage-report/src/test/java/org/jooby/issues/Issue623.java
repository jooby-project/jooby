package org.jooby.issues;

import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.jooby.flyway.Flywaydb;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue623 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("flyway.db1.url",
            ConfigValueFactory.fromAnyRef("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1"))
        .withValue("flyway.db1.locations", ConfigValueFactory
            .fromAnyRef(Arrays.asList("i623/fway1")))
        .withValue("flyway.db2.url",
            ConfigValueFactory.fromAnyRef("jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1"))
        .withValue("flyway.db2.locations", ConfigValueFactory
            .fromAnyRef(Arrays.asList("i623/fway2"))));

    use(new Flywaydb("flyway.db1"));
    use(new Flywaydb("flyway.db2"));

    get("/623", req -> req.require(req.param("name").value(), Flyway.class).info().current()
        .getDescription());
  }

  @Test
  public void bootstratp2dbs() throws Exception {
    request()
        .get("/623?name=flyway.db1")
        .expect("fway1");
    request()
        .get("/623?name=flyway.db2")
        .expect("fway2");
  }
}
