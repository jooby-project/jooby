package org.jooby.ehcache;

import static org.junit.Assert.assertEquals;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class EhcacheFeature extends ServerFeature {

  {
    use(ConfigFactory
        .empty()
        .withValue("ehcache.cache.default.maxEntriesLocalHeap",
            ConfigValueFactory.fromAnyRef(10000))
        .withValue("ehcache.cache.default.eternal", ConfigValueFactory.fromAnyRef(false))
        .withValue("ehcache.cache.default.timeToIdle", ConfigValueFactory.fromAnyRef("2m"))
        .withValue("ehcache.cache.default.timeToLive", ConfigValueFactory.fromAnyRef("2m"))
        .withValue("ehcache.cache.default.maxEntriesLocalDisk",
            ConfigValueFactory.fromAnyRef(10000000))
        .withValue("ehcache.cache.default.diskExpiryThreadInterval",
            ConfigValueFactory.fromAnyRef("2m"))
        .withValue("ehcache.cache.default.memoryStoreEvictionPolicy",
            ConfigValueFactory.fromAnyRef("lru"))
        .withValue("ehcache.cache.default.persistence.strategy",
            ConfigValueFactory.fromAnyRef("LOCALTEMPSWAP")));

    use(new Eh().doWith(conf -> {
      CacheConfiguration cache = conf.getDefaultCacheConfiguration();
      assertEquals(10000, cache.getMaxEntriesLocalHeap());
      assertEquals(false, cache.isEternal());
      assertEquals(120, cache.getTimeToIdleSeconds());
      assertEquals(120, cache.getTimeToLiveSeconds());
      assertEquals(10000000, cache.getMaxEntriesLocalDisk());
      assertEquals(120, cache.getDiskExpiryThreadIntervalSeconds());
      assertEquals(MemoryStoreEvictionPolicy.LRU, cache.getMemoryStoreEvictionPolicy());
      assertEquals(Strategy.LOCALTEMPSWAP, cache.getPersistenceConfiguration().getStrategy());
    }));

    get("/eh/cm", req -> {
      return req.require(CacheManager.class).getCacheNames().length;
    });
  }

  @Test
  public void ehcache() throws Exception {
    request().get("/eh/cm")
        .expect("0");
  }
}
