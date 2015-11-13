package org.jooby.ehcache;

import net.sf.ehcache.Ehcache;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class Ehcache1CacheFeature extends ServerFeature {

  {
    use(ConfigFactory.parseResources(getClass(), "ehcache-str.conf"));

    use(new Eh());

    get("/eh/1", req -> {
      return req.require(Ehcache.class).getName();
    });
  }

  @Test
  public void ehcache() throws Exception {
    request().get("/eh/1")
        .expect("cache1");
  }
}
