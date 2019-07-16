package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.json.Gzon;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue930 extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));
    use(new Gzon());
  }

  @Test
  public void gzonRenderJsonWithWideAcceptIsPresent() throws Exception {
    request()
        .get("/930")
        .header("Accept", "application/json, */*")
        .expect("{\"message\":\"Not Found(404): /930\",\"status\":404,\"reason\":\"Not Found\"}");

    request()
        .get("/930")
        .header("Accept", "application/json")
        .expect("{\"message\":\"Not Found(404): /930\",\"status\":404,\"reason\":\"Not Found\"}");
  }

}
