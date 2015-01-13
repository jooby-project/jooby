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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
/**
o * Licensed to the Apache Software Foundation (ASF) under one
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
package org.jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import org.jooby.Route.Filter;
import org.jooby.internal.AppManager;
import org.jooby.internal.AssetFormatter;
import org.jooby.internal.AssetHandler;
import org.jooby.internal.BuiltinBodyConverter;
import org.jooby.internal.LocaleUtils;
import org.jooby.internal.RouteHandler;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.RoutePattern;
import org.jooby.internal.Server;
import org.jooby.internal.SessionManager;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.mvc.Routes;
import org.jooby.internal.routes.HeadHandler;
import org.jooby.internal.routes.OptionsHandler;
import org.jooby.internal.routes.TraceHandler;
import org.jooby.internal.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>Getting Started:</h1>
 * <p>
 * A new application must extends Jooby, register one ore more {@link Body.Formatter} and some
 * {@link Route routes}. It sounds like a lot of work to do, but it isn't.
 * </p>
 *
 * <pre>
 * public class MyApp extends Jooby {
 *
 *   {
 *      use(new Json()); // 1. JSON serializer.
 *
 *      // 2. Define a route
 *      get("/", (req, rsp) {@literal ->} {
 *        Map{@literal <}String, Object{@literal >} model = ...;
 *        rsp.send(model);
 *      }
 *   }
 *
 *  public static void main(String[] args) throws Exception {
 *    new MyApp().start(); // 3. Done!
 *  }
 * }
 * </pre>
 *
 * <h1>Properties files</h1>
 * <p>
 * Jooby delegate configuration management to <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a>. If you are unfamiliar with <a
 * href="https://github.com/typesafehub/config">TypeSafe Config</a> please take a few minutes to
 * discover what <a href="https://github.com/typesafehub/config">TypeSafe Config</a> can do for you.
 * </p>
 *
 * <p>
 * By default Jooby looks for an <code>application.conf</code> file at the root of the classpath. If
 * you want to specify a different file or location, you can do it with {@link #use(Config)}.
 * </p>
 *
 * <p>
 * <a href="https://github.com/typesafehub/config">TypeSafe Config</a> uses a hierarchical model to
 * define and override properties.
 * </p>
 * <p>
 * A {@link Jooby.Module} might provides his own set of properties through the
 * {@link Jooby.Module#config()} method. By default, this method returns an empty config object.
 * </p>
 * For example:
 *
 * <pre>
 *   use(new M1());
 *   use(new M2());
 *   use(new M3());
 * </pre>
 *
 * Previous example had the following order (first-listed are higher priority):
 * <ul>
 * <li>System properties</li>
 * <li>application.conf</li>
 * <li>M3 properties</li>
 * <li>M2 properties</li>
 * <li>M1 properties</li>
 * </ul>
 * <p>
 * System properties takes precedence over any application specific property.
 * </p>
 *
 * <h1>env</h1>
 * <p>
 * Jooby defines two modes: <strong>dev</strong> or something else. In Jooby, <strong>dev</strong>
 * is special and some modules could apply special settings while running in <strong>dev</strong>.
 * Any other env is usually considered a <code>prod</code> like env. But that depends on module
 * implementor.
 * </p>
 * <p>
 * A env can be defined in your <code>application.conf</code> file using the
 * <code>application.env</code> property. If missing, Jooby set the env for you to
 * <strong>dev</strong>.
 * </p>
 * <p>
 * There is more at {@link Env} so take a few minutes to discover what a {@link Env} can do for you.
 * </p>
 *
 * <h1>Modules</h1>
 * <p>
 * {@link Jooby.Module Modules} are quite similar to a Guice modules except that the configure
 * callback has been complementing with {@link Env} and {@link Config}.
 * </p>
 *
 * <pre>
 *   public class MyModule implements Jooby.Module {
 *     public void configure(env env, Config config, Binder binder) {
 *     }
 *   }
 * </pre>
 *
 * From the configure callback you can bind your services as you usually do in a Guice app.
 * <p>
 * There is more at {@link Jooby.Module} so take a few minutes to discover what a
 * {@link Jooby.Module} can do for you.
 * </p>
 *
 * <h1>Path Patterns</h1>
 * <p>
 * Jooby supports Ant-style path patterns:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.html} or
 * {@code com/txst.html}</li>
 * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * <li>{@code **}/{@code *} - matches any path at any level.</li>
 * <li>{@code *} - matches any path at any level, shorthand for {@code **}/{@code *}.</li>
 * </ul>
 *
 * <h2>Variables</h2>
 * <p>
 * Jooby supports path parameters too:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
 * <code>id</code> var.</li>
 * </ul>
 *
 * <h1>Routes</h1>
 * <p>
 * Routes perform actions in response to a server HTTP request. There are two types of routes
 * callback: {@link Route.Handler} and {@link Route.Filter}.
 * </p>
 * <p>
 * Routes are executed in the order they are defined, for example:
 *
 * <pre>
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 * Please note first and second routes are converted to a filter, so previous example is the same
 * as:
 *
 * <pre>
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 * Due to the use of lambdas a route is a singleton and you should NOT use global variables. For
 * example this is a bad practice:
 *
 * <pre>
 *  List{@literal <}String{@literal >} names = new ArrayList{@literal <}{@literal >}(); // names produces side effects
 *  get("/", (req, rsp) {@literal ->} {
 *     names.add(req.param("name").stringValue();
 *     // response will be different between calls.
 *     rsp.send(names);
 *   });
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc route use annotations to define routes:
 * </p>
 *
 * <pre>
 *   use(MyRoute.class);
 *   ...
 *   // MyRoute.java
 *   {@literal @}Path("/")
 *   public class MyRoute {
 *
 *    {@literal @}GET
 *    public String hello() {
 *      return "Hello Jooby";
 *    }
 *   }
 * </pre>
 * <p>
 * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
 * simplifications.
 * </p>
 *
 * <p>
 * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}, and {@link org.jooby.mvc.Viewable}
 * .
 * </p>
 *
 * <h1>Static Files</h1>
 * <p>
 * Static files, like: *.js, *.css, ..., etc... can be served with:
 * </p>
 *
 * <pre>
 *   assets("assets/**");
 * </pre>
 * <p>
 * Classpath resources under the <code>/assets</code> folder will be accessible from client/browser.
 * </p>
 * <h1>Bootstrap</h1>
 * <p>
 * The bootstrap process is defined as follows:
 * </p>
 * <h2>1. Configuration files are loaded in this order:</h2>
 * <ol>
 * <li>System properties</li>
 * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
 * <li>Configuration properties from {@link Jooby.Module modules}</li>
 * </ol>
 *
 * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
 * <ol>
 * <li>An {@link Injector Guice Injector} is created.</li>
 * <li>It configures each registered {@link Jooby.Module module}</li>
 * <li>At this point Guice is ready and all the services has been binded.</li>
 * <li>The {@link Jooby.Module#start() start method} is invoked.</li>
 * <li>Finally, Jooby starts the web server</li>
 * </ol>
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby.Module
 */
