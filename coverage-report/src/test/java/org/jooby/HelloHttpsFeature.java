package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HelloHttpsFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.securePort",
        ConfigValueFactory.fromAnyRef(9943)));

    get("/", () -> "Hello");

    get("/bye", () -> "bye!");
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
        .get("/")
        .expect("Hello");

    request()
        .get("/bye")
        .expect("bye!");
  }

}
