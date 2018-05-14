package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Issue441b extends ServerFeature {

  {
    securePort(8877);

    get("/441", req -> req.port());
  }

  @BeforeClass
  public static void httpsOn() {
    protocol = "https";
  }

  @AfterClass
  public static void httpsOff() {
    protocol = "http";
  }

  @Test
  public void hello() throws Exception {
    request()
        .get("/441")
        .expect("8877");

  }

}
