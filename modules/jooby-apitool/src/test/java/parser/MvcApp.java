package parser;

import org.jooby.Jooby;

public class MvcApp extends Jooby {

  {
    use(MvcRoutes.class);
  }
}
