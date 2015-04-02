package org.jooby.jedis;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import redis.clients.jedis.Jedis;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JedisCustomDbPropertyFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("mydb", ConfigValueFactory.fromAnyRef("redis://localhost:6379"))
        .withValue("jedis.mydb.maxTotal", ConfigValueFactory.fromAnyRef(8)));


    use(new Redis("mydb"));

    get("/:key/:value", req -> {
      try (Jedis jedis = req.require(Key.get(Jedis.class, Names.named("mydb")))) {
        String key = req.param("key").value();
        jedis.setex(key, 120, req.param("value").value());
        return jedis.get(key);
      }
    });
  }

  @Test
  public void connect() throws Exception {
    request()
        .get("/foo2/bar")
        .expect("bar");
  }
}
