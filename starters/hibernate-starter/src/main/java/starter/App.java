package starter;

import io.jooby.Jooby;
import io.jooby.hibernate.HibernateModule;
import io.jooby.hibernate.TransactionalRequest;
import io.jooby.hikari.HikariModule;
import io.jooby.json.JacksonModule;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public class App extends Jooby {

  {
    /** JSON: */
    install(new JacksonModule());

    /** Jdbc: */
    install(new HikariModule());

    /**
     * Hibernate:
     */
    install(new HibernateModule(Pet.class));

    /**
     * Insert some data on startup:
     */
    onStarted(() -> {
      EntityManagerFactory factory = require(EntityManagerFactory.class);

      EntityManager em = factory.createEntityManager();

      EntityTransaction trx = em.getTransaction();
      trx.begin();
      em.persist(new Pet("Lala"));
      em.persist(new Pet("Mandy"));
      em.persist(new Pet("Fufy"));
      em.persist(new Pet("Dina"));
      trx.commit();

      em.close();
    });

    /** Open session in view filter: */
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