public class Jooby {

  /**
   * A module can publish or produces: {@link Route.Definition routes}, {@link Body.Parser},
   * {@link Body.Formatter}, {@link Request.Module request modules} and any other
   * application specific service or contract of your choice.
   * <p>
   * It is similar to {@link com.google.inject.Module} except for the callback method receives a
   * {@link Env}, {@link Config} and {@link Binder}.
   * </p>
   *
   * <p>
   * A module can provide his own set of properties through the {@link #config()} method. By
   * default, this method returns an empty config object.
   * </p>
   * For example:
   *
   * <pre>
   *   use(new M1());
   *   use(new M2());
   *   use(new M3());
   * </pre>
   *
   * Previous example had the following order (first-listed are higher priority):
   * <ul>
   * <li>System properties</li>
   * <li>application.conf</li>
   * <li>M3 properties</li>
   * <li>M2 properties</li>
   * <li>M1 properties</li>
   * </ul>
   *
   * <p>
   * A module can provide start/stop methods in order to start or close resources.
   * </p>
   *
   * @author edgar
   * @since 0.1.0
   * @see Jooby#use(Jooby.Module)
   */
  public interface Module {

    /**
     * @return Produces a module config object (when need it). By default a module doesn't produce
     *         any configuration object.
     */
    default @Nonnull Config config() {
      return ConfigFactory.empty();
    }

    /**
     * Callback method to start a module. This method will be invoked after all the registered
     * modules has been configured.
     */
    default void start() {
    }

    /**
     * Callback method to stop a module and clean any resources. Invoked when the application is
     * about to shutdown.
     */
    default void stop() {
    }

    /**
     * Configure and produces bindings for the underlying application. A module can optimize or
     * customize a service by checking current the {@link Env application env} and/or the current
     * application properties available from {@link Config}.
     *
     * @param env The current application's env. Not null.
     * @param config The current config object. Not null.
     * @param binder A guice binder. Not null.
     */
    void configure(@Nonnull Env env, @Nonnull Config config, @Nonnull Binder binder);

  }

