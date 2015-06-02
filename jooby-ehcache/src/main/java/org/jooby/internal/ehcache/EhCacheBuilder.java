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

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;

import org.jooby.ehcache.Eh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigUtil;
import com.typesafe.config.ConfigValue;

class EhCacheBuilder {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Eh.class);

  protected SizeOfPolicyConfiguration sizeOfPolicy(final String path, final Config conf) {
    SizeOfPolicyConfiguration sizeOfPolicy = new SizeOfPolicyConfiguration();
    sint(path, conf, "maxDepth", sizeOfPolicy::maxDepth);
    sstr(path, conf, "maxDepthExceededBehavior", sizeOfPolicy::maxDepthExceededBehavior);
    return sizeOfPolicy;
  }

  protected <T extends FactoryConfiguration<?>> T newFactory(
      final String path, final Config config, final Supplier<T> supplier) {
    T factory = supplier.get();

    sstr(path, config, "class", factory::setClass);
    String sep = ";";
    factory.setProperties(toPropertiesLine(config.withoutPath("class"), sep));
    factory.setPropertySeparator(";");
    requireNonNull(factory.getFullyQualifiedClassPath(), "No .class found in: " + path);
    return factory;
  }

  protected String toPropertiesLine(final Config properties, final String sep) {
    StringBuilder plainprops = new StringBuilder();
    for (Entry<String, ConfigValue> property : properties.entrySet()) {
      plainprops.append(property.getKey())
          .append("=")
          .append(property.getValue().unwrapped().toString())
          .append(sep);
    }
    if (plainprops.length() > 0) {
      plainprops.setLength(plainprops.length() - sep.length());
    }
    return plainprops.toString();
  }

  protected void sbool(final String path, final Config config, final String name,
      final Consumer<Boolean> setter) {
    if (config.hasPath(name)) {
      boolean value = config.getBoolean(name);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void slong(final String path, final Config config, final String name,
      final Consumer<Long> setter) {
    if (config.hasPath(name)) {
      long value = config.getLong(name);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sint(final String path, final Config config, final String name,
      final Consumer<Integer> setter) {
    if (config.hasPath(name)) {
      int value = config.getInt(name);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void siseconds(final String path, final Config config, final String name,
      final Consumer<Integer> setter) {
    if (config.hasPath(name)) {
      int value = (int) config.getDuration(name, TimeUnit.SECONDS);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sseconds(final String path, final Config config, final String name,
      final Consumer<Long> setter) {
    if (config.hasPath(name)) {
      long value = config.getDuration(name, TimeUnit.SECONDS);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sms(final String path, final Config config, final String name,
      final Consumer<Long> setter) {
    if (config.hasPath(name)) {
      long value = config.getDuration(name, TimeUnit.MILLISECONDS);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sbytes(final String path, final Config config, final String name,
      final Consumer<Long> setter) {
    if (config.hasPath(name)) {
      long value = config.getBytes(name);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sstr(final String path, final Config config, final String name,
      final Consumer<String> setter) {
    if (config.hasPath(name)) {
      String value = config.getString(name);
      log.debug("setting {}.{} = {}", path, name, value);
      setter.accept(value);
    }
  }

  protected void sconf(final String path, final Config config, final String name,
      final Consumer<Config> setter) {
    if (config.hasPath(name)) {
      Config value = config.getConfig(name);
      setter.accept(value);
    }
  }

  protected void each(final Config conf, final BiConsumer<String, Config> callback) {
    for (String name : names(conf)) {
      callback.accept(name, conf.getConfig(name));
    }
  }

  private Iterable<String> names(final Config conf) {
    Set<String> result = new LinkedHashSet<>();
    conf.root().forEach((k, v) -> result.add(ConfigUtil.splitPath(k).get(0)));
    return result;
  }

}
