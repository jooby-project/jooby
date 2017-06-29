package apps.neo4j;

import org.jooby.Jooby;
import org.jooby.neo4j.Neo4j;

public class Neo4jWrongApp extends Jooby {

  {
    use(new Neo4j());
  }

  public static void main(final String[] args) {
    run(Neo4jWrongApp::new, args);
  }
}
