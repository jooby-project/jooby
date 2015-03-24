package org.jooby.jedis;

import java.net.URI;

import javax.inject.Provider;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

class RedisProvider implements Provider<JedisPool>, Managed {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Redis.class);

  private JedisPool pool;

  private URI uri;

  private GenericObjectPoolConfig config;

  public RedisProvider(final JedisPool pool, final URI uri, final GenericObjectPoolConfig config) {
    this.pool = pool;
    this.uri = uri;
    this.config = config;
  }

  @Override
  public void start() throws Exception {
    // NOOP
    log.info("Starting {}", uri);
    if (log.isDebugEnabled()) {
      log.debug("  blockWhenExhausted = {}", config.getBlockWhenExhausted());
      log.debug("  evictionPolicyClassName = {}", config.getEvictionPolicyClassName());
      log.debug("  jmxEnabled = {}", config.getJmxEnabled());
      log.debug("  jmxNamePrefix = {}", config.getJmxNamePrefix());
      log.debug("  lifo = {}", config.getLifo());
      log.debug("  maxIdle = {}", config.getMaxIdle());
      log.debug("  maxTotal = {}", config.getMaxTotal());
      log.debug("  maxWaitMillis = {}", config.getMaxWaitMillis());
      log.debug("  minEvictableIdleTimeMillis = {}", config.getMinEvictableIdleTimeMillis());
      log.debug("  minIdle = {}", config.getMinIdle());
      log.debug("  numTestsPerEvictionRun = {}", config.getNumTestsPerEvictionRun());
      log.debug("  softMinEvictableIdleTimeMillis = {}", config.getSoftMinEvictableIdleTimeMillis());
      log.debug("  testOnBorrow = {}", config.getTestOnBorrow());
      log.debug("  testOnReturn = {}", config.getTestOnReturn());
      log.debug("  timeBetweenEvictionRunsMillis = {}", config.getTimeBetweenEvictionRunsMillis());
    }
  }

  @Override
  public void stop() throws Exception {
    if (this.pool != null) {
      log.info("Stopping {}", uri);
      this.pool.destroy();
      this.pool = null;
    }
  }

  @Override
  public JedisPool get() {
    return pool;
  }

}
