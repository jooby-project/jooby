package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HelloHttpsFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.securePort",
        ConfigValueFactory.fromAnyRef(8443)));

    get("/", req -> "Hello");

    get("/bye", req -> "bye!");
  }

  @Test
  public void hello() throws Exception {
    request("https")
        .get("/")
        .expect("Hello");

    request("https")
        .get("/bye")
        .expect("bye!");
  }

}
