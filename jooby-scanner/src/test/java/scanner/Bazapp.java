package scanner;

import org.jooby.Jooby;

public class Bazapp extends Jooby {

  {
    get("/baz", () -> "bar");
  }
}
