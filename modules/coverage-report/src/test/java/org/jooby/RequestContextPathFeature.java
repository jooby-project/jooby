package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class RequestContextPathFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.path", ConfigValueFactory.fromAnyRef("/x")));

    get("/hello", req -> req.contextPath() + req.path());

    get("/u/p:id", req -> req.contextPath() + req.path());

  }

  @Test
  public void requestPath() throws Exception {
    request()
        .get("/x/hello")
        .expect("/x/hello");
  }

  @Test
  public void varRequestPath() throws Exception {
    request()
        .get("/x/u/p1")
        .expect("/x/u/p1");
  }

}
