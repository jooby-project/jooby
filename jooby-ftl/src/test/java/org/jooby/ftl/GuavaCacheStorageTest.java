package org.jooby.ftl;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.internal.ftl.GuavaCacheStorage;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.cache.Cache;

public class GuavaCacheStorageTest {

  @SuppressWarnings("unchecked")
  @Test
  public void clear() throws Exception {
    new MockUnit(Cache.class)
        .expect(unit -> {
          unit.get(Cache.class).invalidateAll();
        })
        .run(unit -> {
          new GuavaCacheStorage(unit.get(Cache.class)).clear();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get() throws Exception {
    String view = "key";
    new MockUnit(Cache.class)
        .expect(unit -> {
          expect(unit.get(Cache.class).getIfPresent(view)).andReturn("value");
        })
        .run(unit -> {
          assertEquals("value", new GuavaCacheStorage(unit.get(Cache.class)).get(view));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void put() throws Exception {
    String view = "key";
    String value = "value";
    new MockUnit(Cache.class)
        .expect(unit -> {
          unit.get(Cache.class).put(view, value);
        })
        .run(unit -> {
          new GuavaCacheStorage(unit.get(Cache.class)).put(view, value);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void remove() throws Exception {
    String view = "key";
    new MockUnit(Cache.class)
        .expect(unit -> {
          unit.get(Cache.class).invalidate(view);
        })
        .run(unit -> {
          new GuavaCacheStorage(unit.get(Cache.class)).remove(view);
        });
  }

}
