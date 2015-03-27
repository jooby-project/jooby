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
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
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

import javax.inject.Singleton;

import org.jooby.Session.Store;
import org.jooby.internal.AppPrinter;
import org.jooby.internal.AssetFormatter;
import org.jooby.internal.AssetHandler;
import org.jooby.internal.BuiltinBodyConverter;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.internal.LifecycleProcessor;
import org.jooby.internal.LocaleUtils;
import org.jooby.internal.RequestScope;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.ServerLookup;
import org.jooby.internal.SessionManager;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.internal.reqparam.CollectionParamConverter;
import org.jooby.internal.reqparam.CommonTypesParamConverter;
import org.jooby.internal.reqparam.DateParamConverter;
import org.jooby.internal.reqparam.EnumParamConverter;
import org.jooby.internal.reqparam.LocalDateParamConverter;
import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.OptionalParamConverter;
import org.jooby.internal.reqparam.ParamResolver;
import org.jooby.internal.reqparam.StaticMethodParamConverter;
import org.jooby.internal.reqparam.StringConstructorParamConverter;
import org.jooby.internal.reqparam.UploadParamConverter;
import org.jooby.internal.routes.HeadHandler;
import org.jooby.internal.routes.OptionsHandler;
import org.jooby.internal.routes.TraceHandler;
import org.jooby.scope.RequestScoped;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>Getting Started:</h1>
 * <p>
 * A new application must extends Jooby, register one ore more {@link BodyFormatter} and some
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
 *     names.add(req.param("name").value();
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
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}
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
   * A module can publish or produces: {@link Route.Definition routes}, {@link BodyParser},
   * {@link BodyFormatter}, and any other application specific service or contract of your choice.
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
    default Config config() {
      return ConfigFactory.empty();
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
    void configure(Env env, Config config, Binder binder);

  }

  static {
    // Avoid warning message from logback when multiples files are present
    String logback = System.getProperty("logback.configurationFile");
    if (Strings.isNullOrEmpty(logback)) {
      // set it, when missing
      System.setProperty("logback.configurationFile", "logback.xml");
    }
  }

  public Jooby() {
    use(new ServerLookup());
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
   * The override config. Optional.
   */
  private Config source;

  /** Keep the global injector instance. */
  private Injector injector;

  /** Error handler. */
  private Err.Handler err;

  /** Body formatters. */
  private List<BodyFormatter> formatters = new LinkedList<>();

  /** Body parsers. */
  private List<BodyParser> parsers = new LinkedList<>();

  /** Session store. */
  private Session.Definition session = new Session.Definition(Session.Mem.class);

  /** Flag to control the addition of the asset formatter. */
  private boolean assetFormatter = false;

  /** Env builder. */
  private Env.Builder env = Env.DEFAULT;

  private LinkedList<ParamConverter> converters = new LinkedList<>();

  /**
   * Import ALL the direct routes from the given app. PLEASE NOTE: that ONLY routes are imported.
   * {@link Jooby.Module modules}, {@link BodyFormatter formatters}, etc... won't be import it.
   *
   * @param app Routes provider.
   * @return This jooby instance.
   */
  public Jooby use(final Jooby app) {
    requireNonNull(app, "App is required.");
    if (!assetFormatter && app.assetFormatter) {
      this.assetFormatter = true;
      use(new AssetFormatter());
    }
    app.bag.forEach(c -> {
      if (!(c instanceof Jooby.Module)) {
        this.bag.add(c);
      }
    });
    return this;
  }

  /**
   * Set a custom {@link Env.Builder} to use.
   *
   * @param env A custom env builder.
   * @return This jooby instance.
   */
  public Jooby env(final Env.Builder env) {
    this.env = requireNonNull(env, "Env builder is required.");
    return this;
  }

  public <T> T require(final Class<T> type) {
    checkState(injector != null, "App didn't start yet");
    return injector.getInstance(type);
  }

  /**
   * Setup a session store to use. Useful if you want/need to persist sessions between shutdowns.
   * Sessions are not persisted by defaults.
   *
   * @param store A session store.
   * @return A session store definition.
   */
  public Session.Definition session(final Class<? extends Session.Store> store) {
    this.session = new Session.Definition(requireNonNull(store, "A session store is required."));
    return this.session;
  }

  /**
   * Setup a session store to use. Useful if you want/need to persist sessions between shutdowns.
   * Sessions are not persisted by defaults.
   *
   * @param store A session store.
   * @return A session store definition.
   */
  public Session.Definition session(final Session.Store store) {
    this.session = new Session.Definition(requireNonNull(store, "A session store is required."));
    return this.session;
  }

  /**
   * Register a new param converter. See {@link ParamConverter} for more details.
   *
   * @param converter A param converter.
   * @return This jooby instance.
   */
  public Jooby param(final ParamConverter converter) {
    converters.add(requireNonNull(converter, "A param converter is required."));
    return this;
  }

  /**
   * Append a body formatter for write HTTP messages.
   *
   * @param formatter A body formatter.
   * @return This jooby instance.
   */
  public Jooby use(final BodyFormatter formatter) {
    this.formatters.add(requireNonNull(formatter, "A body formatter is required."));
    return this;
  }

  /**
   * Append a body parser for write HTTP messages.
   *
   * @param parser A body parser.
   * @return This jooby instance.
   */
  public Jooby use(final BodyParser parser) {
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
  public Route.Definition use(final String path,
      final Route.Filter filter) {
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
  public Route.Definition use(final String verb, final String path,
      final Route.Filter filter) {
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
  public Route.Definition use(final String verb, final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition(verb, path, handler));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definition use(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("*", path, handler));
  }

  /**
   * Append a route that supports HTTP GET method:
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
  public Route.Definition get(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append two routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/mode/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2, final String path3,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler), get(path3, handler) });
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
  public Route.Definition get(final String path,
      final Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler), get(path3, handler) });
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
  public Route.Definition get(final String path,
      final Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler));
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, handler), get(path2, handler), get(path3, handler) });
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
  public Route.Definition get(final String path,
      final Route.Filter filter) {
    return appendDefinition(new Route.Definition("GET", path, filter));
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, filter), get(path2, filter) });
  }

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions get(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{get(path1, filter), get(path2, filter), get(path3, filter) });
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
  public Route.Definition post(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler), post(path3, handler) });
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
  public Route.Definition post(final String path,
      final Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler), post(path3, handler) });
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
  public Route.Definition post(final String path,
      final Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("POST", path, handler));
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, handler), post(path2, handler), post(path3, handler) });
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
  public Route.Definition post(final String path,
      final Route.Filter filter) {
    return appendDefinition(new Route.Definition("POST", path, filter));
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, filter), post(path2, filter) });
  }

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions post(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{post(path1, filter), post(path2, filter), post(path3, filter) });
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
  public Route.Definition head(final String path, final Route.Handler handler) {
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
  public Route.Definition head(final String path,
      final Route.OneArgHandler handler) {
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
  public Route.Definition head(final String path,
      final Route.ZeroArgHandler handler) {
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
  public Route.Definition head(final String path,
      final Route.Filter filter) {
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
  public Route.Definition head() {
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
  public Route.Definition options(final String path,
      final Route.Handler handler) {
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
  public Route.Definition options(final String path,
      final Route.OneArgHandler handler) {
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
  public Route.Definition options(final String path,
      final Route.ZeroArgHandler handler) {
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
  public Route.Definition options(final String path,
      final Route.Filter filter) {
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
  public Route.Definition options() {
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
  public Route.Definition put(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler), put(path3, handler) });
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
  public Route.Definition put(final String path,
      final Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler), put(path3, handler) });
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
  public Route.Definition put(final String path,
      final Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("PUT", path, handler));
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, handler), put(path2, handler), put(path3, handler) });
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
  public Route.Definition put(final String path,
      final Route.Filter filter) {
    return appendDefinition(new Route.Definition("PUT", path, filter));
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, filter), put(path2, filter) });
  }

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions put(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{put(path1, filter), put(path2, filter), put(path3, filter) });
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
  public Route.Definition patch(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler),
            patch(path3, handler) });
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
  public Route.Definition patch(final String path,
      final Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler),
            patch(path3, handler) });
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
  public Route.Definition patch(final String path,
      final Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("PATCH", path, handler));
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, handler), patch(path2, handler),
            patch(path3, handler) });
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
  public Route.Definition patch(final String path,
      final Route.Filter filter) {
    return appendDefinition(new Route.Definition("PATCH", path, filter));
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, filter), patch(path2, filter) });
  }

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions patch(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{patch(path1, filter), patch(path2, filter),
            patch(path3, filter) });
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
  public Route.Definition delete(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2,
      final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler),
            delete(path3, handler) });
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
  public Route.Definition delete(final String path,
      final Route.OneArgHandler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final String path3,
      final Route.OneArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler),
            delete(path3, handler) });
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
  public Route.Definition delete(final String path,
      final Route.ZeroArgHandler handler) {
    return appendDefinition(new Route.Definition("DELETE", path, handler));
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler) });
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final String path3,
      final Route.ZeroArgHandler handler) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, handler), delete(path2, handler),
            delete(path3, handler) });
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
  public Route.Definition delete(final String path,
      final Route.Filter filter) {
    return appendDefinition(new Route.Definition("DELETE", path, filter));
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, filter), delete(path2, filter) });
  }

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  public Route.Definitions delete(final String path1,
      final String path2, final String path3,
      final Route.Filter filter) {
    return new Route.Definitions(
        new Route.Definition[]{delete(path1, filter), delete(path2, filter),
            delete(path3, filter) });
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
  public Route.Definition trace(final String path,
      final Route.Handler handler) {
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
  public Route.Definition trace(final String path,
      final Route.OneArgHandler handler) {
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
  public Route.Definition trace(final String path,
      final Route.ZeroArgHandler handler) {
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
  public Route.Definition trace(final String path,
      final Route.Filter filter) {
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
  public Route.Definition trace() {
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
  public Route.Definition connect(final String path,
      final Route.Handler handler) {
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
  public Route.Definition connect(final String path,
      final Route.OneArgHandler handler) {
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
  public Route.Definition connect(final String path,
      final Route.ZeroArgHandler handler) {
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
  public Route.Definition connect(final String path,
      final Route.Filter filter) {
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
  private Route.Handler handler(final Class<? extends Route.Handler> handler) {
    requireNonNull(handler, "Route handler is required.");
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
  private Route.Filter filter(final Class<? extends Route.Filter> filter) {
    requireNonNull(filter, "Filter is required.");
    return (req, rsp, chain) -> req.require(filter).handle(req, rsp, chain);
  }

  /**
   * Send a static file.
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
  public Route.Definition assets(final String path) {
    return assets(path, "/");
  }

  /**
   * Send a static file.
   *
   * <p>Basic example</p>
   *
   * <pre>
   *   assets("/js/**", "/");
   * </pre>
   *
   * A request for: <code>/js/jquery.js</code> will be translated to: <code>/lib/jquery.js</code>.
   *
   * <p>Webjars example:</p>
   * <pre>
   *   assets("/js/**", "/resources/webjars/{0}");
   * </pre>
   *
   * A request for: <code>/js/jquery/2.1.3/jquery.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * The <code>{0}</code> represent the <code>**</code> capturing group.
   *
   * <p>Another webjars example:</p>
   * <pre>
   *   assets("/js/*-*.js", "/resources/webjars/{0}/{1}/{0}.js");
   * </pre>
   *
   * <p>
   * A request for: <code>/js/jquery-2.1.3.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * </p>
   *
   * @param path The path to publish.
   * @param location A resource location.
   * @return A new route definition.
   */
  public Route.Definition assets(final String path, final String location) {
    if (!assetFormatter) {
      formatters.add(new AssetFormatter());
      assetFormatter = true;
    }
    return get(path, new AssetHandler(location, getClass()));
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
   * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}.
   * </p>
   *
   * @param routeClass A route(s) class.
   * @return This jooby instance.
   */
  public Jooby use(final Class<?> routeClass) {
    requireNonNull(routeClass, "Route class is required.");
    bag.add(routeClass);
    return this;
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
  public Jooby use(final Jooby.Module module) {
    requireNonNull(module, "A module is required.");
    modules.add(module);
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
  public Jooby use(final Config config) {
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
  public Jooby err(final Err.Handler err) {
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
  public WebSocket.Definition ws(final String path,
      final WebSocket.Handler handler) {
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
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

    this.injector = bootstrap();

    Config config = injector.getInstance(Config.class);

    log.debug("config tree:\n{}", configTree(config.origin().description()));

    // Start server
    Server server = injector.getInstance(Server.class);
    String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();

    server.start();
    long end = System.currentTimeMillis();

    log.info("[{}@{}]: {} server started in {}ms\n\n{}\n",
        config.getString("application.env"),
        serverName,
        getClass().getSimpleName(),
        end - start,
        injector.getInstance(AppPrinter.class));

    boolean join = config.hasPath("server.join") ? config.getBoolean("server.join") : true;
    if (join) {
      server.join();
    }
  }

  private String configTree(final String description) {
    return configTree(description.split(":\\s+\\d+,|,"), 0);
  }

  private String configTree(final String[] sources, final int i) {
    if (i < sources.length) {
      return new StringBuilder()
          .append(Strings.padStart("", i, ' '))
          .append("└── ")
          .append(sources[i])
          .append("\n")
          .append(configTree(sources, i + 1))
          .toString();
    }
    return "";
  }

  private Injector bootstrap() throws Exception {
    Config config = buildConfig(
        Optional.ofNullable(this.source)
            .orElseGet(
                () -> ConfigFactory.parseResources("application.conf")
            )
        );
    Env env = this.env.build(config);

    final Charset charset = Charset.forName(config.getString("application.charset"));

    final Locale locale = LocaleUtils.toLocale(config.getString("application.lang"));

    String dateFormat = config.getString("application.dateFormat");
    ZoneId zoneId = ZoneId.of(config.getString("application.tz"));
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter
        .ofPattern(dateFormat, locale)
        .withZone(zoneId);

    DecimalFormat numberFormat = new DecimalFormat(config.getString("application.numberFormat"));

    // Guice Stage
    Stage stage = "dev".equals(env.name()) ? Stage.DEVELOPMENT : Stage.PRODUCTION;

    // dependency injection
    @SuppressWarnings("unchecked")
    Injector injector = Guice.createInjector(stage, binder -> {

      // type converters
        new TypeConverters().configure(binder);

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
        binder.bind(DateTimeFormatter.class).toInstance(dateTimeFormatter);

        // bind number format
        binder.bind(NumberFormat.class).toInstance(numberFormat);
        binder.bind(DecimalFormat.class).toInstance(numberFormat);

        // bind managed
        binder.bindListener(Matchers.any(), new LifecycleProcessor());

        // bind formatter & parser
        Multibinder<BodyParser> parserBinder = Multibinder
            .newSetBinder(binder, BodyParser.class);
        Multibinder<BodyFormatter> formatterBinder = Multibinder
            .newSetBinder(binder, BodyFormatter.class);

        // Routes
        Multibinder<Route.Definition> definitions = Multibinder
            .newSetBinder(binder, Route.Definition.class);

        // Web Sockets
        Multibinder<WebSocket.Definition> sockets = Multibinder
            .newSetBinder(binder, WebSocket.Definition.class);

        // tmp dir
        File tmpdir = new File(config.getString("application.tmpdir"));
        tmpdir.mkdirs();
        binder.bind(File.class).annotatedWith(Names.named("application.tmpdir"))
            .toInstance(tmpdir);

        RouteMetadata classInfo = new RouteMetadata(env);
        binder.bind(RouteMetadata.class).toInstance(classInfo);

        converters.add(new CommonTypesParamConverter());
        converters.add(new CollectionParamConverter());
        converters.add(new OptionalParamConverter());
        converters.add(new UploadParamConverter());
        converters.add(new EnumParamConverter());
        converters.add(new DateParamConverter(dateFormat));
        converters.add(new LocalDateParamConverter(dateTimeFormatter));
        converters.add(new LocaleParamConverter());
        converters.add(new StaticMethodParamConverter("valueOf"));
        converters.add(new StaticMethodParamConverter("fromString"));
        converters.add(new StaticMethodParamConverter("forName"));
        converters.add(new StringConstructorParamConverter());

        binder.bind(ParamResolver.class);

        Multibinder<ParamConverter> converterBinder = Multibinder
            .newSetBinder(binder, ParamConverter.class);
        converters.forEach(it -> converterBinder.addBinding().toInstance(it));

        // modules, routes and websockets
        bag.forEach(candidate -> {
          if (candidate instanceof Jooby.Module) {
            install((Jooby.Module) candidate, env, config, binder);
          } else if (candidate instanceof Route.Definition) {
            definitions.addBinding().toInstance((Route.Definition) candidate);
          } else if (candidate instanceof WebSocket.Definition) {
            sockets.addBinding().toInstance((WebSocket.Definition) candidate);
          } else {
            binder.bind((Class<?>) candidate);
            MvcRoutes.routes(env, classInfo, (Class<?>) candidate)
                .forEach(route -> definitions.addBinding().toInstance(route));
          }
        });

        // parser & formatter
        parsers.forEach(it -> parserBinder.addBinding().toInstance(it));
        formatters.forEach(it -> formatterBinder.addBinding().toInstance(it));

        formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatReader);
        formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatStream);
        formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatByteArray);
        formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatByteBuffer);
        formatterBinder.addBinding().toInstance(BuiltinBodyConverter.formatAny);

        parserBinder.addBinding().toInstance(BuiltinBodyConverter.parseString);

        binder.bind(HttpHandler.class).to(HttpHandlerImpl.class).in(Singleton.class);

        RequestScope requestScope = new RequestScope();
        binder.bind(RequestScope.class).toInstance(requestScope);
        binder.bindScope(RequestScoped.class, requestScope);

        // session manager
        binder.bind(SessionManager.class).asEagerSingleton();
        binder.bind(Session.Definition.class).toInstance(session);
        Object sstore = session.store();
        if (sstore instanceof Class) {
          binder.bind(Session.Store.class).to((Class<? extends Store>) sstore)
              .asEagerSingleton();
        } else {
          binder.bind(Session.Store.class).toInstance((Store) sstore);;
        }

        binder.bind(Request.class).toProvider(() -> {
          throw new OutOfScopeException(Request.class.getName());
        }).in(RequestScoped.class);

        binder.bind(Response.class).toProvider(() -> {
          throw new OutOfScopeException(Response.class.getName());
        }).in(RequestScoped.class);

        binder.bind(Session.class).toProvider(() -> {
          throw new OutOfScopeException(Session.class.getName());
        }).in(RequestScoped.class);

        // err
        if (err == null) {
          binder.bind(Err.Handler.class).toInstance(new Err.Default());
        } else {
          binder.bind(Err.Handler.class).toInstance(err);
        }
      });

    return injector;
  }

  /**
   * Stop the application, close all the modules and stop the web server.
   */
  public void stop() {
    if (injector != null) {
      stopManaged();

      try {
        Server server = injector.getInstance(Server.class);
        server.stop();
        log.info("Server stopped");
      } catch (Exception ex) {
        log.error("Web server didn't stop normally", ex);
      }
      injector = null;
    }
  }

  private void stopManaged() {
    // stop modules
    LifecycleProcessor.onPreDestroy(injector, log);
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

    String env = Arrays.asList(system, source).stream()
        .filter(it -> it.hasPath("application.env"))
        .findFirst()
        .map(c -> c.getString("application.env"))
        .orElse("dev");

    Config modeConfig = modeConfig(source, env);

    // application.[env].conf -> application.conf
    Config config = modeConfig.withFallback(source);

    return system
        .withFallback(config)
        .withFallback(moduleStack)
        .withFallback(MediaType.types)
        .withFallback(defaultConfig(config, env))
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
    File froot = new File(fname);
    File fconfig = new File("config", fname);
    Config config = ConfigFactory.empty();
    if (froot.exists()) {
      config = config.withFallback(ConfigFactory.parseFile(froot));
    }
    if (fconfig.exists()) {
      config = config.withFallback(ConfigFactory.parseFile(fconfig));
    }
    return config;
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

    defaults.put("env", "dev");
    defaults.put("path", "/");
    defaults.put("host", "0.0.0.0");
    defaults.put("port", 8080);
    defaults.put("charset", "UTF-8");
    defaults.put("dateFormat", "dd-MM-yy");

    Builder<String, Object> session = ImmutableMap.<String, Object> builder()
        .put("cookie", ImmutableMap.<String, Object> builder()
            .put("name", "jooby.sid")
            .put("path", "/")
            .put("maxAge", -1)
            .put("httpOnly", true)
            .put("secure", false)
            .build()
        )
        .put("timeout", "30m")
        .put("saveInterval", "60s")
        .put("preserveOnStop", true);

    defaults.put("session", session.build());

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
      locale = LocaleUtils.toLocale(config.getString("application.lang"));
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

    int processors = Runtime.getRuntime().availableProcessors();
    Map<String, Object> runtime = ImmutableMap.<String, Object> builder()
        .put("processors", processors)
        .put("processors-plus1", processors + 1)
        .put("processors-plus2", processors + 2)
        .put("processors-x2", processors * 2)
        .build();

    Map<String, Object> application = ImmutableMap.<String, Object> builder()
        .put("application", defaults)
        .put("runtime", runtime)
        .build();
    return ConfigValueFactory.fromMap(application, "jooby-defaults").toConfig();
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
    // root nodes
    traverse(binder, "", config.root());

    // terminal nodes
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

  private void traverse(final Binder binder, final String p, final ConfigObject root) {
    root.forEach((n, v) -> {
      if (v instanceof ConfigObject) {
        ConfigObject child = (ConfigObject) v;
        String path = p + n;
        Named named = Names.named(path);
        binder.bind(Config.class).annotatedWith(named).toInstance(child.toConfig());
        traverse(binder, path + ".", child);
      }
    });
  }

}
