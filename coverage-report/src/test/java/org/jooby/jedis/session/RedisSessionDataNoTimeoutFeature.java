package org.jooby.jedis.session;

import static org.junit.Assert.assertTrue;

import org.jooby.Session;
import org.jooby.jedis.Redis;
import org.jooby.jedis.RedisSessionStore;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class RedisSessionDataNoTimeoutFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6379"))
        .withValue("application.session.timeout", ConfigValueFactory.fromAnyRef("0")));

    use(new Redis());

    session(RedisSessionStore.class);

    get("/s1", req -> {
      Session session = req.session();
      session
          .set("name", "edgar")
          .set("age", 34);
      return session.attributes();
    });

    get("/s2", req -> {
      Session session = req.session();
      return session.attributes();
    });

    get("/destroy", req -> {
      req.session().destroy();;
      return "done";
    });
  }

  @Test
  public void save() throws Exception {
    request()
        .get("/s1")
        .expect(rsp -> {
          assertTrue(rsp.equals("{name=edgar, age=34}") || rsp.equals("{age=34, name=edgar}"));
        })
        .request(r1 ->
            r1.get("/s2")
                .expect(rsp -> {
                  assertTrue(rsp.equals("{name=edgar, age=34}")
                      || rsp.equals("{age=34, name=edgar}"));
                }).request(r2 -> {
                  // cleanup
                    r2.get("/destroy")
                        .expect("done");
                  })
        );

  }
}
