package org.jooby.issues;

import javax.sql.DataSource;

import org.jooby.jdbc.Jdbc;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue622 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db.spock.url", ConfigValueFactory
            .fromAnyRef("jdbc:h2:mem:spock;DB_CLOSE_DELAY=-1"))
        .withValue("db.spock.user", ConfigValueFactory.fromAnyRef("sa"))
        .withValue("db.spock.password", ConfigValueFactory.fromAnyRef(""))
        .withValue("db.spock.hikari.maximumPoolSize", ConfigValueFactory.fromAnyRef(100))
        .withValue("db.spock.hikari.autoCommit", ConfigValueFactory.fromAnyRef(true))
        .withValue("db.vulcan.url", ConfigValueFactory
            .fromAnyRef("jdbc:h2:mem:vulcan;DB_CLOSE_DELAY=-1"))
        .withValue("db.vulcan.user", ConfigValueFactory.fromAnyRef("sa"))
        .withValue("db.vulcan.password", ConfigValueFactory.fromAnyRef(""))
        .withValue("db.vulcan.hikari.maximumPoolSize", ConfigValueFactory.fromAnyRef(50))
        .withValue("db.vulcan.hikari.autoCommit", ConfigValueFactory.fromAnyRef(true)));

    use(new Jdbc("db.spock"));
    use(new Jdbc("db.vulcan"));

    get("/622", () -> {
      require("spock", DataSource.class);
      require("vulcan", DataSource.class);
      return "OK";
    });
  }

  @Test
  public void bootstrap2DbsWithCustomSetup() throws Exception {
    request()
        .get("/622")
        .expect("OK");
  }
}
