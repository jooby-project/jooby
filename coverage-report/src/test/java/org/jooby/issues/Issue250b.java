package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue250b extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.securePort",
        ConfigValueFactory.fromAnyRef(9943)));

    get("/", req -> {
      req.port();
      return req.header("host").value() + ":" + req.port();
    });
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
  public void defaultHttpsPort() throws Exception {
    request().get("/")
        .header("host", "3edfd8c1.ngrok.io")
        .expect("3edfd8c1.ngrok.io:443");
  }

}
