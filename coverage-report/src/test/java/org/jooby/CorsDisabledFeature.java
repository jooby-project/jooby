package org.jooby;

import java.util.Optional;

import org.jooby.handlers.Cors;
import org.jooby.handlers.CorsHandler;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CorsDisabledFeature extends ServerFeature {

  {
    use("*", new CorsHandler(
        new Cors().withOrigin("http://foo.com")
            .withHeaders("*")
            .withoutCreds()
            .withExposedHeaders("H1")
            .withMaxAge(-1)
            .disabled()));

    get("/greeting", req -> "Hello " + req.param("name").value("World") + "!");
  }

  @Test
  public void skipcors() throws Exception {
    request()
        .get("/greeting")
        .header("Origin", "http://bar.com")
        .expect("Hello World!")
        .header("Access-Control-Allow-Origin", Optional.empty())
        .header("Access-Control-Allow-Credentials", Optional.empty());
  }

}
