package morphia;

import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.jooby.mongodb.Monphia;
import org.mongodb.morphia.Datastore;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class MorphiaApp extends Jooby {

  {
    use(new Jackson());

    use(ConfigFactory.empty().withValue("db",
        ConfigValueFactory.fromAnyRef("mongodb://localhost/mydb"))
        .withValue("db1",
            ConfigValueFactory.fromAnyRef("mongodb://localhost/demo")));

    use(new Monphia());

    use(new Monphia("db1"));

    path("/api/beer", () -> {
      get(req -> {
        Datastore ds = req.require(Datastore.class);
        return ds.createQuery(Beer.class).asList();
      });
      post(req -> {
        Beer beer = req.body().to(Beer.class);
        Datastore ds = req.require(Datastore.class);
        ds.save(beer);
        return beer;
      });
    });
  }

  public static void main(final String[] args) throws Throwable {
    run(MorphiaApp::new, "server.join=false");
  }
}
