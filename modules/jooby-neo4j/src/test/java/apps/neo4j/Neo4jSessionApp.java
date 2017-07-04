package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.Session;
import org.jooby.neo4j.Neo4j;
import org.jooby.neo4j.Neo4jSessionStore;

import com.google.common.collect.ImmutableMap;
import com.graphaware.neo4j.expire.ExpirationModuleBootstrapper;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import iot.jcypher.database.IDBAccess;

public class Neo4jSessionApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("session.timeout", ConfigValueFactory.fromAnyRef(30))
        .withValue("com.graphaware.runtime.enabled", ConfigValueFactory.fromAnyRef(true))
        .withValue("com.graphaware.module", ConfigValueFactory.fromAnyRef(
            (ImmutableMap.of("class", ExpirationModuleBootstrapper.class.getName(),
                "nodeExpirationProperty", "_expire")))));

    use(new Neo4j("mem"));

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

    get("/unset", req -> {
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
    run(Neo4jSessionApp::new, args);
  }
}
