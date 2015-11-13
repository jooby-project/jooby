package org.jooby;

import java.util.List;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AcceptHeaderFeature extends ServerFeature {

  {
    get("/", req -> req.accept());

    get("/accept", req -> {
      List<MediaType> types = req.param("type").toList(MediaType.class);
      return req.accepts(types)
          .map(MediaType::toString).orElse("nope");
    });
  }

  @Test
  public void defaultAccept() throws Exception {
    request()
        .get("/")
        .expect("[*/*]");
  }

  @Test
  public void htmlAccept() throws Exception {
    request()
        .get("/")
        .header("accept", "text/html")
        .expect("[text/html]");
  }

  @Test
  public void multipleAcceptHeader() throws Exception {
    request()
        .get("/")
        .header("accept", "text/html,*/*")
        .expect("[text/html, */*]");
  }

  @Test
  public void accepts() throws Exception {
    request()
        .get("/accept?type=text/html")
        .header("accept", "text/html")
        .expect("text/html");

    request()
        .get("/accept?type=text/html")
        .header("accept", "text/*, application/json")
        .expect("text/html");

    request()
        .get("/accept?type=application/json&type=text/plain")
        .header("accept", "text/*, application/json")
        .expect("application/json");

    request().
        get("/accept?type=application/json")
        .header("accept", "text/*, application/json")
        .expect("application/json");

    request().get("/accept?type=image/png")
        .header("accept", "text/*, application/json")
        .expect("nope");

    request()
        .get("/accept?type=text/html&type=application/json")
        .header("accept", "text/*;q=.5, application/json")
        .expect("application/json");
  }

}
