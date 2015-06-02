package org.jooby.ehcache;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class EhcacheSessionStoreFeature extends ServerFeature {

  {
    use(ConfigFactory.parseResources(getClass(), "ehcache-sessions.conf"));

    use(new Eh());

    session(EhSessionStore.class);

    get("/eh/create/session", req -> {
      Session session = req.session();
      session.set("k1", "v1");
      return session.get("k1").value();
    });

    get("/eh/get/session", req -> {
      Session session = req.session();
      return session.get("k1").toOptional();
    });

    get("/eh/destroy/session", req -> {
      Session session = req.session();
      session.destroy();
      return "done";
    });
  }

  @Test
  public void create() throws Exception {
    request().get("/eh/get/session")
        .expect("Optional.empty");

    request().get("/eh/create/session")
        .expect("v1");

    request().get("/eh/get/session")
        .expect("Optional[v1]");

    Thread.sleep(2500L);

    // timeIdle will expire
    request().get("/eh/get/session")
        .expect("Optional.empty");

    // recreate
    request().get("/eh/create/session")
        .expect("v1");

    // get
    request().get("/eh/get/session")
        .expect("Optional[v1]");

    // destroy
    request().get("/eh/destroy/session")
        .expect(200);

    // get
    request().get("/eh/get/session")
        .expect("Optional.empty");
  }

}
