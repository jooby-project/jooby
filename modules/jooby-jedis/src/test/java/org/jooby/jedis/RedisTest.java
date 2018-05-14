package org.jooby.jedis;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Provider;
import java.net.URI;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Redis.class, JedisPool.class, GenericObjectPoolConfig.class})
public class RedisTest {

  private Block onManaged = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(isA(Throwing.Runnable.class))).andReturn(env);
    expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"));
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.serviceKey()).andReturn(new Env.ServiceKey());
        })
        .expect(unit -> {
          GenericObjectPoolConfig poolConfig = unit
              .mockConstructor(GenericObjectPoolConfig.class);
          poolConfig.setBlockWhenExhausted(true);
          poolConfig
              .setEvictionPolicyClassName("org.apache.commons.pool2.impl.DefaultEvictionPolicy");
          poolConfig.setJmxEnabled(false);
          poolConfig.setJmxNamePrefix("redis-pool");
          poolConfig.setLifo(true);
          poolConfig.setMaxIdle(10);
          poolConfig.setMaxTotal(128);
          poolConfig.setMaxWaitMillis(-1);
          poolConfig.setMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setMinIdle(10);
          poolConfig.setNumTestsPerEvictionRun(3);
          poolConfig.setSoftMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setTestOnBorrow(false);
          poolConfig.setTestOnReturn(false);
          poolConfig.setTestWhileIdle(false);
          poolConfig.setTimeBetweenEvictionRunsMillis(-1);

          JedisPool pool = unit.mockConstructor(
              JedisPool.class,
              new Class[]{GenericObjectPoolConfig.class, URI.class, int.class},
              poolConfig, URI.create("redis://localhost:6780"), 2000);

          AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
          jpABB.toInstance(pool);
          jpABB.toInstance(pool);

          AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(jABB);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(jABB);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(JedisPool.class))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class))).andReturn(jABB);
          expect(binder.bind(Key.get(JedisPool.class, Names.named("db")))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class, Names.named("db")))).andReturn(jABB);
        })
        .expect(onManaged)
        .run(unit -> {
          new Redis().configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldGetJedisInstance() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"));
    new MockUnit(Env.class, Binder.class, Jedis.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.serviceKey()).andReturn(new Env.ServiceKey());
        })
        .expect(unit -> {
          GenericObjectPoolConfig poolConfig = unit
              .mockConstructor(GenericObjectPoolConfig.class);
          poolConfig.setBlockWhenExhausted(true);
          poolConfig
              .setEvictionPolicyClassName(
                  "org.apache.commons.pool2.impl.DefaultEvictionPolicy");
          poolConfig.setJmxEnabled(false);
          poolConfig.setJmxNamePrefix("redis-pool");
          poolConfig.setLifo(true);
          poolConfig.setMaxIdle(10);
          poolConfig.setMaxTotal(128);
          poolConfig.setMaxWaitMillis(-1);
          poolConfig.setMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setMinIdle(10);
          poolConfig.setNumTestsPerEvictionRun(3);
          poolConfig.setSoftMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setTestOnBorrow(false);
          poolConfig.setTestOnReturn(false);
          poolConfig.setTestWhileIdle(false);
          poolConfig.setTimeBetweenEvictionRunsMillis(-1);

          JedisPool pool = unit.mockConstructor(
              JedisPool.class,
              new Class[]{GenericObjectPoolConfig.class, URI.class, int.class},
              poolConfig, URI.create("redis://localhost:6780"), 2000);

          expect(pool.getResource()).andReturn(unit.get(Jedis.class));

          AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
          jpABB.toInstance(pool);
          jpABB.toInstance(pool);

          AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(jABB);
          expect(jABB.toProvider(unit.capture(Provider.class))).andReturn(jABB);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(JedisPool.class))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class))).andReturn(jABB);
          expect(binder.bind(Key.get(JedisPool.class, Names.named("db")))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class, Names.named("db")))).andReturn(jABB);
        })
        .expect(onManaged)
        .run(unit -> {
          new Redis().configure(unit.get(Env.class), config, unit.get(Binder.class));
        }, unit -> {
          assertEquals(unit.get(Jedis.class), unit.captured(Provider.class).iterator().next()
              .get());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jedisConfigOverride() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"))
        .withValue("jedis.db.maxTotal", ConfigValueFactory.fromAnyRef(8));
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.serviceKey()).andReturn(new Env.ServiceKey());
        })
        .expect(unit -> {
          GenericObjectPoolConfig poolConfig = unit
              .mockConstructor(GenericObjectPoolConfig.class);
          poolConfig.setBlockWhenExhausted(true);
          poolConfig
              .setEvictionPolicyClassName(
                  "org.apache.commons.pool2.impl.DefaultEvictionPolicy");
          poolConfig.setJmxEnabled(false);
          poolConfig.setJmxNamePrefix("redis-pool");
          poolConfig.setLifo(true);
          poolConfig.setMaxIdle(10);
          poolConfig.setMaxTotal(8);
          poolConfig.setMaxWaitMillis(-1);
          poolConfig.setMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setMinIdle(10);
          poolConfig.setNumTestsPerEvictionRun(3);
          poolConfig.setSoftMinEvictableIdleTimeMillis(1800000L);
          poolConfig.setTestOnBorrow(false);
          poolConfig.setTestOnReturn(false);
          poolConfig.setTestWhileIdle(false);
          poolConfig.setTimeBetweenEvictionRunsMillis(-1);

          JedisPool pool = unit.mockConstructor(
              JedisPool.class,
              new Class[]{GenericObjectPoolConfig.class, URI.class, int.class},
              poolConfig, URI.create("redis://localhost:6780"), 2000);

          AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
          jpABB.toInstance(pool);
          jpABB.toInstance(pool);

          AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(jABB);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(jABB);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(JedisPool.class))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class))).andReturn(jABB);
          expect(binder.bind(Key.get(JedisPool.class, Names.named("db")))).andReturn(jpABB);
          expect(binder.bind(Key.get(Jedis.class, Names.named("db")))).andReturn(jABB);
        })
        .expect(onManaged)
        .run(unit -> {
          new Redis().configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @Test
  public void defaultConfig() throws Exception {
    assertEquals(jedisConfig(), new Redis().config());
  }

  private Config jedisConfig() {
    return ConfigFactory.parseResources(getClass(), "jedis.conf");
  }
}
