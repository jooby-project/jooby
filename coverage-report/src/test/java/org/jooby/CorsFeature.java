package org.jooby;

import java.util.Optional;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CorsFeature extends ServerFeature {

  {
    cors();

    get("/greeting", req -> "Hello " + req.param("name").value("World") + "!");
  }

  @Test
  public void corsdef() throws Exception {
    request()
        .get("/greeting")
        .header("Origin", "http://foo.com")
        .expect("Hello World!")
        .header("Access-Control-Allow-Origin", "http://foo.com")
        .header("Access-Control-Allow-Credentials", true);
  }

  @Test
  public void skipcors() throws Exception {
    request()
        .get("/greeting")
        .expect("Hello World!")
        .header("Access-Control-Allow-Origin", Optional.empty())
        .header("Access-Control-Allow-Credentials", Optional.empty());
  }

  @Test
  public void corsChromeLocalFile() throws Exception {
    request()
        .get("/greeting")
        .header("Origin", "null")
        .expect("Hello World!")
        .header("Access-Control-Allow-Origin", "*");
  }

  @Test
  public void preflight() throws Exception {
    request()
        .options("/greeting")
        .header("Origin", "http://foo.com")
        .header("Access-Control-Request-Method", "GET")
        .expect("")
        .expect(200)
        .header("Access-Control-Allow-Origin", "http://foo.com")
        .header("Access-Control-Allow-Methods", "GET,POST")
        .header("Access-Control-Allow-Headers", "X-Requested-With,Content-Type,Accept,Origin")
        .header("Access-Control-Allow-Credentials", true)
        .header("Access-Control-Max-Age", 1800);
  }

  @Test
  public void preflightAsSimple() throws Exception {
    request()
        .options("/greeting")
        .header("Origin", "http://foo.com")
        .expect(405)
        .header("Access-Control-Allow-Origin", Optional.empty())
        .header("Access-Control-Allow-Methods", Optional.empty())
        .header("Access-Control-Allow-Headers", Optional.empty())
        .header("Access-Control-Allow-Credentials", Optional.empty())
        .header("Access-Control-Max-Age", Optional.empty());
  }

  @Test
  public void preflightMethodNotAllowed() throws Exception {
    request()
        .options("/greeting")
        .header("Origin", "http://foo.com")
        .header("Access-Control-Request-Method", "PUT")
        .expect(405)
        .header("Access-Control-Allow-Origin", Optional.empty())
        .header("Access-Control-Allow-Methods", Optional.empty())
        .header("Access-Control-Allow-Headers", Optional.empty())
        .header("Access-Control-Allow-Credentials", Optional.empty())
        .header("Access-Control-Max-Age", Optional.empty());
  }

  @Test
  public void preflightHeaderNotAllowed() throws Exception {
    request()
        .options("/greeting")
        .header("Origin", "http://foo.com")
        .header("Access-Control-Request-Method", "GET")
        .header("Access-Control-Request-Headers", "Custom-Header")
        .expect(405)
        .header("Access-Control-Allow-Origin", (String) null)
        .header("Access-Control-Allow-Methods", (String) null)
        .header("Access-Control-Allow-Headers", (String) null)
        .header("Access-Control-Allow-Credentials", (String) null)
        .header("Access-Control-Max-Age", (String) null);
  }

}
