package apps;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.couchbase.Couchbase;
import org.jooby.couchbase.CouchbaseSessionStore;
import org.jooby.couchbase.Datastore;
import org.jooby.couchbase.N1Q;
import org.jooby.json.Jackson;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.view.ViewQuery;

public class App extends Jooby {

  {
    use(new Jackson());

    use(new Couchbase("couchbase://localhost/beers"));

    session(CouchbaseSessionStore.class);

    onStart(r -> {
      Bucket bucket = r.require(Bucket.class);
      bucket.query(N1qlQuery.simple(Index.createPrimaryIndex().on("beers")));
    });

    AtomicInteger inc = new AtomicInteger(0);
    get("/", req -> {
      Session session = req.session();
      session.set("foo", inc.incrementAndGet());

      return session.attributes();
    });

    get("/:name", req -> {
      Session session = req.session();
      return session.get(req.param("name").value()).value();
    });

    use("/api/beer")
        .get(req -> {
          return require(Datastore.class)
              .query(N1Q.from(Beer.class));
        })
        .get("/view", req -> {
          return require(Datastore.class)
              .query(ViewQuery.from("dev_beers", "beers").limit(2));
        })
        .post(req -> {
          Datastore ds = req.require(Datastore.class);
          Beer beer = req.body().to(Beer.class);
          Beer b = ds.upsert()
              .execute(beer);
          return b;
        })
        .get("/:id", req -> {
          Beer beer = req.require(Datastore.class).get(Beer.class, req.param("id").longValue());
          return beer;
        })
        .get("/:id/exists", req -> {
          return req.require(Datastore.class).exists(Beer.class, req.param("id").longValue());
        })
        .delete("/:id", req -> {
          Datastore ds = req.require(Datastore.class);
          Beer beer = ds.get(Beer.class, req.param("id").value());
          return ds.remove(beer);
        });

  }

  public static void main(final String[] args) throws Throwable {
    run(App::new, args);
  }
}
