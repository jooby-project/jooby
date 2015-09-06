package org.jooby.hazelcast;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Ignore;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@Ignore
public class HazelcastSessionStoreIntSecsFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("hazelcast.session.timeout", ConfigValueFactory.fromAnyRef(2)));

    use(new Hcast());

    session(HcastSessionStore.class);

    get("/hcast2/create/session", req -> {
      Session session = req.session();
      session.set("k1", "v1");
      return session.get("k1").value();
    });

    get("/hcast2/get/session", req -> {
      Session session = req.session();
      return session.get("k1").toOptional();
    });

    get("/hcast2/destroy/session", req -> {
      Session session = req.session();
      session.destroy();
      return "done";
    });
  }

  @Test
  public void create() throws Exception {
    request().get("/hcast2/get/session")
        .expect("Optional.empty");

    request().get("/hcast2/create/session")
        .expect("v1");

    Thread.sleep(300L);

    request().get("/hcast2/get/session")
        .expect("Optional[v1]");

    Thread.sleep(2500L);

    // timeIdle will expire
    request().get("/hcast2/get/session")
        .expect("Optional.empty");

    // recreate
    request().get("/hcast2/create/session")
        .expect("v1");

    // get
    request().get("/hcast2/get/session")
        .expect("Optional[v1]");

    // destroy
    request().get("/hcast2/destroy/session")
        .expect(200);

    // get
    request().get("/hcast2/get/session")
        .expect("Optional.empty");
  }

}
