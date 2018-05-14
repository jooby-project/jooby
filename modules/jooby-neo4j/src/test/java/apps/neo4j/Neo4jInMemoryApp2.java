package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.neo4j.Neo4j;
import org.neo4j.graphdb.GraphDatabaseService;

public class Neo4jInMemoryApp2 extends Jooby {

  {
    use(new Neo4j("mem"));

    onStart(() -> {
      System.out.println(require(GraphDatabaseService.class));
    });

  }

  public static void main(final String[] args) {
    run(Neo4jInMemoryApp2::new, args);
  }
}
