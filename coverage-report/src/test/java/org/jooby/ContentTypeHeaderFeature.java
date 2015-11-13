package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ContentTypeHeaderFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.type()));
  }

  @Test
  public void defaultContentType() throws Exception {
    request()
        .get("/")
        .expect("*/*");
  }

  @Test
  public void htmlContentType() throws Exception {
    request()
        .get("/")
        .header("content-type", "text/html")
        .expect("text/html");
  }

  @Test
  public void jsonContentType() throws Exception {
    request()
        .get("/")
        .header("content-type", "application/json")
        .expect("application/json");
  }

}
