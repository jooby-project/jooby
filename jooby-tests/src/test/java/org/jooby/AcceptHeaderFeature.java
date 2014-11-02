package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.MediaType;
import org.junit.Test;

public class AcceptHeaderFeature extends ServerFeature {

  {
    get("/", (req, rsp) -> rsp.send(req.accept()));

    get("/accept",
        (req, rsp) -> rsp.send(req.accepts(req.param("type").toList(MediaType.class))
            .map(MediaType::toString).orElse("nope")));
  }

  @Test
  public void defaultAccept() throws Exception {
    assertEquals("[*/*]", Request.Get(uri("/").build()).execute().returnContent().asString());
  }

  @Test
  public void htmlAccept() throws Exception {
    assertEquals("[text/html]", Request.Get(uri("/").build()).addHeader("accept", "text/html")
        .execute().returnContent().asString());
  }

  @Test
  public void multipleAcceptHeader() throws Exception {
    assertEquals("[text/html, */*]", Request.Get(uri("/").build())
        .addHeader("accept", "text/html,*/*")
        .execute().returnContent().asString());
  }

  @Test
  public void accepts() throws Exception {
    assertEquals("text/html",
        Request.Get(uri("/accept?type=text/html").build()).addHeader("accept", "text/html")
            .execute().returnContent().asString());

    assertEquals(
        "text/html", Request.Get(uri("/accept?type=text/html").build())
            .addHeader("accept", "text/*, application/json")
            .execute().returnContent().asString());

    assertEquals(
        "application/json",
        Request.Get(uri("/accept?type=application/json&type=text/plain").build())
            .addHeader("accept", "text/*, application/json")
            .execute().returnContent().asString());

    assertEquals(
        "application/json", Request.Get(uri("/accept?type=application/json").build())
            .addHeader("accept", "text/*, application/json")
            .execute().returnContent().asString());

    assertEquals(
        "nope", Request.Get(uri("/accept?type=image/png").build())
            .addHeader("accept", "text/*, application/json")
            .execute().returnContent().asString());

    assertEquals(
        "application/json",
        Request.Get(uri("/accept?type=text/html&type=application/json").build())
            .addHeader("accept", "text/*;q=.5, application/json")
            .execute().returnContent().asString());
  }

}
