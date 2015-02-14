package org.jooby.integration.session;

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
      rsp.send(session.get("v1"));
      session.unset("v1");
    });

    get("/unset-all", (req, rsp) -> {
      Session session = req.session();
      rsp.send(session.get("v1"));
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
                .expect("Optional[v1]")
                .request(r2 ->
                    r2.get("/s2")
                        .expect("Optional.empty")
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
                .expect("Optional[v1]")
                .request(r2 ->
                    r2.get("/s2")
                        .expect("Optional.empty")
                        .request(r3 ->
                            r3.get("/s2")
                                .expect("Optional.empty")
                        )
                )
        );
  }

}
