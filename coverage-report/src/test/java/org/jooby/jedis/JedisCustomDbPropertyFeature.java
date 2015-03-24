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
        jedis.set(req.param("key").value(), req.param("value").value());
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
