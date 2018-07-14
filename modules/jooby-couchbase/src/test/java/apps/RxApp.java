package apps;

import org.jooby.Jooby;
import org.jooby.couchbase.AsyncDatastore;
import org.jooby.couchbase.Couchbase;
import org.jooby.couchbase.N1Q;
import org.jooby.json.Jackson;
import org.jooby.rx.Rx;

import com.couchbase.client.java.view.ViewQuery;

public class RxApp extends Jooby {

  {
    use(new Jackson());

    use(new Rx());

    use(new Couchbase("couchbase://localhost/beers"));

    path("/api/beer", () -> {
      get(req -> {
        return require(AsyncDatastore.class)
            .query(N1Q.from(Beer.class));
      });
      get("/view", req -> {
        return require(AsyncDatastore.class)
            .query(ViewQuery.from("dev_beers", "beers").limit(2));
      });
      post(req -> {
        AsyncDatastore ds = req.require(AsyncDatastore.class);
        Beer beer = req.body().to(Beer.class);
        return ds.upsert(beer);
      });
      get("/:id", req -> {
        return req.require(AsyncDatastore.class).get(Beer.class, req.param("id").longValue());
      });
      delete("/:id", req -> {
        AsyncDatastore ds = req.require(AsyncDatastore.class);
        return ds.remove(Beer.class, req.param("id").value());
      });
    });
  }

  public static void main(final String[] args) throws Throwable {
    run(RxApp::new, args);
  }
}
