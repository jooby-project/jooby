package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.pac4j.Auth;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue624 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("auth.login.redirectTo", ConfigValueFactory.fromAnyRef("/afterlogin"))
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/624")));

    use(new Auth());

    get("/afterlogin", req -> req.path());
  }

  @Test
  public void shouldForceARedirect() throws Exception {
    request()
        .get("/624/auth?username=test&password=test")
        .expect("/afterlogin");
  }

}
