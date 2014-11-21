package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.jooby.Mutant;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class AllParamsFeature extends ServerFeature {

  {

    post("/:name", (req, rsp) -> {
      Map<String, Mutant> params = req.params();
      assertEquals("a", params.get("name").toList(String.class).get(0));
      assertEquals("b", params.get("name").toList(String.class).get(1));
      assertEquals("c", params.get("name").toList(String.class).get(2));
      assertEquals("d", params.get("name").toList(String.class).get(3));
      assertEquals("a1", params.get("p1").stringValue());
      assertEquals("a2", params.get("p2").stringValue());

      rsp.send("done");
    });

  }

  @Test
  public void paramPrecedence() throws Exception {
    assertEquals("done", Request.Post(uri("/a?name=b&name=c&p1=a1").build())
        .bodyForm(
            new BasicNameValuePair("name", "d"),
            new BasicNameValuePair("p2", "a2")
        ).execute().returnContent().asString());
  }

}
