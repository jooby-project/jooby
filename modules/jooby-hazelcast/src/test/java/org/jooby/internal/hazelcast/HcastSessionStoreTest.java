package org.jooby.internal.hazelcast;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jooby.Session;
import org.jooby.hazelcast.HcastSessionStore;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@SuppressWarnings("unchecked")
public class HcastSessionStoreTest {

  private Block sessions = unit -> {
    HazelcastInstance hcast = unit.get(HazelcastInstance.class);
    expect(hcast.getMap("sessions")).andReturn(unit.get(IMap.class));
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(HazelcastInstance.class, IMap.class)
        .expect(sessions)
        .run(unit -> {
          new HcastSessionStore(unit.get(HazelcastInstance.class), "sessions", "30m");
        });
  }

  @Test
  public void defaultsNoTimeout() throws Exception {
    new MockUnit(HazelcastInstance.class, IMap.class)
        .expect(sessions)
        .run(unit -> {
          new HcastSessionStore(unit.get(HazelcastInstance.class), "sessions", "0");
        });
  }

  @Test
  public void get() throws Exception {
    Map<String, String> sessionMap = Maps
        .newHashMap(
            ImmutableMap.of("_accessedAt", "1", "_createdAt", "2", "_savedAt", "3", "k", "v"));
    new MockUnit(HazelcastInstance.class, IMap.class, Session.class, Session.Builder.class)
        .expect(sessions)
        .expect(unit -> {
          Session.Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
          expect(builder.accessedAt(1)).andReturn(builder);
          expect(builder.createdAt(2)).andReturn(builder);
          expect(builder.savedAt(3)).andReturn(builder);
          expect(builder.set(ImmutableMap.of("k", "v"))).andReturn(builder);
          expect(builder.build()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          IMap<String, Object> session = unit.get(IMap.class);
          expect(session.get("sid")).andReturn(sessionMap);
        })
        .run(unit -> {
          HcastSessionStore store = new HcastSessionStore(unit.get(HazelcastInstance.class),
              "sessions", "30m");
          assertEquals(unit.get(Session.class), store.get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void getNoSession() throws Exception {
    new MockUnit(HazelcastInstance.class, IMap.class, Session.class, Session.Builder.class)
        .expect(sessions)
        .expect(unit -> {
          Session.Builder builder = unit.get(Session.Builder.class);
          expect(builder.sessionId()).andReturn("sid");
        })
        .expect(unit -> {
          IMap<String, Object> session = unit.get(IMap.class);
          expect(session.get("sid")).andReturn(null);
        })
        .run(unit -> {
          HcastSessionStore store = new HcastSessionStore(unit.get(HazelcastInstance.class),
              "sessions", "30m");
          assertEquals(null, store.get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void save() throws Exception {
    Map<String, String> sessionMap = ImmutableMap.of("k", "v");
    new MockUnit(HazelcastInstance.class, IMap.class, Session.class)
        .expect(sessions)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.attributes()).andReturn(sessionMap);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
          expect(session.id()).andReturn("sid");
        })
        .expect(unit -> {
          IMap<String, Object> session = unit.get(IMap.class);
          session.set("sid",
              ImmutableMap.of("_createdAt", "1", "_accessedAt", "2", "_savedAt", "3", "k", "v"),
              1800, TimeUnit.SECONDS);
        })
        .run(unit -> {
          HcastSessionStore store = new HcastSessionStore(unit.get(HazelcastInstance.class),
              "sessions", "30m");
          store.save(unit.get(Session.class));
        });
  }

  @Test
  public void create() throws Exception {
    Map<String, String> sessionMap = ImmutableMap.of("k", "v");
    new MockUnit(HazelcastInstance.class, IMap.class, Session.class)
        .expect(sessions)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.attributes()).andReturn(sessionMap);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
          expect(session.id()).andReturn("sid");
        })
        .expect(unit -> {
          IMap<String, Object> session = unit.get(IMap.class);
          session.set("sid",
              ImmutableMap.of("_createdAt", "1", "_accessedAt", "2", "_savedAt", "3", "k", "v"),
              1800, TimeUnit.SECONDS);
        })
        .run(unit -> {
          HcastSessionStore store = new HcastSessionStore(unit.get(HazelcastInstance.class),
              "sessions", "30m");
          store.create(unit.get(Session.class));
        });
  }

  @Test
  public void remove() throws Exception {
    new MockUnit(HazelcastInstance.class, IMap.class, Session.class)
        .expect(sessions)
        .expect(unit -> {
          IMap<String, Object> session = unit.get(IMap.class);
          expect(session.remove("sid")).andReturn(null);
        })
        .run(unit -> {
          HcastSessionStore store = new HcastSessionStore(unit.get(HazelcastInstance.class),
              "sessions", "30m");
          store.delete("sid");
        });
  }

}
