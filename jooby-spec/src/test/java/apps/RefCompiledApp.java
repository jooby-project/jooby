package apps;

import org.jooby.Jooby;

public class RefCompiledApp extends Jooby {

  {

    use(new CompiledApp());

    get("/after", () -> "after");
  }
}
