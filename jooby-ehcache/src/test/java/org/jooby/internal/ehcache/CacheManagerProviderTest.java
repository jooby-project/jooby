package org.jooby.internal.ehcache;

import static org.junit.Assert.assertEquals;
import net.sf.ehcache.CacheManager;

import org.jooby.MockUnit;
import org.jooby.internal.ehcache.CacheManagerProvider;
import org.junit.Test;

public class CacheManagerProviderTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(CacheManager.class)
        .run(unit -> {
          new CacheManagerProvider(unit.get(CacheManager.class));
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(CacheManager.class)
        .run(unit -> {
          assertEquals(unit.get(CacheManager.class),
              new CacheManagerProvider(unit.get(CacheManager.class)).get());
        });
  }

  @Test
  public void startNoop() throws Exception {
    new MockUnit(CacheManager.class)
        .run(unit -> {
          new CacheManagerProvider(unit.get(CacheManager.class)).start();
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(CacheManager.class)
        .expect(unit -> {
          CacheManager cm = unit.get(CacheManager.class);
          cm.shutdown();
        })
        .run(unit -> {
          CacheManagerProvider cm = new CacheManagerProvider(unit.get(CacheManager.class));
          cm.stop();
          // ignored
            cm.stop();
          });
  }

}
