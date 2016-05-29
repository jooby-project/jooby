package apps;

import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.jooby.mongodb.MongoRx;

import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoCollection;

import rx.schedulers.Schedulers;

public class MongoRxApp extends Jooby {

  {
    use(new Jackson());

    use(new MongoRx("mongodb://localhost/pets.Pet")
        .observableAdapter(observable -> observable.observeOn(Schedulers.io())));

    get("/list", req -> req.require(MongoCollection.class)
        .find());

    get("/db", req -> req.require(MongoClient.class)
        .listDatabaseNames());
  }

  public static void main(final String[] args) throws Throwable {
    run(MongoRxApp::new, args);
  }
}
