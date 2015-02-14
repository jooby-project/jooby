package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue22 extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/demo")));

    get("/", () -> "/");

    get("/path", () -> "path");
  }

  @Test
  public void appShouldBeMountedOnApplicationPath() throws Exception {
    request()
        .get("/demo")
        .expect(200)
        .expect("/");

    request()
        .get("/demo/path")
        .expect(200)
        .expect("path");

  }

  @Test
  public void wrongPathShouldResolveAs404() throws Exception {
    request()
        .get("/")
        .expect(404);

    request()
        .get("/path")
        .expect(404);

  }

}
