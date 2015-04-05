package org.jooby.jedis;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.jooby.MockUnit;
import org.jooby.Session;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class RedisSessionStoreTest {

  @Test
  public void save() throws Exception {
    Map<String, String> attrs = ImmutableMap.of("x", "X");
    Map<String, String> attrsToSave = ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        );
    new MockUnit(JedisPool.class, Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("1234");
          expect(session.attributes()).andReturn(attrs);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hmset("sessions:1234", attrsToSave)).andReturn("sessions:1234");
          expect(jedis.expire("sessions:1234", 1800)).andReturn(1L);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          new RedisSessionStore(unit.get(JedisPool.class), "sessions", "30m")
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void saveNoTimeout() throws Exception {
    Map<String, String> attrs = ImmutableMap.of("x", "X");
    Map<String, String> attrsToSave = ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        );
    new MockUnit(JedisPool.class, Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("1234");
          expect(session.attributes()).andReturn(attrs);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hmset("sessions:1234", attrsToSave)).andReturn("sessions:1234");
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          new RedisSessionStore(unit.get(JedisPool.class), "sessions", "0")
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void create() throws Exception {
    Map<String, String> attrs = ImmutableMap.of("x", "X");
    Map<String, String> attrsToSave = ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        );
    new MockUnit(JedisPool.class, Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("1234");
          expect(session.attributes()).andReturn(attrs);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hmset("sessions:1234", attrsToSave)).andReturn("sessions:1234");
          expect(jedis.expire("sessions:1234", 1800)).andReturn(1L);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          new RedisSessionStore(unit.get(JedisPool.class), "sessions", "30m")
              .create(unit.get(Session.class));
        });
  }

  @Test
  public void saveTimeoutInSecs() throws Exception {
    Map<String, String> attrs = ImmutableMap.of("x", "X");
    Map<String, String> attrsToSave = ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        );
    new MockUnit(JedisPool.class, Session.class)
        .expect(unit -> {
          Session session = unit.get(Session.class);
          expect(session.id()).andReturn("1234");
          expect(session.attributes()).andReturn(attrs);
          expect(session.createdAt()).andReturn(1L);
          expect(session.accessedAt()).andReturn(2L);
          expect(session.savedAt()).andReturn(3L);
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hmset("sessions:1234", attrsToSave)).andReturn("sessions:1234");
          expect(jedis.expire("sessions:1234", 30)).andReturn(1L);

          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          new RedisSessionStore(unit.get(JedisPool.class), "sessions", 30)
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(JedisPool.class, Session.class)
        .expect(unit -> {

          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.del("sessions:1234")).andReturn(1L);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          new RedisSessionStore(unit.get(JedisPool.class), "sessions", "30m")
              .delete("1234");
        });
  }

  @Test
  public void get() throws Exception {
    Map<String, String> attrs = Maps.newHashMap(ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        ));

    new MockUnit(JedisPool.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");
          expect(sb.accessedAt(2)).andReturn(sb);
          expect(sb.createdAt(1)).andReturn(sb);
          expect(sb.savedAt(3)).andReturn(sb);
          expect(sb.set(ImmutableMap.of("x", "X"))).andReturn(sb);
          expect(sb.build()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hgetAll("sessions:1234")).andReturn(attrs);
          expect(jedis.expire("sessions:1234", 1800)).andReturn(1L);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          assertEquals(unit.get(Session.class), new RedisSessionStore(
              unit.get(JedisPool.class), "sessions", "30m")
              .get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void getNoTimeout() throws Exception {
    Map<String, String> attrs = Maps.newHashMap(ImmutableMap
        .of(
            "x", "X",
            "_accessedAt", "2",
            "_createdAt", "1",
            "_savedAt", "3"
        ));

    new MockUnit(JedisPool.class, Session.class, Session.Builder.class)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");
          expect(sb.accessedAt(2)).andReturn(sb);
          expect(sb.createdAt(1)).andReturn(sb);
          expect(sb.savedAt(3)).andReturn(sb);
          expect(sb.set(ImmutableMap.of("x", "X"))).andReturn(sb);
          expect(sb.build()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hgetAll("sessions:1234")).andReturn(attrs);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          assertEquals(unit.get(Session.class), new RedisSessionStore(
              unit.get(JedisPool.class), "sessions", "0")
              .get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void getNullExpired() throws Exception {
    Map<String, String> attrs = null;

    new MockUnit(JedisPool.class, Session.Builder.class)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hgetAll("sessions:1234")).andReturn(attrs);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          assertEquals(null, new RedisSessionStore(
              unit.get(JedisPool.class), "sessions", "30m")
              .get(unit.get(Session.Builder.class)));
        });
  }

  @Test
  public void getEmptyExpired() throws Exception {
    Map<String, String> attrs = Collections.emptyMap();

    new MockUnit(JedisPool.class, Session.Builder.class)
        .expect(unit -> {
          Session.Builder sb = unit.get(Session.Builder.class);
          expect(sb.sessionId()).andReturn("1234");
        })
        .expect(unit -> {
          Jedis jedis = unit.mock(Jedis.class);
          expect(jedis.hgetAll("sessions:1234")).andReturn(attrs);
          jedis.close();

          JedisPool pool = unit.get(JedisPool.class);
          expect(pool.getResource()).andReturn(jedis);
        })
        .run(unit -> {
          assertEquals(null, new RedisSessionStore(
              unit.get(JedisPool.class), "sessions", "30m")
              .get(unit.get(Session.Builder.class)));
        });
  }

}
