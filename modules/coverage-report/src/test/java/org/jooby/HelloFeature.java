package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HelloFeature extends ServerFeature {

  {
    get("/", req -> "Hello");

    get("/bye", req -> "bye!");
  }

  @Test
  public void hello() throws Exception {
    request()
        .get("/")
        .expect("Hello");

    request()
        .get("/bye")
        .expect("bye!");

    request()
        .get("/not-found")
        .expect(404);
  }

}
