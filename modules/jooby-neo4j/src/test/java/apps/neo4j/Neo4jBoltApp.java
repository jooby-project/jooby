package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.neo4j.Neo4j;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import iot.jcypher.database.remote.BoltDBAccess;

public class Neo4jBoltApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db.url", ConfigValueFactory.fromAnyRef("bolt://localhost:7687"))
        .withValue("db.user", ConfigValueFactory.fromAnyRef("neo4j"))
        .withValue("db.password", ConfigValueFactory.fromAnyRef("development")));

    use(new Neo4j());

    onStart(() -> {
      System.out.println(require(BoltDBAccess.class).getSession());
    });

  }

  public static void main(final String[] args) {
    run(Neo4jBoltApp::new, args);
  }
}
