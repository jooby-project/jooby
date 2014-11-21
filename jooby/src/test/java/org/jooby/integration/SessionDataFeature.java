package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionDataFeature extends ServerFeature {

  {

    get("/s1", (req, rsp) -> {
      Session session = req.session();
      session.set("v1", "v1");
      rsp.send("xx");
    });

    get("/s2", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.get("v1"));
      session.unset("v1");
    });
  }

  @Test
  public void locals() throws Exception {
    execute(
        GET(uri("s1")),
        (r0) -> {
          String setCookie = r0.getFirstHeader("Set-Cookie").getValue();
          assertEquals(
              "Optional[v1]",
              execute(
                  GET(uri("s2")).addHeader("Cookie", setCookie),
                  (r1) -> {
                    assertEquals("Optional.empty",
                        execute(GET(uri("s2")).addHeader("Cookie", setCookie), (r2) -> {
                        }));
                  }));
        });

    assertEquals("Optional.empty", execute(GET(uri("s2")), (r) -> {
    }));

  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
