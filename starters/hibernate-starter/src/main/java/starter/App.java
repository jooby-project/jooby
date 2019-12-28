package starter;

import io.jooby.Jooby;
import io.jooby.flyway.FlywayModule;
import io.jooby.hibernate.HibernateModule;
import io.jooby.hibernate.TransactionalRequest;
import io.jooby.hikari.HikariModule;
import io.jooby.json.JacksonModule;

import javax.persistence.EntityManager;

public class App extends Jooby {

  {
    /** JSON: */
    install(new JacksonModule());

    /** Jdbc: */
    install(new HikariModule());

    /** Database migration: */
    install(new FlywayModule());

    /**
     * Hibernate:
     */
    install(new HibernateModule(Pet.class));

    /** Open session in view filter (entitymanager + transaction): */
    decorator(new TransactionalRequest());

    /**
     * Find all via query-dsl:
     */
    get("/pets", ctx -> {
      EntityManager em = require(EntityManager.class);
      return em.createQuery("from Pet", Pet.class)
          .getResultList();
    });

    /**
     * Find by id via entity manager:
     */
    get("/pets/{id:\\d+}", ctx -> {
      int id = ctx.path("id").intValue();
      EntityManager em = require(EntityManager.class);
      return em.find(Pet.class, id);
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
