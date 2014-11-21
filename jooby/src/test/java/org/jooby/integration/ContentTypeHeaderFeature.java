package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ContentTypeHeaderFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.type()));
  }

  @Test
  public void defaultContentType() throws Exception {
    assertEquals("*/*", Request.Get(uri("/").build()).execute().returnContent().asString());
  }

  @Test
  public void htmlContentType() throws Exception {
    assertEquals("text/html", Request.Get(uri("/").build()).addHeader("content-type", "text/html")
        .execute().returnContent().asString());
  }

  @Test
  public void jsonContentType() throws Exception {
    assertEquals("application/json", Request.Get(uri("/").build())
        .addHeader("content-type", "application/json")
        .execute().returnContent().asString());
  }

}
