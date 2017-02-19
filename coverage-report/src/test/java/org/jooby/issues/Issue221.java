package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.test.ServerFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Issue221 extends ServerFeature {

  {

    securePort(9943);
    use(ConfigFactory.empty()
        .withValue("application.securePort", ConfigValueFactory.fromAnyRef(securePort))
        .withValue("application.redirect_https",
            ConfigValueFactory.fromAnyRef("https://localhost:" + securePort + "/{0}")));

    get("/https", req -> req.secure());
  }

  @Test
  public void shouldRedirectToHttps() throws Exception {
    request()
        .get("/https")
        .expect("true");
  }

  @BeforeClass
  public static void httpsOn() {
    protocol = "https";
  }

  @AfterClass
  public static void httpsOff() {
    protocol = "http";
  }

}
