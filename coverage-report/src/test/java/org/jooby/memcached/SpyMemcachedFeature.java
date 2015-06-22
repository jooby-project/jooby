package org.jooby.memcached;

import java.util.Arrays;

import net.spy.memcached.MemcachedClient;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SpyMemcachedFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("memcached.server", ConfigValueFactory.fromAnyRef(Arrays.asList("localhost:11211"))));

    use(new SpyMemcached());

    get("/spymemcached/:key/:value", req -> {
      MemcachedClient client = req.require(MemcachedClient.class);
      return client.set(req.param("key").value(), 3600, req.param("value").value()).get();
    });

    get("/spymemcached/:key", req -> {
      MemcachedClient client = req.require(MemcachedClient.class);
      return client.get(req.param("key").value());
    });
  }

  @Test
  public void memcached() throws Exception {
    request().get("/spymemcached/foo:/bar").expect("true");
    request().get("/spymemcached/foo:").expect("bar");
  }
}
