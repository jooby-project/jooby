package org.jooby.session;

import static org.junit.Assert.assertEquals;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionDataFeature extends ServerFeature {

  {

    get("/s1", (req, rsp) -> {
      Session session = req.session();
      assertEquals(false, session.isSet("v1"));
      session.set("v1", "v1");
      assertEquals(true, session.isSet("v1"));
      rsp.send(session.attributes());
    });

    get("/s2", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.get("v1").toOptional().orElse(""));
      session.unset("v1");
    });

    get("/unset-all", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.get("v1").value());
      session.unset();
    });
  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/s1")
        .expect("{v1=v1}")
        .request(r1 ->
            r1.get("/s2")
                .expect("v1")
                .request(r2 ->
                    r2.get("/s2")
                        .expect("")
                )
        );

  }

  @Test
  public void unsetall() throws Exception {
    request()
        .get("/s1")
        .expect("{v1=v1}")
        .request(r1 ->
            r1.get("/unset-all")
                .expect("v1")
                .request(r2 ->
                    r2.get("/s2")
                        .expect("")
                        .request(r3 ->
                            r3.get("/s2")
                                .expect("")
                        )
                )
        );
  }

}