  static {
    // Avoid warning message from logback when multiples files are present
    String logback = System.getProperty("logback.configurationFile");
    if (Strings.isNullOrEmpty(logback)) {
      // set it, when missing
      System.setProperty("logback.configurationFile", "logback.xml");
    }
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Keep track of routes.
   */
  private final Set<Object> bag = new LinkedHashSet<>();

  /**
   * Keep track of modules.
   */
  private final Set<Jooby.Module> modules = new LinkedHashSet<>();

  /**
   * Keep track of singleton MVC routes.
   */
  private final Set<Class<?>> singletonRoutes = new LinkedHashSet<>();

  /**
   * Keep track of prototype MVC routes.
   */
  private final Set<Class<?>> protoRoutes = new LinkedHashSet<>();

  /**
   * The override config. Optional.
   */
  private Config source;

  /** Keep the global injector instance. */
  private Injector injector;

  /** Error handler. */
  private Err.Handler err;

  /** Body formatters. */
  private List<Body.Formatter> formatters = new LinkedList<>();

  /** Body parsers. */
  private List<Body.Parser> parsers = new LinkedList<>();

  /** Session store. */
  private Session.Definition session = new Session.Definition(new Session.MemoryStore());

  /** Flag to control the addition of the asset formatter. */
  private boolean assetFormatter = false;

  /** Env builder. */
  private Env.Builder env = Env.DEFAULT;

  {
    use(new Undertow());
  }

  /**
   * Set a custom {@link Env.Builder} to use.
   *
   * @param env A custom env builder.
   * @return This jooby instance.
   */
  public @Nonnull Jooby env(final Env.Builder env) {
    this.env = requireNonNull(env, "Env builder is required.");
    return this;
  }

  /**
   * Setup a session store to use. Useful if you want/need to persist sessions between shutdowns.
   * Sessions are not persisted by defaults.
   *
   * @param sessionStore A session store.
   * @return A session store definition.
   */
  public @Nonnull Session.Definition use(@Nonnull final Session.Store sessionStore) {
    this.session = new Session.Definition(requireNonNull(sessionStore,
        "A session store is required."));
    return this.session;
  }

  /**
   * Append a body formatter for write HTTP messages.
   *
   * @param formatter A body formatter.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(@Nonnull final Body.Formatter formatter) {
    this.formatters.add(requireNonNull(formatter, "A body formatter is required."));
    return this;
  }

  /**
   * Append a body parser for write HTTP messages.
   *
   * @param parser A body parser.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(@Nonnull final Body.Parser parser) {
    this.parsers.add(requireNonNull(parser, "A body parser is required."));
    return this;
  }

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("*", path, filter));
  }

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String verb, final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition(verb, path, filter));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String verb, final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition(verb, path, handler));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition use(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("*", path, handler));
  }

  /**
   * Serve a static file from classpath:
   *
   * <pre>
   *   get("/favicon.ico");
   * </pre>
   *
   * This method is a shorcut for:
   *
   * <pre>
   *   get("/favicon.ico", file("/favicon.ico");
   * </pre>
   *
   * @param path A path pattern.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final String path) {
    return appendDefinition(new Route.Definition("GET", path, staticFile(path)));
  }

  /**
   * Append route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append a filter that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition get(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("GET", path, filter));
  }

  /**
   * Append a route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append a route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition post(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("POST", path, filter));
  }

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definition head(final @Nonnull String path, final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("HEAD", path, handler));
  }

  /**
   * Append route that supports HTTP HEAD method:
   *
   * <pre>
   *   head("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("HEAD", path, handler));
  }

  /**
   * Append route that supports HTTP HEAD method:
   *
   * <pre>
   *   head("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("HEAD", path, handler));
  }

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("HEAD", path, filter));
  }

  /**
   * Append a new route that automatically handles HEAD request from existing GET routes.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something); // This route provides default HEAD for this GET route.
   *   });
   * </pre>
   *
   * @return A new route definition.
   */
  public @Nonnull Route.Definition head() {
    return appendDefinition(new Route.Definition("HEAD", "*", filter(HeadHandler.class))
        .name("*.head"));
  }

  /**
   * Append a route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("OPTIONS", path, handler));
  }

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req) {@literal ->}
   *     Body.status(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("OPTIONS", path, handler));
  }

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", () {@literal ->}
   *     Body.status(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("OPTIONS", path, handler));
  }

  /**
   * Append a route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp, chain) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("OPTIONS", path, filter));
  }

  /**
   * Append a new route that automatically handles OPTIONS requests.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   *
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * OPTINOS / produces a response with a Allow header set to: GET, POST.
   *
   * @return A new route definition.
   */
  public @Nonnull Route.Definition options() {
    return appendDefinition(new Route.Definition("OPTIONS", "*", handler(OptionsHandler.class))
        .name("*.options"));
  }

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req) {@literal ->}
   *    Body.status(202)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", () {@literal ->} {
   *     Body.status(202)
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition put(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("PUT", path, filter));
  }

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition patch(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req) {@literal ->}
   *    Body.status(202)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition patch(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", () {@literal ->} {
   *     Body.status(202)
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition patch(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition patch(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("PATCH", path, filter));
  }

  /**
   * Append a route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp) {@literal ->} {
   *     rsp.status(304);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req) {@literal ->}
   *     Body.status(204)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", () {@literal ->}
   *     Body.status(204)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append a route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp, chain) {@literal ->} {
   *     rsp.status(304);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition delete(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("DELETE", path, filter));
  }

  /**
   * Append a route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp) {@literal ->} {
   *     rsp.send(...);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.Handler handler) {
    return appendDefinition(new Route.Definition("TRACE", path, handler));
  }

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req) {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("TRACE", path, handler));
  }

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", () {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("TRACE", path, handler));
  }

  /**
   * Append a route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("TRACE", path, filter));
  }

  /**
   * Append a default trace implementation under the given path. Default trace response, looks
   * like:
   *
   * <pre>
   *  TRACE /path
   *     header1: value
   *     header2: value
   *
   * </pre>
   *
   * @return A new route definition.
   */
  public @Nonnull Route.Definition trace() {
    return appendDefinition(new Route.Definition("TRACE", "*", handler(TraceHandler.class))
        .name("*.trace"));
  }

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      @Nonnull final Route.Handler handler) {
    return appendDefinition(new Route.Definition("CONNECT", path, handler));
  }

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      final @Nonnull Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("CONNECT", path, handler));
  }

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", () {@literal ->}
   *     "connected"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      final @Nonnull Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("CONNECT", path, handler));
  }

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition connect(final @Nonnull String path,
      final @Nonnull Route.Filter filter) {
    return appendDefinition(new Route.Definition("CONNECT", path, filter));
  }

  /**
   * Creates a new {@link Route.Handler} that delegate the execution to the given handler. This is
   * useful when the target handler requires some dependencies.
   *
   * <pre>
   *   public class MyHandler implements Route.Handler {
   *     &#64;Inject
   *     public MyHandler(Dependency d) {
   *     }
   *
   *     public void handle(Request req, Response rsp) throws Exception {
   *      // do something
   *     }
   *   }
   *   ...
   *   // external route
   *   get("/", handler(MyHandler.class));
   *
   *   // inline version route
   *   get("/", (req, rsp) {@literal ->} {
   *     Dependency d = req.getInstance(Dependency.class);
   *     // do something
   *   });
   * </pre>
   *
   * You can access to a dependency from a in-line route too, so the use of external route it is
   * more or less a matter of taste.
   *
   * @param handler The external handler class.
   * @return A new inline route handler.
   */
  private @Nonnull Route.Handler handler(final @Nonnull Class<? extends Route.Handler> handler) {
    requireNonNull(handler, "Route handler is required.");
    registerRouteScope(handler);
    return (req, rsp) -> req.require(handler).handle(req, rsp);
  }

  /**
   * Creates a new {@link Route.Filter} that delegate the execution to the given filter. This is
   * useful when the target handler requires some dependencies.
   *
   * <pre>
   *   public class MyFilter implements Filter {
   *     &#64;Inject
   *     public MyFilter(Dependency d) {
   *     }
   *
   *     public void handle(Request req, Response rsp, Route.Chain chain) throws Exception {
   *      // do something
   *     }
   *   }
   *   ...
   *   // external filter
   *   get("/", filter(MyFilter.class));
   *
   *   // inline version route
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     Dependency d = req.getInstance(Dependency.class);
   *     // do something
   *   });
   * </pre>
   *
   * You can access to a dependency from a in-line route too, so the use of external filter it is
   * more or less a matter of taste.
   *
   * @param filter The external filter class.
   * @return A new inline route.
   */
  private @Nonnull Route.Filter filter(final @Nonnull Class<? extends Route.Filter> filter) {
    requireNonNull(filter, "Filter is required.");
    registerRouteScope(filter);
    return (req, rsp, chain) -> req.require(filter).handle(req, rsp, chain);
  }

  /**
   * Serve or publish static files to browser.
   *
   * <pre>
   *   assets("/assets/**");
   * </pre>
   *
   * Resources are served from root of classpath, for example <code>GET /assets/file.js</code> will
   * be resolve as classpath resource at the same location.
   *
   * @param path The path to publish.
   * @return A new route definition.
   */
  public @Nonnull Route.Definition assets(final @Nonnull String path) {
    if (!assetFormatter) {
      formatters.add(new AssetFormatter());
      assetFormatter = true;
    }
    return get(path, new AssetHandler());
  }

  /**
   * <p>
   * Append one or more routes defined in the given class.
   * </p>
   *
   * <pre>
   *   use(MyRoute.class);
   *   ...
   *   // MyRoute.java
   *   {@literal @}Path("/")
   *   public class MyRoute {
   *
   *    {@literal @}GET
   *    public String hello() {
   *      return "Hello Jooby";
   *    }
   *   }
   * </pre>
   * <p>
   * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
   * simplifications.
   * </p>
   *
   * <p>
   * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
   * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes} and
   * {@link org.jooby.mvc.Viewable}.
   * </p>
   *
   * @param routeClass A route(s) class.
   * @return This jooby instance.
   */
  public @Nonnull Jooby use(final @Nonnull Class<?> routeClass) {
    requireNonNull(routeClass, "Route class is required.");
    registerRouteScope(routeClass);
    bag.add(routeClass);
    return this;
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location Either a relative or absolute location.
   * @return A route handler.
   */
  public Route.Handler redirect(final String location) {
    return redirect(Status.FOUND, location);
  }

  /**
   * Serve a single file from classpath.
   * Usage:
   *
   * <pre>
   *   {
   *     // serve the welcome.html from classpath root
   *     get("/", file("welcome.html");
   *   }
   * </pre>
   *
   * @param location Absolute classpath location.
   * @return A new route handler.
   */
  public Route.Filter staticFile(@Nonnull final String location) {
    requireNonNull(location, "A location is required.");
    String path = RoutePattern.normalize(location);
    if (!assetFormatter) {
      formatters.add(new AssetFormatter());
      assetFormatter = true;
    }
    return filehandler(path);
  }

  private static Filter filehandler(final String path) {
    return (req, rsp, chain) -> {
      new AssetHandler().handle(new Request.Forwarding(req) {

        @Override
        public String path() {
          return route().path();
        }

        @Override
        public Route route() {
          return new Route.Forwarding(super.route()) {
            @Override
            public String path() {
              return path;
            }
          };
        }
      }, rsp, chain);
    };
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param status A redirect status.
   * @param location Either a relative or absolute location.
   * @return A route handler.
   */
  public Route.Handler redirect(final Status status, final String location) {
    requireNonNull(location, "A location is required.");
    return (req, rsp) -> rsp.redirect(status, location);
  }

  /**
   * Check if the class had a Singleton annotation or not in order to register the route as
   * singleton or prototype.
   *
   * @param route
   */
  private void registerRouteScope(final Class<?> route) {
    if (route.getAnnotation(javax.inject.Singleton.class) != null ||
        route.getAnnotation(com.google.inject.Singleton.class) != null) {
      singletonRoutes.add(route);
    } else {
      protoRoutes.add(route);
    }
  }

  /**
   * Keep track of routes in the order user define them.
   *
   * @param route A route definition to append.
   * @return The same route definition.
   */
  private Route.Definition appendDefinition(final Route.Definition route) {
    bag.add(route);
    return route;
  }

  /**
   * Register a application module.
   *
   * @param module The module to register.
   * @return This jooby instance.
   * @see Jooby.Module
   */
  public @Nonnull Jooby use(final @Nonnull Jooby.Module module) {
    requireNonNull(module, "A module is required.");
    modules.add(module);
    bag.add(module);
    return this;
  }

  /**
   * Register a request module.
   *
   * @param module The module to register.
   * @return This jooby instance.
   * @see Request.Module
   */
  public @Nonnull Jooby use(final @Nonnull Request.Module module) {
    requireNonNull(module, "A module is required.");
    bag.add(module);
    return this;
  }

  /**
   * Set the application configuration object. You must call this method when the default file
   * name: <code>application.conf</code> doesn't work for you or when you need/want to register two
   * or more files.
   *
   * @param config The application configuration object.
   * @return This jooby instance.
   * @see Config
   */
  public @Nonnull Jooby use(final @Nonnull Config config) {
    this.source = requireNonNull(config, "A config is required.");
    return this;
  }

  /**
   * Setup a route error handler. Default error handler {@link Err.Default} does content
   * negotation and this method allow to override/complement default handler.
   *
   * @param err A route error handler.
   * @return This jooby instance.
   */
  public @Nonnull Jooby err(final @Nonnull Err.Handler err) {
    this.err = requireNonNull(err, "An err handler is required.");
    return this;
  }

  /**
   * Append a new WebSocket handler under the given path.
   *
   * <pre>
   *   ws("/ws", (socket) {@literal ->} {
   *     // connected
   *     socket.onMessage(message {@literal ->} {
   *       System.out.println(message);
   *     });
   *     socket.send("Connected"):
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A connect callback.
   * @return A new WebSocket definition.
   */
  public @Nonnull WebSocket.Definition ws(final @Nonnull String path,
      final @Nonnull WebSocket.Handler handler) {
    WebSocket.Definition ws = new WebSocket.Definition(path, handler);
    checkArgument(bag.add(ws), "Path is in use: '%s'", path);
    return ws;
  }

  /**
   * <h1>Bootstrap</h1>
   * <p>
   * The bootstrap process is defined as follows:
   * </p>
   * <h2>1. Configuration files (first-listed are higher priority)</h2>
   * <ol>
   * <li>System properties</li>
   * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
   * <li>{@link Jooby.Module Modules} properties</li>
   * </ol>
   *
   * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
   * <ol>
   * <li>An {@link Injector Guice Injector} is created.</li>
   * <li>It calls to {@link Jooby.Module#configure(Env, Config, Binder)} for each module.</li>
   * <li>At this point Guice is ready and all the services has been binded.</li>
   * <li>It calls to {@link Jooby.Module#start() start method} for each module.</li>
   * <li>A web server is started</li>
   * </ol>
   *
   * @throws Exception If something fails to start.
   */
  public void start() throws Exception {
    start(new String[0]);
  }

  /**
   * <h1>Bootstrap</h1>
   * <p>
   * The bootstrap process is defined as follows:
   * </p>
   * <h2>1. Configuration files (first-listed are higher priority)</h2>
   * <ol>
   * <li>System properties</li>
   * <li>Application properties: {@code application.conf} or custom, see {@link #use(Config)}</li>
   * <li>{@link Jooby.Module Modules} properties</li>
   * </ol>
   *
   * <h2>2. Dependency Injection and {@link Jooby.Module modules}</h2>
   * <ol>
   * <li>An {@link Injector Guice Injector} is created.</li>
   * <li>It calls to {@link Jooby.Module#configure(Env, Config, Binder)} for each module.</li>
   * <li>At this point Guice is ready and all the services has been binded.</li>
   * <li>It calls to {@link Jooby.Module#start() start method} for each module.</li>
   * <li>A web server is started</li>
   * </ol>
   *
   * @param args Application arguments.
   * @throws Exception If something fails to start.
   */
  public void start(final String[] args) throws Exception {
    long start = System.currentTimeMillis();
    // shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      stop();
    }));

    this.injector = bootstrap();

    // Start server
    Server server = injector.getInstance(Server.class);

    server.start();
    long end = System.currentTimeMillis();
    Config config = injector.getInstance(Config.class);
    log.info("Server started in {}ms\n{}\nlistening on:\n  http://localhost:{}\n",
        end - start,
        injector.getInstance(RouteHandler.class),
        config.getInt("application.port"));
  }

  private static AppManager appManager(final Jooby app, final Env env, final Logger log) {
    return action -> {
      if (action == AppManager.STOP) {
        app.stop();
        return app.injector;
      }
      try {
        long start = System.currentTimeMillis();

        Jooby newApp = app.getClass().newInstance();
        app.stopModules();

        Injector injector = newApp.bootstrap();

        // override modules, so they can be stopped it against restarts.
        app.modules.addAll(newApp.modules);

        log.info("reloading of {} took {}ms", app.getClass().getName(),
            System.currentTimeMillis() - start);
        return injector;
      } catch (InstantiationException | IllegalAccessException ex) {
        log.debug("Can't create app", ex);
        return app.injector;
      }
    };
  }

  private Injector bootstrap() {
    Config config = buildConfig(
        Optional.ofNullable(this.source)
            .orElseGet(
                () -> ConfigFactory.parseResources("application.conf")
            )
        );
    Env env = this.env.build(config);

    final Charset charset = Charset.forName(config.getString("application.charset"));

    final Locale locale = LocaleUtils.toLocale(config.getString("application.lang"), "_");

    ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
    DateTimeFormatter dateTimeFormat = DateTimeFormatter
        .ofPattern(config.getString("application.dateFormat"), locale)
        .withZone(zoneId);

    DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));

    // Guice Stage
    Stage stage = "dev".equals(env.name()) ? Stage.DEVELOPMENT : Stage.PRODUCTION;

    // dependency injection
    Injector injector = Guice
        .createInjector(stage, binder -> {

          TypeConverters.configure(binder);

          if ("dev".equals(env.name())) {
            binder.bind(Key.get(String.class, Names.named("internal.appClass")))
                .toInstance(getClass().getName());
            binder.bind(AppManager.class).toInstance(appManager(this, env, log));
          }

          // bind config
          bindConfig(binder, config);

          // bind env
          binder.bind(Env.class).toInstance(env);

          // bind charset
          binder.bind(Charset.class).toInstance(charset);

          // bind locale
          binder.bind(Locale.class).toInstance(locale);

          // bind time zone
          binder.bind(ZoneId.class).toInstance(zoneId);
          binder.bind(TimeZone.class).toInstance(TimeZone.getTimeZone(zoneId));

          // bind date format
          binder.bind(DateTimeFormatter.class).toInstance(dateTimeFormat);

          // bind number format
          binder.bind(NumberFormat.class).toInstance(numberFormat);
          binder.bind(DecimalFormat.class).toInstance(numberFormat);

          // bind formatter & parser
          Multibinder<Body.Parser> parserBinder = Multibinder
              .newSetBinder(binder, Body.Parser.class);
          Multibinder<Body.Formatter> formatterBinder = Multibinder
              .newSetBinder(binder, Body.Formatter.class);

          // session definition
          binder.bind(Session.Definition.class).toInstance(session);

          // Routes
          Multibinder<Route.Definition> definitions = Multibinder
              .newSetBinder(binder, Route.Definition.class);

          // Web Sockets
          Multibinder<WebSocket.Definition> sockets = Multibinder
              .newSetBinder(binder, WebSocket.Definition.class);

          // Request Modules
          Multibinder<Request.Module> requestModule = Multibinder
              .newSetBinder(binder, Request.Module.class);

          // bind prototype routes in request module
          if (protoRoutes.size() > 0) {
            requestModule.addBinding().toInstance(
                b -> protoRoutes.forEach(routeClass1 -> b.bind(routeClass1)));
          }

          // tmp dir
          File tmpdir = new File(config.getString("application.tmpdir"));
          tmpdir.mkdirs();
          binder.bind(File.class).annotatedWith(Names.named("application.tmpdir"))
              .toInstance(tmpdir);

          // parser & formatter
          parsers.forEach(it -> parserBinder.addBinding().toInstance(it));
          formatters.forEach(it -> formatterBinder.addBinding().toInstance(it));

          RouteMetadata classInfo = new RouteMetadata(env);
          binder.bind(RouteMetadata.class).toInstance(classInfo);

          // modules, routes and websockets
          bag.forEach(candidate -> {
            if (candidate instanceof Jooby.Module) {
              install((Jooby.Module) candidate, env, config, binder);
            } else if (candidate instanceof Request.Module) {
              requestModule.addBinding().toInstance((Request.Module) candidate);
            } else if (candidate instanceof Route.Definition) {
              definitions.addBinding().toInstance((Route.Definition) candidate);
            } else if (candidate instanceof WebSocket.Definition) {
              sockets.addBinding().toInstance((WebSocket.Definition) candidate);
            } else {
              Routes.routes(env, classInfo, (Class<?>) candidate)
                  .forEach(route -> definitions.addBinding().toInstance(route));
            }
          });

          // Singleton routes
          singletonRoutes.forEach(routeClass3 -> binder.bind(routeClass3).in(Scopes.SINGLETON));

          formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatReader);
          formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatStream);
          formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatByteArray);
          formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatByteBuffer);
          formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatAny);

          parserBinder.addBinding().toInstance(BuiltinBodyConverter.parseString);

          // session manager
          binder.bind(SessionManager.class).toInstance(new SessionManager(config, session));

          // err
          if (err == null) {
            binder.bind(Err.Handler.class).toInstance(new Err.Default());
          } else {
            binder.bind(Err.Handler.class).toInstance(err);
          }
        });

    // start modules
    for (Jooby.Module module : modules) {
      module.start();
    }

    return injector;
  }

  /**
   * Stop the application, close all the modules and stop the web server.
   */
  public void stop() {
    stopModules();

    if (injector != null) {
      try {
        Server server = injector.getInstance(Server.class);
        server.stop();
      } catch (Exception ex) {
        log.error("Web server didn't stop normally", ex);
      }
      log.info("Server stopped");
      injector = null;
    }
  }

  private void stopModules() {
    // stop modules
    for (Jooby.Module module : modules) {
      try {
        module.stop();
      } catch (Exception ex) {
        log.warn("Module didn't stop normally: " + module.getClass().getName(), ex);
      }
    }
    modules.clear();
  }

  /**
   * Build configuration properties, it configure system, app and modules properties.
   *
   * @param source Source config to use.
   * @return A configuration properties ready to use.
   */
  private Config buildConfig(final Config source) {
    // system properties
    Config system = ConfigFactory.systemProperties()
        // file encoding got corrupted sometimes so we force and override.
        .withValue("file.encoding",
            ConfigValueFactory.fromAnyRef(System.getProperty("file.encoding")));

    // set module config
    Config moduleStack = ConfigFactory.empty();
    for (Jooby.Module module : ImmutableList.copyOf(modules).reverse()) {
      moduleStack = moduleStack.withFallback(module.config());
    }

    // jooby config
    Config jooby = ConfigFactory.parseResources(Jooby.class, "jooby.conf");

    String env = Arrays.asList(system, source, jooby).stream()
        .filter(it -> it.hasPath("application.env"))
        .findFirst()
        .get()
        .getString("application.env");

    Config modeConfig = modeConfig(source, env);

    // application.[env].conf -> application.conf
    Config config = modeConfig.withFallback(source);

    return system
        .withFallback(config)
        .withFallback(moduleStack)
        .withFallback(MediaType.types)
        .withFallback(defaultConfig(config, env))
        .withFallback(jooby)
        .resolve();
  }

  /**
   * Build a env config: <code>[application].[env].[conf]</code>.
   * Stack looks like
   *
   * <pre>
   *   (file://[origin].[env].[conf])?
   *   (cp://[origin].[env].[conf])?
   *   file://application.[env].[conf]
   *   /application.[env].[conf]
   * </pre>
   *
   * @param source App source to use.
   * @param env Application env.
   * @return A config env.
   */
  private Config modeConfig(final Config source, final String env) {
    String origin = source.origin().resource();
    Config result = ConfigFactory.empty();
    if (origin != null) {
      // load [resource].[env].[ext]
      int dot = origin.lastIndexOf('.');
      String originConf = origin.substring(0, dot) + "." + env + origin.substring(dot);

      result = fileConfig(originConf).withFallback(ConfigFactory.parseResources(originConf));
    }
    String appConfig = "application." + env + ".conf";
    return result
        .withFallback(fileConfig(appConfig))
        .withFallback(fileConfig("application.conf"))
        .withFallback(ConfigFactory.parseResources(appConfig));
  }

  /**
   * Config from file system.
   *
   * @param fname A file name.
   * @return A config for the file name.
   */
  private Config fileConfig(final String fname) {
    File in = new File(fname);
    return in.exists() ? ConfigFactory.parseFile(in) : ConfigFactory.empty();
  }

  /**
   * Build default application.* properties.
   *
   * @param config A source config.
   * @param env Application env.
   * @return default properties.
   */
  private Config defaultConfig(final Config config, final String env) {
    Map<String, Object> defaults = new LinkedHashMap<>();

    // set app name
    defaults.put("name", getClass().getSimpleName());

    // build dir (dev only)
    defaults.put("builddir", Paths.get(System.getProperty("user.dir"), "target", "classes")
        .toString());

    // set tmpdir
    String deftmpdir = "java.io.tmpdir";
    String tmpdir = config.hasPath(deftmpdir)
        ? config.getString(deftmpdir)
        : System.getProperty(deftmpdir);
    if (tmpdir.endsWith(File.separator)) {
      tmpdir = tmpdir.substring(0, tmpdir.length() - File.separator.length());
    }
    defaults.put("tmpdir", tmpdir + File.separator + defaults.get("name"));

    // namespacce
    defaults.put("ns", getClass().getPackage().getName());

    // locale
    final Locale locale;
    if (!config.hasPath("application.lang")) {
      locale = Locale.getDefault();
      defaults.put("lang", locale.getLanguage() + "_" + locale.getCountry());
    } else {
      locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
    }

    // time zone
    if (!config.hasPath("application.tz")) {
      defaults.put("tz", ZoneId.systemDefault().getId());
    }

    // number format
    if (!config.hasPath("application.numberFormat")) {
      String pattern = ((DecimalFormat) DecimalFormat.getInstance(locale)).toPattern();
      defaults.put("numberFormat", pattern);
    }

    Map<String, Object> application = ImmutableMap.of("application", defaults);
    return ConfigValueFactory.fromMap(application).toConfig();
  }

  /**
   * Install a {@link JoobyModule}.
   *
   * @param module The module to install.
   * @param env Application env.
   * @param config The configuration object.
   * @param binder A Guice binder.
   */
  private void install(final Jooby.Module module, final Env env, final Config config,
      final Binder binder) {
    try {
      module.configure(env, config, binder);
    } catch (Exception ex) {
      throw new IllegalStateException("Error found on module: " + module.getClass().getName(), ex);
    }
  }

  /**
   * Bind a {@link Config} and make it available for injection. Each property of the config is also
   * binded it and ready to be injected with {@link javax.inject.Named}.
   *
   * @param binder Guice binder.
   * @param config App config.
   */
  @SuppressWarnings("unchecked")
  private void bindConfig(final Binder binder, final Config config) {
    for (Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Named named = Names.named(name);
      Object value = entry.getValue().unwrapped();
      if (value instanceof List) {
        List<Object> values = (List<Object>) value;
        Type listType = Types.listOf(values.iterator().next().getClass());
        Key<Object> key = (Key<Object>) Key.get(listType, Names.named(name));
        binder.bind(key).toInstance(values);
      } else {
        @SuppressWarnings("rawtypes")
        Class type = value.getClass();
        binder.bind(type).annotatedWith(named).toInstance(value);
      }
    }
    // bind config
    binder.bind(Config.class).toInstance(config);
  }

}
