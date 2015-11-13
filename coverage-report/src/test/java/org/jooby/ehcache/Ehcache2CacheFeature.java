package org.jooby.ehcache;

import static org.junit.Assert.assertEquals;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class Ehcache2CacheFeature extends ServerFeature {

  {
    use(ConfigFactory.parseResources(getClass(), "ehcache-hash.conf"));

    use(new Eh().doWith(conf-> {
      CacheConfiguration c1 = conf.getCacheConfigurations().get("c1");
      assertEquals(true, c1.isEternal());

      CacheConfiguration c2 = conf.getCacheConfigurations().get("c2");
      assertEquals(10, c2.getMaxEntriesLocalHeap());
    }));

    get("/eh/:cache", req -> {
      return req.require(req.param("cache").value(), Ehcache.class).getName();
    });
  }

  @Test
  public void ehcache() throws Exception {
    request().get("/eh/c1")
        .expect("c1");

    request().get("/eh/c2")
        .expect("c2");
  }
}
