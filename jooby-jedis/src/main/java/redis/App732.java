package redis;

import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.Jooby;
import org.jooby.jedis.Redis;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import redis.clients.jedis.Jedis;

public class App732 extends Jooby {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6379")));

    use(new Redis());

    onStart(() -> {
      try (Jedis jedis = require(Jedis.class)) {
        jedis.set("foo", "bar");
      }
    });

    AtomicInteger inc = new AtomicInteger();
    get("/", () -> {
      try (Jedis jedis = require(Jedis.class)) {
        return jedis.get("foo") + ":" + inc.incrementAndGet();
      }
    });
  }

  public static void main(final String[] args) {
    run(App732::new, args);
  }
}
