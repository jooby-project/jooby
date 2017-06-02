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
package org.jooby.hazelcast;

import static java.util.Objects.requireNonNull;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby.Module;

import com.google.inject.Binder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>hazelcast module</h1>
 * <p>
 * Exports a <a href="http://hazelcast.org">Hazelcast</a> instances and optionally a
 * {@link HcastSessionStore} session store.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Hcast());
 *
 *   get("/:key", req {@literal ->} {
 *     HazelcastInstance hcast = req.require(HazelcastInstance.class);
 *     ...
 *   });
 * }
 * </pre>
 *
 * <h2>configuration</h2>
 * <p>
 * Any property under <code>hazelcast.*</code> will be automatically add it while bootstrapping a
 * {@link HazelcastInstance}.
 * </p>
 *
 * <p>
 * Configuration can be done programmatically via: {@link #doWith(Consumer)}.
 * </p>
 *
 * <pre>
 * {
 *   use(new Hcast()
 *    .doWith(config {@literal ->} {
 *      config.setXxx
 *    })
 *   );
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.9.0
 */
public class Hcast implements Module {

  private BiConsumer<com.hazelcast.config.Config, Config> configurer;

  /**
   * Set a configurer callback.
   *
   * @param configurer A configurer callback.
   * @return This instance.
   */
  public Hcast doWith(final BiConsumer<com.hazelcast.config.Config, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  /**
   * Set a configurer callback.
   *
   * @param configurer A configurer callback.
   * @return This instance.
   */
  public Hcast doWith(final Consumer<com.hazelcast.config.Config> configurer) {
    requireNonNull(configurer, "Configurer callback is required.");
    return doWith((config, conf) -> configurer.accept(config));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    com.hazelcast.config.Config config = new com.hazelcast.config.Config();

    config.setProperties(toProperties(conf.getConfig("hazelcast")));

    if (configurer != null) {
      configurer.accept(config, conf);
    }

    HazelcastInstance hcast = Hazelcast.newHazelcastInstance(config);

    binder.bind(com.hazelcast.config.Config.class).toInstance(config);
    binder.bind(HazelcastInstance.class).toInstance(hcast);
    env.onStop(hcast::shutdown);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "hcast.conf");
  }

  private Properties toProperties(final Config config) {
    Properties properties = new Properties();

    config.withoutPath("session").entrySet().forEach(prop -> {
      properties.setProperty("hazelcast." + prop.getKey(), prop.getValue().unwrapped().toString());
    });

    return properties;
  }

}
