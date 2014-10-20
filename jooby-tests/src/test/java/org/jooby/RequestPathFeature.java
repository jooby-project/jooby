package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class RequestPathFeature extends ServerFeature {

  {

    get("/hello", (req, res) -> res.send(req.path()));

    get("/u/p:id", (req, res) -> res.send(req.path()));

  }

  @Test
  public void requestPath() throws Exception {
    assertEquals("/hello", Request.Get(uri("hello").build()).execute().returnContent().asString());
  }

  @Test
  public void varRequestPath() throws Exception {
    assertEquals("/u/p1", Request.Get(uri("/u/p1").build()).execute().returnContent().asString());
  }

}
