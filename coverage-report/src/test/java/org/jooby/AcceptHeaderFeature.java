package org.jooby;

import org.jooby.MediaType;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AcceptHeaderFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.accept()));

    get("/accept",
        (req, rsp) -> rsp.send(req.accepts(req.param("type").toList(MediaType.class))
            .map(MediaType::toString).orElse("nope")));
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
