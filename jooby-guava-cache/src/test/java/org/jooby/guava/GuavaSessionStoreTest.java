package org.jooby.guava;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.cache.Cache;

public class GuavaSessionStoreTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Cache.class)
        .run(unit -> {
          new GuavaSessionStore(unit.get(Cache.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void get() throws Exception {
    new MockUnit(Cache.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("sid");

          Cache cache = unit.get(Cache.class);
          expect(cache.getIfPresent("sid")).andReturn(unit.get(Session.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Session.class), new GuavaSessionStore(unit.get(Cache.class))
              .get(unit.get(Session.Builder.class)));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void save() throws Exception {
    new MockUnit(Cache.class, Session.class)
        .expect(unit -> {
          Session sb = unit.get(Session.class);
          expect(sb.id()).andReturn("sid");

          Cache cache = unit.get(Cache.class);
          cache.put("sid", unit.get(Session.class));
        })
        .run(unit -> {
          new GuavaSessionStore(unit.get(Cache.class))
              .save(unit.get(Session.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void create() throws Exception {
    new MockUnit(Cache.class, Session.class)
        .expect(unit -> {
          Session sb = unit.get(Session.class);
          expect(sb.id()).andReturn("sid");

          Cache cache = unit.get(Cache.class);
          cache.put("sid", unit.get(Session.class));
        })
        .run(unit -> {
          new GuavaSessionStore(unit.get(Cache.class))
              .create(unit.get(Session.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void delete() throws Exception {
    new MockUnit(Cache.class)
        .expect(unit -> {
          Cache cache = unit.get(Cache.class);
          cache.invalidate("sid");
        })
        .run(unit -> {
          new GuavaSessionStore(unit.get(Cache.class))
              .delete("sid");
        });
  }

}
