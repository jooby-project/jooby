package io.jooby.redis;

import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;

public class RedisURITest {
  @Test
  public void shouldCreateURI() {
    RedisURI redis = RedisURI.create("redis");
    System.out.println(redis);
  }
}
