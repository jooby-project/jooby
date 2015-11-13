package org.jooby;

import java.util.Optional;

import org.jooby.handlers.Cors;
import org.jooby.handlers.CorsHandler;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CorsCustomFeature extends ServerFeature {

  {
    use("*", new CorsHandler(new Cors()
        .withOrigin("http://foo.com")
        .withHeaders("*")
        .withoutCreds()
        .withExposedHeaders("H1")
        .withMaxAge(-1)));

    get("/greeting", req -> "Hello " + req.param("name").value("World") + "!");
  }

  @Test
  public void corsCustom() throws Exception {
    request()
        .get("/greeting")
        .header("Origin", "http://foo.com")
        .expect("Hello World!")
        .header("Access-Control-Allow-Origin", "http://foo.com")
        .header("Access-Control-Allow-Credentials", Optional.empty())
        .header("Access-Control-Expose-Headers", "H1")
        .header("Vary", "Origin");
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

  @Test
  public void preflight() throws Exception {
    request()
        .options("/greeting")
        .header("Origin", "http://foo.com")
        .header("Access-Control-Request-Headers", "Custom")
        .header("Access-Control-Request-Method", "GET")
        .expect(200)
        .header("Access-Control-Allow-Origin", "http://foo.com")
        .header("Access-Control-Allow-Headers", "Custom")
        .header("Access-Control-Allow-Methods", "GET,POST")
        .header("Access-Control-Allow-Credentials", Optional.empty())
        .header("Vary", "Origin");
  }

}
