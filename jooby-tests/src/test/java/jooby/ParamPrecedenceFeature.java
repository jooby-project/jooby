package jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
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
    assertEquals("[a, b, c]", Request.Post(uri("precedence/a?name=b").build())
        .bodyForm(
            new BasicNameValuePair("name", "c")
        ).execute().returnContent().asString());
  }

}
