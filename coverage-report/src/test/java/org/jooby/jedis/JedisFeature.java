package org.jooby.jedis;

import java.util.HashMap;
import java.util.Map;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import redis.clients.jedis.Jedis;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JedisFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6379")));

    use(new Redis());

    get("/:key/:value", req -> {
      try (Jedis jedis = req.require(Jedis.class)) {
        jedis.set(req.param("key").value(), req.param("value").value());

        Map<String, String> attrs = new HashMap<>();
        attrs.put("name", "edgar");
        attrs.put("age", "34");
        jedis.hmset("session:1", attrs);

        return jedis.get(req.param("key").value());
      }
    });
  }

  @Test
  public void connect() throws Exception {
    request()
        .get("/foo/bar")
        .expect("bar");
  }
}
