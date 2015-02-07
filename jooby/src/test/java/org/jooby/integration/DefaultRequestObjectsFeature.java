package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class DefaultRequestObjectsFeature extends ServerFeature {

  {

    get("/req", req-> {
      return req.require(org.jooby.Request.class).path();
    });

    get("/rsp", req-> {
      req.require(org.jooby.Response.class);
      return "/rsp";
    });

    get("/session", req-> {
      req.require(org.jooby.Session.class);
      return "/session";
    });

  }

  @Test
  public void reqParam() throws Exception {
    assertEquals("/req", GET(uri("req")));
  }

  @Test
  public void rspParam() throws Exception {
    assertEquals("/rsp", GET(uri("rsp")));
  }

  @Test
  public void sessionParam() throws Exception {
    assertEquals("/session", GET(uri("session")));
  }

  private static String GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build()).execute().returnContent().asString();
  }

}
