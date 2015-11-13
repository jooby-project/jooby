package org.jooby.memcached;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.MemcachedClient;

import org.jooby.Session;
import org.jooby.Session.Builder;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class SpySessionStoreTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(MemcachedClient.class)
        .run(unit -> {
          new SpySessionStore(unit.get(MemcachedClient.class), "sessions:", "30m");
        });
  }

  @Test
  public void get() throws Exception {
    Map<String, String> msession = Maps.newHashMap(
        ImmutableMap.of(
            "_accessedAt", "1",
            "_createdAt", "2",
            "_savedAt", "3",
            "foo", "bar"
            )
        );
    new MockUnit(MemcachedClient.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.get("sessions:sid")).andReturn(msession);

          expect(client.touch("sessions:sid", 1800)).andReturn(null);
        })
        .expect(unit -> {
          Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
          expect(builder.accessedAt(1L)).andReturn(builder);
          expect(builder.createdAt(2L)).andReturn(builder);
          expect(builder.savedAt(3L)).andReturn(builder);
          expect(builder.set(ImmutableMap.of("foo", "bar"))).andReturn(builder);

          expect(builder.build()).andReturn(unit.get(Session.class));
        })
        .run(unit -> {
          Session session = new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "30m"
              ).get(unit.get(Session.Builder.class));
          assertEquals(unit.get(Session.class), session);
        });
  }

  @Test
  public void getNullSession() throws Exception {
    Map<String, String> msession = null;
    new MockUnit(MemcachedClient.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.get("sessions:sid")).andReturn(msession);
        })
        .expect(unit -> {
          Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
        })
        .run(unit -> {
          Session session = new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "30m"
              ).get(unit.get(Session.Builder.class));
          assertEquals(null, session);
        });
  }

  @Test
  public void getEmptySession() throws Exception {
    Map<String, String> msession = Collections.emptyMap();
    new MockUnit(MemcachedClient.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.get("sessions:sid")).andReturn(msession);
        })
        .expect(unit -> {
          Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
        })
        .run(unit -> {
          Session session = new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "30m"
              ).get(unit.get(Session.Builder.class));
          assertEquals(null, session);
        });
  }

  @Test
  public void save() throws Exception {
    Map<String, String> msession = ImmutableMap.of(
        "_accessedAt", "1",
        "_createdAt", "2",
        "_savedAt", "3",
        "foo", "bar"
        );
    new MockUnit(MemcachedClient.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.set("sessions:sid", 1800, msession)).andReturn(null);
        })
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("sid");
          expect(session.createdAt()).andReturn(2L);
          expect(session.accessedAt()).andReturn(1L);
          expect(session.savedAt()).andReturn(3L);
          expect(session.attributes()).andReturn(msession);
        })
        .run(unit -> {
          new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "30m"
            ).save(unit.get(Session.class));
          });
  }

  @Test
  public void create() throws Exception {
    Map<String, String> msession = ImmutableMap.of(
        "_accessedAt", "1",
        "_createdAt", "2",
        "_savedAt", "3",
        "foo", "bar"
        );
    new MockUnit(MemcachedClient.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.set("sessions:sid", 1800, msession)).andReturn(null);
        })
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("sid");
          expect(session.createdAt()).andReturn(2L);
          expect(session.accessedAt()).andReturn(1L);
          expect(session.savedAt()).andReturn(3L);
          expect(session.attributes()).andReturn(msession);
        })
        .run(unit -> {
          new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "1800"
            ).create(unit.get(Session.class));
          });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(MemcachedClient.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          MemcachedClient client = unit.get(MemcachedClient.class);
          expect(client.replace("sessions:sid", 1, new HashMap<>())).andReturn(null);
        })
        .run(unit -> {
          new SpySessionStore(
              unit.get(MemcachedClient.class), "sessions:", "30m"
            ).delete("sid");
          });
  }

}
