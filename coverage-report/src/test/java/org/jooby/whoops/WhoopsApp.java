package org.jooby.whoops;

import org.jooby.Jooby;
import org.jooby.Request;

public class WhoopsApp extends Jooby {

  {
    use(new Whoops());

    get("/", req -> {
      try {
        return doSomething(req);
      } catch (Exception ex) {
        throw new IllegalStateException("Xxx", ex);
      }
    });
  }

  public static void main(final String[] args) throws Throwable {
    run(WhoopsApp::new, args);
  }

  private Object doSomething(final Request req) {
    try {
    return dx();
    } catch(Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private Object dx() {
    throw new UnsupportedOperationException("Something broken!");
  }
}
