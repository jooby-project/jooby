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
package org.jooby.internal.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheDecoratorFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExceptionHandlerFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheExtensionFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;

import com.typesafe.config.Config;

public class CacheConfigurationBuilder extends EhCacheBuilder {

  private String path;

  private CacheConfiguration cache;

  public CacheConfigurationBuilder(final String name) {
    this.path = "ehcache.cache." + name;
    this.cache = new CacheConfiguration();
    cache.setName(name);
  }

  public CacheConfiguration build(final Config config) {

    sms(path, config, "cacheLoaderTimeout", cache::setCacheLoaderTimeoutMillis);

    slong(path, config, "cacheLoaderTimeoutMillis", cache::setCacheLoaderTimeoutMillis);

    sbool(path, config, "clearOnFlush", cache::setClearOnFlush);

    sbool(path, config, "copyOnRead", cache::setCopyOnRead);

    sbool(path, config, "copyOnWrite", cache::setCopyOnWrite);

    sint(path, config, "diskAccessStripes", cache::setDiskAccessStripes);

    sseconds(path, config, "diskExpiryThreadInterval", cache::setDiskExpiryThreadIntervalSeconds);

    slong(path, config, "diskExpiryThreadIntervalSeconds",
        cache::setDiskExpiryThreadIntervalSeconds);

    sint(path, config, "diskSpoolBufferSizeMB", cache::setDiskSpoolBufferSizeMB);

    sbool(path, config, "eternal", cache::setEternal);

    sbool(path, config, "logging", cache::setLogging);

    sbytes(path, config, "maxBytesLocalDisk", cache::setMaxBytesLocalDisk);

    sbytes(path, config, "maxBytesLocalHeap", cache::setMaxBytesLocalHeap);

    sbytes(path, config, "maxBytesLocalOffHeap", cache::setMaxBytesLocalOffHeap);

    slong(path, config, "maxElementsInMemory", cache::setMaxEntriesLocalHeap);

    slong(path, config, "maxElementsOnDisk", cache::setMaxEntriesLocalDisk);

    slong(path, config, "maxEntriesInCache", cache::setMaxEntriesInCache);

    slong(path, config, "maxEntriesLocalDisk", cache::setMaxEntriesLocalDisk);

    slong(path, config, "maxEntriesLocalHeap", cache::setMaxEntriesLocalHeap);

    sbytes(path, config, "maxMemoryOffHeap", cache::setMaxBytesLocalOffHeap);

    sstr(path, config, "memoryStoreEvictionPolicy", cache::setMemoryStoreEvictionPolicy);

    sbool(path, config, "overflowToOffHeap", cache::setOverflowToOffHeap);

    sseconds(path, config, "timeToIdle", cache::setTimeToIdleSeconds);

    slong(path, config, "timeToIdleSeconds", cache::setTimeToIdleSeconds);

    sseconds(path, config, "timeToLive", cache::setTimeToLiveSeconds);

    slong(path, config, "timeToLiveSeconds", cache::setTimeToLiveSeconds);

    sstr(path, config, "transactionalMode", cache::setTransactionalMode);

    sconf(path, config, "persistence", this::persistence);

    sconf(path, config, "bootstrapCacheLoaderFactory", this::bootstrapCacheLoaderFactory);

    sconf(path, config, "cacheDecoratorFactory", this::cacheDecoratorFactory);

    sconf(path, config, "cacheEventListenerFactory", this::cacheEventListenerFactory);

    sconf(path, config, "cacheExceptionHandlerFactory", this::cacheExceptionHandlerFactory);

    sconf(path, config, "cacheExtensionFactory", this::cacheExtensionFactory);

    sconf(path, config, "cacheLoaderFactory", this::cacheLoaderFactory);

    sconf(path, config, "cacheWriter", this::cacheWriter);

    sstr(path, config, "pinning.store", pinning ->
        cache.addPinning(new PinningConfiguration().store(pinning)));

    sconf(path, config, "sizeOfPolicy", this::sizeOfPolicy);

    sconf(path, config, "terracotta", this::terracota);

    return cache;
  }

  private void cacheLoaderFactory(final Config conf) {
    if (conf.hasPath("class")) {
      cache.addCacheLoaderFactory(newFactory(path + ".cacheLoaderFactory", conf,
          CacheLoaderFactoryConfiguration::new));
    } else {
      each(conf, (name, c) -> {
        cache.addCacheLoaderFactory(
            newFactory(path + ".cacheLoaderFactory." + name, c,
                CacheLoaderFactoryConfiguration::new)
            );
      });
    }
  }

  private void persistence(final Config conf) {
    PersistenceConfiguration persistence = new PersistenceConfiguration();

    sstr(path + ".persistence", conf, "strategy", persistence::setStrategy);
    sbool(path + ".persistence", conf, "synchronousWrites", persistence::setSynchronousWrites);

    cache.addPersistence(persistence);
  }

  private void bootstrapCacheLoaderFactory(final Config conf) {
    cache.addBootstrapCacheLoaderFactory(
        newFactory(path + ".bootstrapCacheLoaderFactory", conf,
            BootstrapCacheLoaderFactoryConfiguration::new)
        );
  }

  private void cacheDecoratorFactory(final Config conf) {
    if (conf.hasPath("class")) {
      cache.addCacheDecoratorFactory(
          newFactory(path + ".cacheDecoratorFactory", conf,
              CacheDecoratorFactoryConfiguration::new)
          );
    } else {
      each(conf, (name, decoconf) -> {
        cache.addCacheDecoratorFactory(
            newFactory(path + ".cacheDecoratorFactory." + name, decoconf,
                CacheDecoratorFactoryConfiguration::new)
            );
      });
    }
  }

