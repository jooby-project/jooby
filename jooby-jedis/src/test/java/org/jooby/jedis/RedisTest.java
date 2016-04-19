package org.jooby.jedis;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.inject.Provider;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Redis.class, JedisPool.class, GenericObjectPoolConfig.class })
public class RedisTest {

  private Block onManaged = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(isA(CheckedRunnable.class))).andReturn(env);
    expect(env.onStop(isA(CheckedRunnable.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"));
    new MockUnit(Env.class, Binder.class)
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
              new Class[]{GenericObjectPoolConfig.class, URI.class, int.class },
              poolConfig, URI.create("redis://localhost:6780"), 2000);

          AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
          jpABB.toInstance(pool);

          ScopedBindingBuilder sbbJABB = unit.mock(ScopedBindingBuilder.class);
          sbbJABB.asEagerSingleton();

          AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
          expect(jABB.toProvider(isA(Provider.class))).andReturn(sbbJABB);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(JedisPool.class)).andReturn(jpABB);
          expect(binder.bind(Jedis.class)).andReturn(jABB);
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
        .expect(
            unit -> {
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
                  new Class[]{GenericObjectPoolConfig.class, URI.class, int.class },
                  poolConfig, URI.create("redis://localhost:6780"), 2000);

              expect(pool.getResource()).andReturn(unit.get(Jedis.class));

              AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
              jpABB.toInstance(pool);

              ScopedBindingBuilder sbbJABB = unit.mock(ScopedBindingBuilder.class);
              sbbJABB.asEagerSingleton();

              AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(jABB.toProvider(unit.capture(Provider.class))).andReturn(sbbJABB);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(JedisPool.class)).andReturn(jpABB);
              expect(binder.bind(Jedis.class)).andReturn(jABB);
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
  public void named() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"));
    new MockUnit(Env.class, Binder.class)
        .expect(
            unit -> {
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
                  new Class[]{GenericObjectPoolConfig.class, URI.class, int.class },
                  poolConfig, URI.create("redis://localhost:6780"), 2000);

              LinkedBindingBuilder<JedisPool> jpLBB = unit.mock(LinkedBindingBuilder.class);
              jpLBB.toInstance(pool);

              AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(jpABB.annotatedWith(Names.named("db"))).andReturn(jpLBB);

              ScopedBindingBuilder sbbJABB = unit.mock(ScopedBindingBuilder.class);
              sbbJABB.asEagerSingleton();

              LinkedBindingBuilder<Jedis> jLBB = unit.mock(LinkedBindingBuilder.class);
              expect(jLBB.toProvider(isA(Provider.class))).andReturn(sbbJABB);

              AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(jABB.annotatedWith(Names.named("db"))).andReturn(jLBB);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(JedisPool.class)).andReturn(jpABB);
              expect(binder.bind(Jedis.class)).andReturn(jABB);
            })
        .expect(onManaged)
        .run(unit -> {
          new Redis().named().configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void unnamed() throws Exception {
    Config config = jedisConfig()
        .withValue("db1", ConfigValueFactory.fromAnyRef("redis://localhost:6780"));
    new MockUnit(Env.class, Binder.class)
        .expect(
            unit -> {
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
                  new Class[]{GenericObjectPoolConfig.class, URI.class, int.class },
                  poolConfig, URI.create("redis://localhost:6780"), 2000);

              AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
              jpABB.toInstance(pool);

              ScopedBindingBuilder sbbJABB = unit.mock(ScopedBindingBuilder.class);
              sbbJABB.asEagerSingleton();

              AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(jABB.toProvider(isA(Provider.class))).andReturn(sbbJABB);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(JedisPool.class)).andReturn(jpABB);
              expect(binder.bind(Jedis.class)).andReturn(jABB);
            })
        .expect(onManaged)
        .run(unit -> {
          new Redis("db1").unnamed().configure(unit.get(Env.class), config,
              unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jedisConfigOverride() throws Exception {
    Config config = jedisConfig()
        .withValue("db", ConfigValueFactory.fromAnyRef("redis://localhost:6780"))
        .withValue("jedis.db.maxTotal", ConfigValueFactory.fromAnyRef(8));
    new MockUnit(Env.class, Binder.class)
        .expect(
            unit -> {
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
                  new Class[]{GenericObjectPoolConfig.class, URI.class, int.class },
                  poolConfig, URI.create("redis://localhost:6780"), 2000);

              AnnotatedBindingBuilder<JedisPool> jpABB = unit.mock(AnnotatedBindingBuilder.class);
              jpABB.toInstance(pool);

              ScopedBindingBuilder sbbJABB = unit.mock(ScopedBindingBuilder.class);
              sbbJABB.asEagerSingleton();

              AnnotatedBindingBuilder<Jedis> jABB = unit.mock(AnnotatedBindingBuilder.class);
              expect(jABB.toProvider(isA(Provider.class))).andReturn(sbbJABB);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(JedisPool.class)).andReturn(jpABB);
              expect(binder.bind(Jedis.class)).andReturn(jABB);
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
