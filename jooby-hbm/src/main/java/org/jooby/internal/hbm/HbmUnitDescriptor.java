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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Provider;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

public class HbmUnitDescriptor implements PersistenceUnitDescriptor {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private ClassLoader loader;

  private Provider<DataSource> dataSourceHolder;

  private Config config;

  private List<URL> jars = Collections.emptyList();

  private URL unitRoot;

  public HbmUnitDescriptor(final ClassLoader loader, final Provider<DataSource> dataSourceHolder,
      final Config config, final Set<String> pkgs) {
    this.loader = loader;
    this.dataSourceHolder = dataSourceHolder;
    this.config = config;
    List<URL> pkgList = packageToScan(loader, pkgs);
    if (pkgList.size() > 0) {
      unitRoot = pkgList.get(0);
      jars = pkgList.subList(1, pkgList.size());
    }
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
    return unitRoot;
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
    return jars;
  }

  @Override
  public ClassLoader getClassLoader() {
    return loader;
  }

  private List<URL> packageToScan(final ClassLoader loader, final Set<String> pkgs) {
    List<URL> result = new ArrayList<>();
    for (String pkg : pkgs) {
      try {
        Enumeration<URL> resources = loader.getResources(pkg.replace(".", "/"));
        while (resources.hasMoreElements()) {
          URL url = resources.nextElement();
          result.add(url);
        }
      } catch (IOException ex) {
        log.debug("Unable to load package: {}", pkg, ex);
      }
    }
    return result;
  }

}