  private void sizeOfPolicy(final Config conf) {
    cache.addSizeOfPolicy(sizeOfPolicy(path + ".sizeOfPolicy", conf));
  }

  private void cacheExtensionFactory(final Config conf) {
    if (conf.hasPath("class")) {
      cache.addCacheExtensionFactory(newFactory(path + ".cacheExtensionFactory", conf,
          CacheExtensionFactoryConfiguration::new));
    } else {
      each(conf, (name, decoconf) -> {
        cache.addCacheExtensionFactory(
            newFactory(path + ".cacheExtensionFactory." + name, decoconf,
                CacheExtensionFactoryConfiguration::new)
            );
      });
    }
  }

  private void cacheEventListenerFactory(final Config conf) {
    if (conf.hasPath("class")) {
      cache.addCacheEventListenerFactory(
          newCacheEventListenerFactory(path + ".cacheEventListenerFactory", conf));
    } else {
      each(conf, (name, c) ->
          cache.addCacheEventListenerFactory(
              newCacheEventListenerFactory(path + ".cacheEventListenerFactory" + name, c)
              ));
    }
  }

  private void cacheExceptionHandlerFactory(final Config conf) {
    cache.addCacheExceptionHandlerFactory(
        newFactory(path + ".cacheExceptionHandlerFactory", conf,
            CacheExceptionHandlerFactoryConfiguration::new)
        );
  }

  private void cacheWriter(final Config conf) {

    String path = this.path + ".cacheWriter";

    CacheWriterConfiguration writer = new CacheWriterConfiguration();

    sint(path, conf, "maxWriteDelay", writer::setMaxWriteDelay);

    sint(path, conf, "minWriteDelay", writer::setMinWriteDelay);

    sbool(path, conf, "notifyListenersOnException", writer::setNotifyListenersOnException);

    sint(path, conf, "rateLimitPerSecond", writer::setRateLimitPerSecond);

    siseconds(path, conf, "retryAttemptDelay", writer::setRetryAttemptDelaySeconds);

    sint(path, conf, "retryAttemptDelaySeconds", writer::setRetryAttemptDelaySeconds);

    sint(path, conf, "retryAttempts", writer::setRetryAttempts);

    sbool(path, conf, "writeBatching", writer::setWriteBatching);

    sint(path, conf, "writeBatchSize", writer::setWriteBatchSize);

    sint(path, conf, "writeBehindConcurrency", writer::setWriteBehindConcurrency);

    sint(path, conf, "writeBehindMaxQueueSize", writer::setWriteBehindMaxQueueSize);

    sbool(path, conf, "writeCoalescing", writer::setWriteCoalescing);

    sstr(path, conf, "writeMode", writer::setWriteMode);

    cache.addCacheWriter(writer);
  }

  private void terracota(final Config config) {
    TerracottaConfiguration terracota = new TerracottaConfiguration();

    String path = this.path + ".terracotta";

    sconf(path, config, "nonstop", conf -> terracota.addNonstop(nonstop(path + ".nonstop", conf)));

    sbool(path, config, "cacheXA", terracota::setCacheXA);

    sbool(path, config, "clustered", terracota::setClustered);

    sbool(path, config, "coherent", coherent ->
        terracota.setConsistency(coherent ? Consistency.STRONG : Consistency.EVENTUAL));

    sbool(path, config, "compressionEnabled", terracota::setCompressionEnabled);

    sint(path, config, "concurrency", terracota::setConcurrency);

    sstr(path, config, "consistency", terracota::setConsistency);

    sbool(path, config, "localCacheEnabled", terracota::setLocalCacheEnabled);

    sbool(path, config, "localKeyCache", terracota::setLocalKeyCache);

    sint(path, config, "localKeyCacheSize", terracota::setLocalKeyCacheSize);

    sbool(path, config, "orphanEviction", terracota::setOrphanEviction);

    sint(path, config, "orphanEvictionPeriod", terracota::setOrphanEvictionPeriod);

    sbool(path, config, "synchronousWrites", terracota::setSynchronousWrites);

    cache.addTerracotta(terracota);
  }

  private NonstopConfiguration nonstop(final String path, final Config config) {
    NonstopConfiguration nonstop = new NonstopConfiguration();

    sconf(path, config, "timeout", conf -> nonstop.addTimeoutBehavior(timeout(conf)));

    sms(path, config, "searchTimeoutMillis", nonstop::searchTimeoutMillis);

    sint(path, config, "bulkOpsTimeoutMultiplyFactor", nonstop::setBulkOpsTimeoutMultiplyFactor);

    sbool(path, config, "enabled", nonstop::setEnabled);

    sbool(path, config, "immediateTimeout", nonstop::setImmediateTimeout);

    sms(path, config, "timeoutMillis", nonstop::searchTimeoutMillis);

    return nonstop;
  }

  private TimeoutBehaviorConfiguration timeout(final Config config) {
    TimeoutBehaviorConfiguration timeout = new TimeoutBehaviorConfiguration();

    String sep = ";";
    timeout.setProperties(toPropertiesLine(config.withoutPath("type"), sep));
    timeout.setPropertySeparator(sep);

    timeout.setType(config.getString("type"));

    return timeout;
  }

  private CacheEventListenerFactoryConfiguration newCacheEventListenerFactory(
      final String path, final Config config) {
    CacheEventListenerFactoryConfiguration factory = newFactory(path,
        config.withoutPath("listenFor"),
        CacheEventListenerFactoryConfiguration::new);
    sstr(path, config, "listenFor", factory::listenFor);
    return factory;
  }

}
