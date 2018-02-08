package jdbc;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Jooby;
import org.jooby.jdbc.Jdbc;

public class Log4jdbcApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db.url", ConfigValueFactory
            .fromAnyRef("jdbc:log4jdbc:mysql://localhost/log4jdbc"))
        .withValue("db.user", ConfigValueFactory.fromAnyRef("root"))
        .withValue("db.password", ConfigValueFactory.fromAnyRef("")));

    use(new Jdbc());
  }

  public static void main(final String[] args) {
    run(Log4jdbcApp::new, args);
  }

}
