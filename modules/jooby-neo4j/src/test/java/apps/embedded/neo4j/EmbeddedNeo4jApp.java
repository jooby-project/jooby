package apps.embedded.neo4j;

import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.embedded.neo4j.EmbeddedNeo4j;
import org.jooby.embedded.neo4j.EmbeddedNeo4jSessionStore;

import java.util.concurrent.atomic.AtomicInteger;

public class EmbeddedNeo4jApp extends Jooby {

  {

    conf("embedded/application.conf");

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