/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.jedis;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jooby.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;

class RedisProvider implements Managed {

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

}
