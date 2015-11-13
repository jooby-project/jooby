package org.jooby;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HeadRequestFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.route().method()));

    // custom head
    head("/head", (req, rsp) -> rsp.send(req.path()));

    // global head
    head();
  }

  @Test
  public void defaultHead() throws Exception {
    request()
        .head("/")
        .expect(200)
        .empty();
  }

  @Test
  public void realHead() throws Exception {
    request()
        .head("/head")
        .expect(200)
        .empty();
  }

  @Test
  public void notFound() throws Exception {
    request()
        .head("/404")
        .expect(404)
        .empty();
  }

}
