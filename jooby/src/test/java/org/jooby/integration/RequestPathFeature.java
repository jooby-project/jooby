package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestPathFeature extends ServerFeature {

  {

    get("/hello", (req, rsp) -> rsp.send(req.path()));

    get("/u/p:id", (req, rsp) -> rsp.send(req.path()));

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
