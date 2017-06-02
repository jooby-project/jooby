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
package org.jooby.ebean;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.internal.ebean.EbeanEnhancer;
import org.jooby.internal.ebean.EbeanManaged;
import org.jooby.jdbc.Jdbc;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import io.ebean.EbeanServer;
import io.ebean.config.ContainerConfig;
import io.ebean.config.ServerConfig;
import javaslang.control.Try.CheckedRunnable;

/**
 * <h1>ebean module</h1>
 * <p>
 * Object-Relational-Mapping via Ebean. It configures and exposes {@link EbeanServer} instances.
 * </p>
 * <p>
 * This module extends {@link Jdbc} module, before going forward, make sure you read the doc of the
 * {@link Jdbc} module first.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>
 * {
 *   use(new Ebeanby().doWith(conf {@literal ->} {
 *    conf.addClass(Pet.class);
 *   }));
 *
 *   get("/pets", req {@literal ->} {
 *     EbeanServer ebean = req.require(EbeanServer.class);
 *     return ebean.createQuery(Pet.class)
 *        .findList();
 *   });
 * }
 * </pre>
 *
 * <p>
 * Usage is pretty straightforward, but of course we need to setup/configure the enhancement.
 * </p>
 *
 * <h2>enhancement</h2>
 * <p>
 * The enhancement process comes in two flavors:
 * </p>
 * <ul>
 * <li>Runtime: via a JVM Agent</li>
 * <li>Build time: via Maven plugin</li>
 * </ul>
 *
 * <h3>recommended setup</h3>
 * <p>
 * The recommended setup consist of setting up both: runtime and build time enhancement.
 * </p>
 *
 * <p>
 * The runtime enhancer increases developer productivity, it let you start your app from IDE
 * and/or <code>mvn jooby:run</code>. All you have to do is to add the agent dependencies to your
 * classpath:
 * </p>
 *
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.avaje.ebeanorm&lt;/groupId&gt;
 *   &lt;artifactId&gt;avaje-ebeanorm-agent&lt;/artifactId&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 *
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.avaje&lt;/groupId&gt;
 *   &lt;artifactId&gt;avaje-agentloader&lt;/artifactId&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p>
 * Did you see the <code>test scope</code>? We don't want to use the runtime enhancer while
 * running in prod. Instead, we want to use the build time enhancer.
 * </p>
 * <p>
 * All you have to do is to add <code>avaje-ebeanorm-mavenenhancer</code> to your
 * <code>pom.xml</code> as described in the
 * <a href="http://ebean-orm.github.io/docs#enhance_maven">official doc</a>.
 * </p>
 *
 * <p>
 * Alternative, and because we want to keep our <code>pom.xml</code> small, you can drop a
 * <code>ebean.activator</code> file inside the <code>src/etc/mvn</code> folder. The presence of the
 * file <code>src/etc/mvn/ebean.activator</code> will trigger the
 * <code>avaje-ebeanorm-mavenenhancer</code> plugin.
 * </p>
 *
 * <h2>configuration</h2>
 * <p>
 * Configuration is done via <code>.conf</code>, for example:
 * </p>
 *
 * <pre>
 * ebean.ddl.generate=false
 * ebean.ddl.run=false
 *
 * ebean.debug.sql=true
 * ebean.debug.lazyload=false
 *
 * ebean.disableClasspathSearch = false
 * </pre>
 *
 * <p>
 * Or programmatically:
 * </p>
 *
 * <pre>
 * {
 *   use(new Ebeanby().doWith(conf {@literal ->} {
 *     conf.setDisableClasspathSearch(false);
 *   }));
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.10.0
 */
public class Ebeanby extends Jdbc {

  /** package for test */
  static final AtomicReference<Set<String>> PKG = new AtomicReference<>(new HashSet<>());

  static {
    // Turn off ebean shutdown hook:
    System.setProperty("ebean.registerShutdownHook", "false");
  }

  /**
   * Creates a new {@link Ebeanby} using the given name to setup a {@link Jdbc} datasource.
   *
   * @param name Name of this ebean module.
   */
  public Ebeanby(final String name) {
    super(name);
  }

  /**
   * Creates a new {@link Ebeanby} using the default {@link Jdbc} name: <code>db</code>.
   */
  public Ebeanby() {
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    configure(env, conf, binder, (name, ds) -> {
      ServerConfig config = new ServerConfig();

      config.setName(name);

      Config cprops = conf.getConfig("ebean");
      if (conf.hasPath("ebean." + name)) {
        cprops = conf.getConfig("ebean." + name)
            .withFallback(cprops)
            .withoutPath(name);
      }

      Properties props = props(cprops);

      ContainerConfig container = new ContainerConfig();
      container.loadFromProperties(props);

      config.setContainerConfig(container);
      config.setDataSource(ds);
      config.loadFromProperties(props);
      config.setDefaultServer(cprops.getBoolean("defaultServer"));
      config.setRegister(cprops.getBoolean("register"));

      callback(config, conf);

      List<String> packages = config.getPackages();
      if (packages == null || packages.size() == 0) {
        packages = ImmutableList.of(conf.getString("application.ns"));
      }
      config.setPackages(packages);
      PKG.get().addAll(packages);

      EbeanManaged server = new EbeanManaged(conf, config);
      env.onStart(server::start);
      env.onStop(server::stop);
      /** Bind db key: */
      Consumer<Key<EbeanServer>> provider = k -> binder.bind(k).toProvider(server)
          .asEagerSingleton();
      ServiceKey keys = env.serviceKey();
      if (!name.equals(dbref)) {
        keys.generate(EbeanServer.class, dbref, provider);
      }
      keys.generate(EbeanServer.class, name, provider);
    });

    // Run enhancer once per JVM
    env.onStart(runEnhancer());
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "ebean.conf").withFallback(super.config());
  }

  private Properties props(final Config config) {
    Properties props = new Properties();

    config.entrySet().forEach(prop -> {
      Object value = prop.getValue().unwrapped();
      props.setProperty("ebean." + prop.getKey(), value.toString());
    });
    return props;
  }

  /**
   * package for test.
   *
   * @return An enhancer task. It runs once per classloader instance.
   */
  static CheckedRunnable runEnhancer() {
    return () -> {
      Set<String> packages = PKG.getAndSet(null);
      if (packages != null) {
        EbeanEnhancer.newEnhancer().run(packages);
      }
    };
  }

}
