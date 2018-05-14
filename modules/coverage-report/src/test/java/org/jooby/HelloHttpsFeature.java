package org.jooby;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HelloHttpsFeature extends ServerFeature {

  {
    securePort(9443);
    use(ConfigFactory.empty().withValue("application.securePort",
        ConfigValueFactory.fromAnyRef(securePort)));

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
