package apps;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.mongodb.MongoSessionStore;
import org.jooby.mongodb.Mongodb;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class MongodbApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mongodb://localhost/mongodbapp"))
        .withValue("session.timeout", ConfigValueFactory.fromAnyRef("2m")));

    use(new Mongodb());

    AtomicInteger inc = new AtomicInteger(0);
    session(MongoSessionStore.class);

    get("/", req -> {
      Session session = req.ifSession().orElseGet(() -> {
        Session newSession = req.session();
        int next = newSession.get("inc").intValue(inc.getAndIncrement());
        newSession.set("inc", next);
        return newSession;
      });
      return session.get("inc");
    });

  }

  public static void main(final String[] args) {
    run(MongodbApp::new, args);
  }
}
