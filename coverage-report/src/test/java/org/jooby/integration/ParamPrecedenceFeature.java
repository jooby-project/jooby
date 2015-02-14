package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ParamPrecedenceFeature extends ServerFeature {

  {

    post("/precedence/:name", (req, resp) -> {
      // path param
      assertEquals("a", req.param("name").stringValue());
      // query param
      assertEquals("b", req.param("name").toList(String.class).get(1));
      // body param
      assertEquals("c", req.param("name").toList(String.class).get(2));
      resp.send(req.param("name").toList(String.class));
    });

  }

  @Test
  public void paramPrecedence() throws Exception {
    request()
        .post("/precedence/a?name=b")
        .form()
        .add("name", "c")
        .expect("[a, b, c]");
  }

}
