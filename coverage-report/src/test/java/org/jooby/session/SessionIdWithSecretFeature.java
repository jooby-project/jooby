package org.jooby.session;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SessionIdWithSecretFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("application.secret",
        ConfigValueFactory.fromAnyRef("1234$")));

    session(new Session.MemoryStore() {
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
      rsp.send("jooby.sid=" + req.cookie("jooby.sid").get().value().get() + ";"
          + req.session().id());
    });

  }

  @Test
  public void shouldHaveASignedID() throws Exception {
    request()
        .get("/session")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          request()
              .get("/sessionCookie")
              .expect(200)
              .expect(setCookie.substring(0, setCookie.indexOf(';')) + ";1234");
        });
  }

}
