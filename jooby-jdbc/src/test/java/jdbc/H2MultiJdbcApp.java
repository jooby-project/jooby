package jdbc;

import org.jooby.Jooby;
import org.jooby.jdbc.Jdbc;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class H2MultiJdbcApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db.spock.url", ConfigValueFactory
            .fromAnyRef("jdbc:h2:mem:spock;DB_CLOSE_DELAY=-1"))
        .withValue("db.spock.user", ConfigValueFactory.fromAnyRef("sa"))
        .withValue("db.spock.password", ConfigValueFactory.fromAnyRef(""))
        .withValue("db.spock.hikari.maximumPoolSize", ConfigValueFactory.fromAnyRef(15))
        .withValue("db.spock.hikari.autoCommit", ConfigValueFactory.fromAnyRef(true))
        .withValue("db.vulcan.url", ConfigValueFactory
            .fromAnyRef("jdbc:h2:mem:vulcan;DB_CLOSE_DELAY=-1"))
        .withValue("db.vulcan.user", ConfigValueFactory.fromAnyRef("sa"))
        .withValue("db.vulcan.password", ConfigValueFactory.fromAnyRef(""))
        .withValue("db.vulcan.hikari.maximumPoolSize", ConfigValueFactory.fromAnyRef(25))
        .withValue("db.vulcan.hikari.autoCommit", ConfigValueFactory.fromAnyRef(true)));

    use(new Jdbc("db.spock"));
    use(new Jdbc("db.vulcan"));
  }

  public static void main(final String[] args) {
    run(H2MultiJdbcApp::new, args);
  }

}
