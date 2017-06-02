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

import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;

import com.typesafe.config.Config;

public class ConfigurationBuilder extends EhCacheBuilder {

  private Configuration eh;

  public ConfigurationBuilder() {
    this.eh = new Configuration();
  }

  public Configuration build(final Config config) {

    sint("ehcache", config, "defaultTransactionTimeoutInSeconds",
        eh::setDefaultTransactionTimeoutInSeconds);

    siseconds("ehcache", config, "defaultTransactionTimeout",
        eh::setDefaultTransactionTimeoutInSeconds);

    sbool("ehcache", config, "dynamicConfig", eh::setDynamicConfig);

    sbytes("ehcache", config, "maxBytesLocalDisk", eh::setMaxBytesLocalDisk);

    sbytes("ehcache", config, "maxBytesLocalHeap", eh::setMaxBytesLocalHeap);

    sbytes("ehcache", config, "maxBytesLocalOffHeap", eh::setMaxBytesLocalOffHeap);

    sstr("ehcache", config, "monitoring", eh::setMonitoring);

    sstr("ehcache", config, "name", eh::setName);

    eh.setUpdateCheck(false);

    sconf("ehcache", config, "cacheManagerEventListenerFactory",
        this::cacheManagerEventListenerFactory);

    sconf("ehcache", config, "cacheManagerPeerListenerFactory",
        this::cacheManagerPeerListenerFactory);

    sconf("ehcache", config, "cacheManagerPeerProviderFactory",
        this::cacheManagerPeerProviderFactory);

    sconf("ehcache", config, "diskStore", this::diskStore);

    sconf("ehcache", config, "sizeOfPolicy", this::sizeOfPolicy);

    sconf("ehcache", config, "terracottaConfig", this::terracotaConfig);

    sconf("ehcache", config, "transactionManagerLookup", this::transactionManagerLookup);

    return eh;
  }

  private void cacheManagerEventListenerFactory(final Config conf) {
    eh.addCacheManagerEventListenerFactory(
        newFactory("ehcache.cacheManagerEventListenerFactory", conf, FactoryConfiguration::new)
        );
  }

  private void cacheManagerPeerListenerFactory(final Config conf) {
    if (conf.hasPath("class")) {
      eh.addCacheManagerPeerListenerFactory(
          newFactory("ehcache.cacheManagerPeerListenerFactory", conf, FactoryConfiguration::new)
          );
    } else {
      each(conf, (name, c) -> {
        eh.addCacheManagerPeerListenerFactory(
            newFactory("ehcache.cacheManagerPeerListenerFactory." + name, c,
                FactoryConfiguration::new)
            );
      });
    }
  }

  private void cacheManagerPeerProviderFactory(final Config conf) {
    if (conf.hasPath("class")) {
      eh.addCacheManagerPeerProviderFactory(
          newFactory("ehcache.cacheManagerPeerProviderFactory", conf, FactoryConfiguration::new)
          );
    } else {
      each(conf, (name, c) -> {
        eh.addCacheManagerPeerProviderFactory(
            newFactory("ehcache.cacheManagerPeerProviderFactory." + name, c,
                FactoryConfiguration::new)
            );
      });
    }
  }

  private void diskStore(final Config conf) {
    DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
    diskStore.setPath(conf.getString("path"));
    eh.addDiskStore(diskStore);
  }

  private void sizeOfPolicy(final Config conf) {
    SizeOfPolicyConfiguration sizeOfPolicy = sizeOfPolicy("ehcache.sizeOfPolicy", conf);
    eh.addSizeOfPolicy(sizeOfPolicy);
  }

  private void terracotaConfig(final Config config) {
    TerracottaClientConfiguration terracota = new TerracottaClientConfiguration();

    sbool("ehcache.terracottaConfig", config, "rejoin", terracota::setRejoin);
    sstr("ehcache.terracottaConfig", config, "url", terracota::setUrl);
    sbool("ehcache.terracottaConfig", config, "wanEnabledTSA", terracota::setWanEnabledTSA);

    eh.addTerracottaConfig(terracota);
  }

  private void transactionManagerLookup(final Config conf) {
    eh.addTransactionManagerLookup(
        newFactory("ehcache.transactionManagerLookup", conf, FactoryConfiguration::new)
        );
  }

}
