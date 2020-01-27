package starter;

import io.ebean.Database;
import io.jooby.Jooby;
import io.jooby.ebean.EbeanModule;
import io.jooby.ebean.TransactionalRequest;
import io.jooby.flyway.FlywayModule;
import io.jooby.hikari.HikariModule;
import io.jooby.json.JacksonModule;
import starter.query.QPet;

public class App extends Jooby {

  {
    /** JSON: */
    install(new JacksonModule());

    /** Jdbc: */
    install(new HikariModule());

    /** Database migration: */
    install(new FlywayModule());
    /**
     * Ebean:
     */
    install(new EbeanModule());

    decorator(new TransactionalRequest());

    /**
     * Find all via query-dsl:
     */
    get("/pets", ctx -> {
      Database db = require(Database.class);
      return db.createQuery(Pet.class).findList();
    });

    /**
     * Find by id via entity manager:
     */
    get("/pets/{id:\\d+}", ctx -> {
      int id = ctx.path("id").intValue();
      return new QPet().id.eq(id).findOne();
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
