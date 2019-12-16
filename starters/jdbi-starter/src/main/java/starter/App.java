package starter;

import io.jooby.Jooby;
import io.jooby.flyway.FlywayModule;
import io.jooby.hikari.HikariModule;
import io.jooby.jdbi.JdbiModule;
import io.jooby.jdbi.TransactionalRequest;
import io.jooby.json.JacksonModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import starter.domain.PetRepository;

public class App extends Jooby {
  {
    /** JSON module: */
    install(new JacksonModule());

    /** DataSource module: */
    install(new HikariModule());

    /** Database migration module: */
    install(new FlywayModule());

    /** Jdbi module: */
    install(new JdbiModule(dataSource -> {
          Jdbi jdbi = Jdbi.create(dataSource);
          jdbi.installPlugin(new SqlObjectPlugin());
          return jdbi;
        }).sqlObjects(PetRepository.class)
    );

    /** Open handle per request: */
    decorator(new TransactionalRequest());

    path("/pets", () -> {
      get("/", ctx -> {
        PetRepository repository = require(PetRepository.class);

        int start = ctx.query("start").intValue(0);
        int max = ctx.query("max").intValue(10);
        return repository.list(start, max);
      });
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
