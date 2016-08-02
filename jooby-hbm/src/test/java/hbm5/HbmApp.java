package hbm5;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.event.spi.EventType;
import org.jooby.Jooby;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.UnitOfWork;
import org.jooby.json.Jackson;

import com.typesafe.config.Config;

public class HbmApp extends Jooby {

  {
    use(new Jackson());

    use(new Hbm("jdbc:h2:./target/hbm")
        .classes(Beer.class)
        .doWith((final BootstrapServiceRegistryBuilder bsrb, final Config conf) -> {
          System.out.println();
          System.out.println(bsrb);
          System.out.println();
        })
        .doWith((final StandardServiceRegistryBuilder ssrb) -> {
          System.out.println(ssrb);
          System.out.println();
        })
        .doWith((final SessionFactory sf) -> {
          System.out.println();
          System.out.println(sf);
        })
        .onEvent(EventType.POST_LOAD, BeerLoaded.class));

    use("*", "*", Hbm.openSessionInView());

    ExecutorService executor = Executors.newSingleThreadExecutor();

    use("/api/beer")
        .post(req -> require(UnitOfWork.class)
            .apply(em -> em.merge(req.body(Beer.class))))
        .post("/async", promise((req, deferred) -> {
          Beer beer = req.body(Beer.class);
          executor.execute(deferred.run(() -> {
            return require(UnitOfWork.class)
                .apply(em -> em.merge(beer));
          }));
        }))
        .get("/q",
            () -> require(Session.class).createQuery("from Beer", Beer.class).getResultList())
        .get("/:id",
            req -> require(Session.class).getReference(Beer.class,
                req.param("id").longValue()).name)
        .get("/async", promise((deferred) -> {
          executor.execute(deferred.run(() -> {
            return require(Session.class).createQuery("from Beer", Beer.class).getResultList();
          }));
        }))
        .get(() -> require(UnitOfWork.class)
            .apply(em -> em.createQuery("from Beer", Beer.class).getResultList()));
  }

  public static void main(final String[] args) throws Throwable {
    run(HbmApp::new, args);
  }
}
