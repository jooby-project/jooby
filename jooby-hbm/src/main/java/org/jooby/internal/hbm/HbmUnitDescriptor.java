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
package org.jooby.internal.hbm;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Provider;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import com.typesafe.config.Config;

public class HbmUnitDescriptor implements PersistenceUnitDescriptor {

  private ClassLoader loader;

  private Provider<DataSource> dataSourceHolder;

  private Config config;

  private boolean scan;

  public HbmUnitDescriptor(final ClassLoader loader, final Provider<DataSource> dataSourceHolder,
      final Config config, final boolean scan) {
    this.loader = loader;
    this.dataSourceHolder = dataSourceHolder;
    this.config = config;
    this.scan = scan;
  }

  @SuppressWarnings("unchecked")
  private <T> T property(final String name, final T defaultValue) {
    if (config.hasPath(name)) {
      return (T) config.getAnyRef(name);
    }
    return defaultValue;
  }

  @Override
  public void pushClassTransformer(final List<String> entityClassNames) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isUseQuotedIdentifiers() {
    return property("hibernate.useQuotedIdentifiers", false);
  }

  @Override
  public boolean isExcludeUnlistedClasses() {
    return property("hibernate.excludeUnlistedClasses", false);
  }

  @Override
  public ValidationMode getValidationMode() {
    return ValidationMode.valueOf(property(AvailableSettings.VALIDATION_MODE, "NONE")
        .toUpperCase());
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    return PersistenceUnitTransactionType.RESOURCE_LOCAL;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return SharedCacheMode.valueOf(property(AvailableSettings.SHARED_CACHE_MODE, "UNSPECIFIED"));
  }

  @Override
  public String getProviderClassName() {
    return HibernatePersistenceProvider.class.getName();
  }

  @Override
  public Properties getProperties() {
    Properties $ = new Properties();
    config.getConfig("javax.persistence")
        .entrySet()
        .forEach(e -> $.put("javax.persistence." + e.getKey(), e.getValue().unwrapped()));
    return $;
  }

  @Override
  public URL getPersistenceUnitRootUrl() {
    if (scan) {
      String ns = config.getString("application.ns").replace('.', '/');
      return loader.getResource(ns);
    } else {
      return null;
    }
  }

  @Override
  public Object getNonJtaDataSource() {
    return dataSourceHolder.get();
  }

  @Override
  public String getName() {
    return dataSourceHolder.toString();
  }

  @Override
  public List<String> getMappingFileNames() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getManagedClassNames() {
    return Collections.emptyList();
  }

  @Override
  public Object getJtaDataSource() {
    return null;
  }

  @Override
  public List<URL> getJarFileUrls() {
    return null;
  }

  @Override
  public ClassLoader getClassLoader() {
    return loader;
  }

}
