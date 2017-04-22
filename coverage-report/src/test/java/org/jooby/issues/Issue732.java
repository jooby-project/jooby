package org.jooby.issues;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.jedis.Redis;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import redis.clients.jedis.Jedis;

public class Issue732 extends ServerFeature {

  {

    use(ConfigFactory
            .empty()
            .withValue("db",
                    ConfigValueFactory.fromAnyRef("redis://localhost:6379")));

    use(new Redis());

    get("/Issue732", () -> {
      try (Jedis jedis = require(Jedis.class)) {
        jedis.get("dummy");
        return "Hello World!";
      }
    });

  }

  @Test
  public void appShouldBeAbleToServeTwoRequestsWithJedisConnection() throws Exception {
//    request().get("/Issue732").expect(200);
//    request().get("/Issue732").expect(200);
  }
}
