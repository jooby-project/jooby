package org.jooby.jedis;

import static org.easymock.EasyMock.expect;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RedisProvider.class, LoggerFactory.class })
public class RedisProviderTest {

  @Test
  public void defaults() throws Exception {
    URI uri = URI.create("redis://localhost:6789");
    new MockUnit(JedisPool.class, GenericObjectPoolConfig.class, Logger.class)
        .expect(unit -> {
          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Redis.class)).andReturn(unit.get(Logger.class));
        })
        .run(unit -> {
          new RedisProvider(unit.get(JedisPool.class), uri, unit
              .get(GenericObjectPoolConfig.class));
        });
  }

  @Test
  public void start() throws Exception {
    URI uri = URI.create("redis://localhost:6789");
    new MockUnit(JedisPool.class, GenericObjectPoolConfig.class, Logger.class)
        .expect(unit -> {
          Logger logger = unit.get(Logger.class);
          logger.info("Starting {}", uri);
          expect(logger.isDebugEnabled()).andReturn(false);
        })
        .expect(unit -> {
          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Redis.class)).andReturn(unit.get(Logger.class));
        })
        .run(unit -> {
          new RedisProvider(unit.get(JedisPool.class), uri, unit
              .get(GenericObjectPoolConfig.class)).start();
        });
  }

  @Test
  public void startDebug() throws Exception {
    URI uri = URI.create("redis://localhost:6789");
    new MockUnit(JedisPool.class, GenericObjectPoolConfig.class, Logger.class)
        .expect(unit -> {
          Logger logger = unit.get(Logger.class);
          logger.info("Starting {}", uri);
          expect(logger.isDebugEnabled()).andReturn(true);

          GenericObjectPoolConfig config = unit.get(GenericObjectPoolConfig.class);

          expect(config.getBlockWhenExhausted()).andReturn(true);
          logger.debug("  blockWhenExhausted = {}", true);

          expect(config.getEvictionPolicyClassName()).andReturn("X");
          logger.debug("  evictionPolicyClassName = {}", "X");

          expect(config.getJmxEnabled()).andReturn(false);
          logger.debug("  jmxEnabled = {}", false);

          expect(config.getJmxNamePrefix()).andReturn("prefix");
          logger.debug("  jmxNamePrefix = {}", "prefix");

          expect(config.getLifo()).andReturn(true);
          logger.debug("  lifo = {}", true);

          expect(config.getMaxIdle()).andReturn(3);
          logger.debug("  maxIdle = {}", 3);

          expect(config.getMaxTotal()).andReturn(2);
          logger.debug("  maxTotal = {}", 2);

          expect(config.getMaxWaitMillis()).andReturn(1000L);
          logger.debug("  maxWaitMillis = {}", 1000L);

          expect(config.getMinEvictableIdleTimeMillis()).andReturn(500L);
          logger.debug("  minEvictableIdleTimeMillis = {}", 500L);

          expect(config.getMinIdle()).andReturn(2);
          logger.debug("  minIdle = {}", 2);

          expect(config.getNumTestsPerEvictionRun()).andReturn(6);
          logger.debug("  numTestsPerEvictionRun = {}", 6);

          expect(config.getSoftMinEvictableIdleTimeMillis()).andReturn(45L);
          logger.debug("  softMinEvictableIdleTimeMillis = {}", 45L);

          expect(config.getTestOnBorrow()).andReturn(true);
          logger.debug("  testOnBorrow = {}", true);

          expect(config.getTestOnReturn()).andReturn(true);
          logger.debug("  testOnReturn = {}", true);

          expect(config.getTimeBetweenEvictionRunsMillis()).andReturn(65L);
          logger.debug("  timeBetweenEvictionRunsMillis = {}", 65L);
        })
        .expect(unit -> {
          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Redis.class)).andReturn(unit.get(Logger.class));
        })
        .run(unit -> {
          new RedisProvider(unit.get(JedisPool.class), uri, unit
              .get(GenericObjectPoolConfig.class)).start();
        });
  }

  @Test
  public void stop() throws Exception {
    URI uri = URI.create("redis://localhost:6789");
    new MockUnit(JedisPool.class, GenericObjectPoolConfig.class, Logger.class)
        .expect(unit -> {
          Logger logger = unit.get(Logger.class);
          logger.info("Stopping {}", uri);
        })
        .expect(unit -> {
          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(Redis.class)).andReturn(unit.get(Logger.class));
        })
        .expect(unit -> {
          JedisPool jedis = unit.get(JedisPool.class);
          jedis.destroy();
        })
        .run(unit -> {
          RedisProvider redis = new RedisProvider(unit.get(JedisPool.class), uri, unit
              .get(GenericObjectPoolConfig.class));
          redis.stop();
          // 2nd call ignored
            redis.stop();
          });
  }

}
