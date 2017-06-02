package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue191 extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("application.securePort", ConfigValueFactory.fromAnyRef("8443"))
        .withValue("application.redirect_https",
            ConfigValueFactory.fromAnyRef("https://localhost:8443/{0}")));

    get("/https", req -> req.secure());
  }

  @Test
  public void shouldRedirectToHttps() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/https")
        .expect(302)
        .header("Location", "https://localhost:8443/https");
  }

}
