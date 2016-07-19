package cassandra;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.cassandra.Cassandra;
import org.jooby.cassandra.CassandraSessionStore;
import org.jooby.cassandra.Datastore;
import org.jooby.json.Jackson;

public class CassandraApp extends Jooby {

  {
    use(new Jackson());

    use(new Cassandra("cassandra://localhost/beers")
        .accesor(BeerRepo.class));

    session(CassandraSessionStore.class);

    /** Session */
    AtomicInteger inc = new AtomicInteger(0);
    get("/", req -> {
      return req.ifSession().orElseGet(() -> {
        return req.session()
            .set("foo", inc.incrementAndGet());
      }).attributes();
    });

    get("/delete", req -> {
      req.ifSession().ifPresent(session -> {
        session.destroy();
      });
      return "session destroyed";
    });

    get("/:name", req -> {
      return req.ifSession()
          .map(session -> session.get(req.param("name").value()).value())
          .orElse("<missing>");
    });

    /** CRUD */
    use("/api/beer")
        .post(req -> {
          Datastore ds = req.require(Datastore.class);
          Beer beer = req.body().to(Beer.class);
          ds.saveAsync(beer);
          return beer;
        })
        .get("/:id", req -> {
          Datastore ds = req.require(Datastore.class);
          return ds.getAsync(Beer.class, req.param("id").value());
        })
        .get(req -> {
          Datastore ds = req.require(Datastore.class);
          return ds.queryAsync(Beer.class, "select * from beer");
        })
        .delete("/:id", req -> {
          Datastore ds = req.require(Datastore.class);
          ds.deleteAsync(Beer.class, req.param("id").value());
          return Results.noContent();
        });
  }

  public static void main(final String[] args) throws Throwable {
    run(CassandraApp::new, args);
  }
}
