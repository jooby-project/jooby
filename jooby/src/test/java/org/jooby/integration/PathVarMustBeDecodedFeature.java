package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class PathVarMustBeDecodedFeature extends ServerFeature {

  {
    get("/:var", req -> req.param("var").stringValue());
  }

  @Test
  public void pathVarShouldBeDecoded() throws Exception {
    assertEquals("path with spaces", Request.Get(uri("path%20with%20spaces").build()).execute()
        .returnContent().asString());

    assertEquals("plus plus", Request.Get(uri("plus+plus").build()).execute()
        .returnContent().asString());
  }
}
