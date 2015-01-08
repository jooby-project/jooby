package org.jooby.integration.session;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.Session;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionIdWithSecretFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("1234$")));

    use(new Session.MemoryStore() {
      @Override
      public String generateID() {
        return "1234";
      }

    });

    get("/session", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.id());
    });

    get("/sessionCookie", (req, rsp) -> {
      rsp.send("jooby.sid=" + req.cookie("jooby.sid").get().value().get() + ";" + req.session().id());
    });

  }

  @Test
  public void shouldHaveASignedID() throws Exception {
    execute(GET(uri("session")), r0 -> {
      assertEquals(200, r0.getStatusLine().getStatusCode());
      String setCookie = r0.getFirstHeader("Set-Cookie").getValue();
      assertEquals(setCookie.substring(0, setCookie.indexOf(';')) + ";1234",
          execute(GET(uri("sessionCookie")), r1 -> {
            assertEquals(200, r1.getStatusLine().getStatusCode());
          }));
    });
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = Executor.newInstance().execute(request).returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }

}
