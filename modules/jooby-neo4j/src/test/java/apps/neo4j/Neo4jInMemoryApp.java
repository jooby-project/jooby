package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.neo4j.Neo4j;
import org.neo4j.graphdb.GraphDatabaseService;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Neo4jInMemoryApp extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));
    use(new Neo4j());

    onStart(() -> {
      System.out.println(require(GraphDatabaseService.class));
    });

  }

  public static void main(final String[] args) {
    run(Neo4jInMemoryApp::new, args);
  }
}
