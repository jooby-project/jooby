package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.neo4j.Neo4j;
import org.jooby.neo4j.Neo4jSessionStore;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import iot.jcypher.database.IDBAccess;

public class Neo4jBoltSessionApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db.url", ConfigValueFactory.fromAnyRef("bolt://localhost:7687"))
        .withValue("db.user", ConfigValueFactory.fromAnyRef("neo4j"))
        .withValue("db.password", ConfigValueFactory.fromAnyRef("development")));

    use(new Neo4j());

    session(Neo4jSessionStore.class);

    get("/set", req -> {
      String attr = req.param("attr").value("foo");
      Session session = req.session();
      session.set("attr", attr);
      return attr;
    });

    get("/get", req -> {
      Session session = req.session();
      return session.get("attr").value();
    });

    get("/remove", req -> {
      Session session = req.session();
      return session.unset("attr").value();
    });

    get("/clear", req -> {
      IDBAccess db = require(IDBAccess.class);
      db.clearDatabase();
      return "done";
    });

    get("/destroy", req -> {
      req.session().destroy();
      return "destroy";
    });
  }

  public static void main(final String[] args) {
    run(Neo4jBoltSessionApp::new, args);
  }
}
