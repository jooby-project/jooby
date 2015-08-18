package org.jooby.hazelcast;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HazelcastSessionStoreFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("hazelcast.session.timeout", ConfigValueFactory.fromAnyRef("1s")));

    use(new Hcast());

    session(HcastSessionStore.class);

    get("/hcast/create/session", req -> {
      Session session = req.session();
      session.set("k1", "v1");
      return session.get("k1").value();
    });

    get("/hcast/get/session", req -> {
      Session session = req.session();
      return session.get("k1").toOptional();
    });

    get("/hcast/destroy/session", req -> {
      Session session = req.session();
      session.destroy();
      return "done";
    });
  }

  @Test
  public void create() throws Exception {
    request().get("/hcast/get/session")
        .expect("Optional.empty");

    request().get("/hcast/create/session")
        .expect("v1");

    Thread.sleep(300L);

    request().get("/hcast/get/session")
        .expect("Optional[v1]");

    Thread.sleep(1500L);

    // timeIdle will expire
    request().get("/hcast/get/session")
        .expect("Optional.empty");

    // recreate
    request().get("/hcast/create/session")
        .expect("v1");

    // get
    request().get("/hcast/get/session")
        .expect("Optional[v1]");

    // destroy
    request().get("/hcast/destroy/session")
        .expect(200);

    // get
    request().get("/hcast/get/session")
        .expect("Optional.empty");
  }

}
