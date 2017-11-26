package parser;

import org.jooby.Jooby;

public class FooApp extends Jooby {
  {
    get("/1", () -> "foo");
  }
}
