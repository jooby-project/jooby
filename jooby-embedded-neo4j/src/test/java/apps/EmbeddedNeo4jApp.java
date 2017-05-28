package apps;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.neo4j.EmbeddedNeo4j;
import org.jooby.neo4j.EmbeddedNeo4jSessionStore;

import java.util.concurrent.atomic.AtomicInteger;

public class EmbeddedNeo4jApp extends Jooby {

  {
    use(ConfigFactory.empty()
          .withValue("neo4j.session.label", ConfigValueFactory.fromAnyRef("sample_app_session"))
          .withValue("session.timeout", ConfigValueFactory.fromAnyRef("1m")));

    use(new EmbeddedNeo4j());

    AtomicInteger inc = new AtomicInteger(0);
    session(EmbeddedNeo4jSessionStore.class);

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

  public static void main(String[] args) {
    run(EmbeddedNeo4jApp::new, args);
  }
}