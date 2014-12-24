package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestIPFeature extends ServerFeature {

  {

    get("/ip", (req) ->  req.ip());

    get("/hostname", (req) ->  req.hostname());

  }

  @Test
  public void ip() throws Exception {
    assertEquals("127.0.0.1", Request.Get(uri("ip").build()).execute().returnContent().asString());
  }

  @Test
  public void hostname() throws Exception {
    assertEquals("localhost", Request.Get(uri("hostname").build()).execute().returnContent().asString());
  }

}
