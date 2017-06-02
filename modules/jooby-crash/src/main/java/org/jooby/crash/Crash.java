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
package org.jooby.crash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.Registry;
import org.jooby.Route;
import org.jooby.WebSocket;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.concurrent.Promise;

/**
 * <h1>crash</h1>
 * <p>
 * Connect, monitor or use virtual machine resources via SSH, telnet or HTTP with
 * <a href="http://www.crashub.org">CRaSH remote shell.</a>
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * import org.jooby.crash;
 *
 * {
 *   use(new Crash());
 * }
 * }</pre>
 *
 * <p>
 * That's all you need to get <a href="http://www.crashub.org">CRaSH</a> up and running!!!
 * </p>
 * <p>
 * Now is time to see how to connect and interact with the <a href="http://www.crashub.org">CRaSH
 * shell</a>
 * </p>
 *
 * <h2>commands</h2>
 * <p>
 * You can write additional shell commands using Groovy or Java, see the
 * <a href="http://www.crashub.org/1.3/reference.html#developping_commands">CRaSH documentation for
 * details</a>. CRaSH search for commands in the <code>cmd</code> folder.
 * </p>
 * <p>
 * Here is a simple ‘hello’ command that could be loaded from <code>cmd/hello.groovy</code> folder:
 * </p>
 * <pre>{@code
 * package commands
 *
 * import org.crsh.cli.Command
 * import org.crsh.cli.Usage
 * import org.crsh.command.InvocationContext
 *
 * class hello {
 *
 *  &#64;Usage("Say Hello")
 *  &#64;Command
 *  def main(InvocationContext context) {
 *      return "Hello"
 *  }
 *
 * }
 * }</pre>
 *
 * <p>
 * Jooby adds some additional attributes and commands to InvocationContext that you can access from
 * your command:
 * </p>
 *
 * <ul>
 * <li>registry: Access to {@link Registry}.</li>
 * <li>conf: Access to {@link Config}.</li>
 * </ul>
 *
 * <h3>routes command</h3>
 * <p>
 * The <code>routes</code> print all the application routes.
 * </p>
 *
 * <h3>conf command</h3>
 * <p>
 * The <code>conf tree</code> print the application configuration tree (configuration precedence).
 * </p>
 *
 * <p>
 * The <code>conf props [path]</code> print all the application properties, sub-tree or a single
 * property if <code>path</code> argument is present.
 * </p>
 *
 * <h2>connectors</h2>
 *
 * <h3>HTTP connector</h3>
 * <p>
 * The HTTP connector is a simple yet powerful collection of HTTP endpoints where you can
 * run
 * <a href="http://www.crashub.org/1.3/reference.html#developping_commands">CRaSH
 * command</a>:
 * </p>
 *
 * <pre>{@code
 *
 * {
 *   use(new Crash()
 *      .plugin(HttpShellPlugin.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Try it:
 * </p>
 *
 * <pre>
 * GET /api/shell/thread/ls
 * </pre>
 *
 * <p>
 * OR:
 * </p>
 *
 * <pre>
 * GET /api/shell/thread ls
 * </pre>
 *
 * <p>
 * The connector listen at <code>/api/shell</code>. If you want to mount the connector some
 * where
 * else just set the property: <code>crash.httpshell.path</code>.
 * </p>
 *
 * <h3>SSH connector</h3>
 * <p>
 * Just add the <a href=
 * "https://mvnrepository.com/artifact/org.crashub/crash.connectors.ssh">crash.connectors.ssh</a>
 * dependency to your project.
 * </p>
 *
 * <p>
 * Try it:
 * </p>
 *
 * <pre>
 * ssh -p 2000 admin@localhost
 * </pre>
 *
 * <p>
 * Default user and password is: <code>admin</code>. See how to provide a custom
 * <a href="http://www.crashub.org/1.3/reference.html#pluggable_auth">authentication
 * plugin</a>.
 * </p>
 *
 * <h3>telnet connector</h3>
 * <p>
 * Just add the <a href=
 * "https://mvnrepository.com/artifact/org.crashub/crash.connectors.telnet">crash.connectors.telnet</a>
 * dependency to your project.
 * </p>
 *
 * <p>
 * Try it:
 * </p>
 *
 * <pre>
 * telnet localhost 5000
 * </pre>
 *
 * <p>
 * Checkout complete
 * <a href="http://www.crashub.org/1.3/reference.html#_telnet_connector">telnet
 * connector</a> configuration.
 * </p>
 *
 * <h3>web connector</h3>
 * <p>
 * Just add the <a href=
 * "https://mvnrepository.com/artifact/org.crashub/crash.connectors.web">crash.connectors.web</a>
 * dependency to your project.
 * </p>
 *
 * <p>
 * Try it:
 * </p>
 *
 * <pre>
 * GET /shell
 * </pre>
 *
 * <p>
 * A web shell console will be ready to go at <code>/shell</code>. If you want to mount the
 * connector some where else just set the property: <code>crash.webshell.path</code>.
 * </p>
 *
 * @author edgar
 * @since 1.0.0
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Crash implements Module {

  static {
    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  private static final Key<Set<CRaSHPlugin>> PLUGINS = Key
      .get(new TypeLiteral<Set<CRaSHPlugin>>() {
      });

  private final Set<Class> plugins = new HashSet<>();

  private final ClassLoader loader;

  /**
   * Creates a new {@link Crash} module.
   *
   * @param loader Class loader to use or <code>null</code>.
   */
  public Crash(final ClassLoader loader) {
    this.loader = Optional.ofNullable(loader).orElse(getClass().getClassLoader());
  }

  /**
   * Creates a new {@link Crash} module.
   */
  public Crash() {
    this(null);
  }

  /**
   * Add a custom plugin to CRaSH.
   *
   * @param plugin Plugin class.
   * @return This module.
   */
  public Crash plugin(final Class<? extends CRaSHPlugin> plugin) {
    plugins.add(plugin);
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    Properties props = new Properties();
    if (conf.hasPath("crash")) {
      conf.getConfig("crash").entrySet().forEach(
          e -> props.setProperty("crash." + e.getKey(), e.getValue().unwrapped().toString()));
    }

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("conf", conf);

    // connectors.web on classpath?
    boolean webShell = loader.getResource("META-INF/resources/js/crash.js") != null;
    if (webShell) {
      plugins.add(WebShellPlugin.class);
      WebShellPlugin.install(env, conf);
    }

    if (plugins.contains(HttpShellPlugin.class)) {
      HttpShellPlugin.install(env, conf);
    }

    Multibinder<CRaSHPlugin> mb = Multibinder.newSetBinder(binder, CRaSHPlugin.class);
    plugins.forEach(it -> mb.addBinding().to(it));

    CrashBootstrap crash = new CrashBootstrap();

    Promise<PluginContext> promise = Promise.make();
    binder.bind(PluginContext.class).toProvider(promise.future()::get);

    env.onStart(r -> {
      Set<Route.Definition> routes = r.require(Route.KEY);
      Set<WebSocket.Definition> sockets = r.require(WebSocket.KEY);
      attributes.put("registry", r);
      attributes.put("routes", routes);
      attributes.put("websockets", sockets);
      attributes.put("env", env);

      Set plugins = Sets.newHashSet(r.require(PLUGINS));
      ServiceLoader.load(CRaSHPlugin.class, this.loader).forEach(plugins::add);

      promise.success(crash.start(loader, props, attributes, plugins));
    });

    env.onStop(crash::stop);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "crash.conf");
  }

}
