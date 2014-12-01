package org.jooby.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RequestLocalsFeature extends ServerFeature {

  {

    get("/locals", (req) -> {
      assertFalse(req.isSet("l1"));
      req.set("l1", "v1");
      assertTrue(req.isSet("l1"));
      return req.get("l1");
    });

    get("/locals/unset", (req) -> {
      req.set("l1", "v1");
      Optional<Object> val = req.unset("l1");
      assertFalse(req.isSet("l1"));
      return val;
    });

    get("/locals/unsetall", (req) -> {
      req.set("l1", "v1");
      Optional<Object> val = req.get("l1");
      req.unset();
      assertFalse(req.isSet("l1"));
      return val;
    });

    get("/locals/attributes", (req) -> {
      req.set("l1", "v1");
      return req.attributes();
    });

  }

  @Test
  public void locals() throws Exception {
    assertEquals("Optional[v1]", Request.Get(uri("locals").build()).execute().returnContent()
        .asString());
  }

  @Test
  public void unset() throws Exception {
    assertEquals("Optional[v1]", Request.Get(uri("locals", "unset").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void unsetall() throws Exception {
    assertEquals("Optional[v1]", Request.Get(uri("locals", "unsetall").build()).execute()
        .returnContent().asString());
  }

  @Test
  public void attributes() throws Exception {
    assertEquals("{l1=v1}", Request.Get(uri("locals", "attributes").build()).execute()
        .returnContent().asString());
  }

}
