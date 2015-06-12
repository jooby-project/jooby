package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue67 extends ServerFeature {
  {
    get("/", req -> "Hello World!");
  }

  @Test
  public void timeout() throws Exception {
    request()
        .get("/")
        .header("Connection", "close")
        .expect("Hello World!");

    Thread.sleep(500000L);
  }
}
