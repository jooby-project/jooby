package apps.pac4j;

import org.jooby.Jooby;
import org.jooby.pac4j.Pac4j;

public class Pac4jMultiClientDemo extends Jooby {

  {

    get("/", () -> "OK");

    get("/form", () -> "Form");
  }

  public static void main(String[] args) throws Exception {
    run(Pac4jMultiClientDemo::new, args);
  }
}
