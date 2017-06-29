package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.neo4j.Neo4j;
import org.neo4j.graphdb.GraphDatabaseService;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Neo4jFsApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("com.graphaware.runtime.enabled", ConfigValueFactory.fromAnyRef(true))
        .withValue("db", ConfigValueFactory.fromAnyRef("fs")));

    use(new Neo4j("target/localdb"));

    onStart(() -> {
      System.out.println(require(GraphDatabaseService.class));
    });

  }

  public static void main(final String[] args) {
    run(Neo4jFsApp::new, args);
  }
}
