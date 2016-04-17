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
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.jooby.Route.Definition;
import org.jooby.Route.Mapper;
import org.jooby.Session.Store;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.AppPrinter;
import org.jooby.internal.AssetProxy;
import org.jooby.internal.BuiltinParser;
import org.jooby.internal.BuiltinRenderer;
import org.jooby.internal.DefaulErrRenderer;
import org.jooby.internal.HttpHandlerImpl;
import org.jooby.internal.JvmInfo;
import org.jooby.internal.LocaleUtils;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.RequestScope;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.ServerLookup;
import org.jooby.internal.SessionManager;
import org.jooby.internal.TypeConverters;
import org.jooby.internal.handlers.HeadHandler;
import org.jooby.internal.handlers.OptionsHandler;
import org.jooby.internal.handlers.TraceHandler;
import org.jooby.internal.js.JsJooby;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.internal.parser.BeanParser;
import org.jooby.internal.parser.DateParser;
import org.jooby.internal.parser.LocalDateParser;
import org.jooby.internal.parser.LocaleParser;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.internal.parser.StaticMethodParser;
import org.jooby.internal.parser.StringConstructorParser;
import org.jooby.internal.ssl.SslContextProvider;
import org.jooby.scope.Providers;
import org.jooby.scope.RequestScoped;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

import javaslang.Predicates;
import javaslang.control.Try;
import javaslang.control.Try.CheckedConsumer;
import javaslang.control.Try.CheckedRunnable;

/**
 * <h1>jooby</h1>
 * <p>
 * A new application must extends Jooby, register one ore more {@link Renderer} and some
 * {@link Route routes}. It sounds like a lot of work to do, but it isn't.
 * </p>
 *
 * <pre>
 * public class MyApp extends Jooby {
 *
 *   {
 *      renderer(new Json()); // 1. JSON serializer.
 *
 *      // 2. Define a route
 *      get("/", req {@literal ->} {
 *        Map{@literal <}String, Object{@literal >} model = ...;
 *        return model;
 *      }
 *   }
 *
 *  public static void main(String[] args) throws Exception {
 *    run(MyApp::new, args); // 3. Done!
 *  }
 * }
 * </pre>
 *
 * <h2>application.conf</h2>
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
 * <h2>env</h2>
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
 * <h2>modules: the jump to full-stack framework</h2>
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
 * <h2>path patterns</h2>
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
 * <h3>variables</h3>
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
 * <h2>routes</h2>
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
 * <h3>mvc routes</h3>
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
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes} .
 * </p>
 *
 * <h2>static files</h2>
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
 *
 * <h2>bootstrap</h2>
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
 * <li>Finally, Jooby starts the web server</li>
 * </ol>
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby.Module
 */
public class Jooby implements Routes, LifeCycle {

  /**
   * A module can publish or produces: {@link Route.Definition routes}, {@link Parser},
   * {@link Renderer}, and any other application specific service or contract of your choice.
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
     * @param conf The current config object. Not null.
     * @param binder A guice binder. Not null.
     */
    void configure(Env env, Config conf, Binder binder);

  }

  private static class MvcClass implements Route.Props<MvcClass> {
    Class<?> routeClass;

    String path;

    ImmutableMap.Builder<String, Object> attrs = ImmutableMap.builder();

    private List<MediaType> consumes;

    private String name;

    private List<MediaType> produces;

    private List<String> excludes;

    private Mapper<?> mapper;

    public MvcClass(final Class<?> routeClass, final String path) {
      this.routeClass = routeClass;
      this.path = path;
    }

    @Override
    public MvcClass attr(final String name, final Object value) {
      attrs.put(name, value);
      return this;
    }

    @Override
    public MvcClass name(final String name) {
      this.name = name;
      return this;
    }

    @Override
    public MvcClass consumes(final List<MediaType> consumes) {
      this.consumes = consumes;
      return this;
    }

    @Override
    public MvcClass produces(final List<MediaType> produces) {
      this.produces = produces;
      return this;
    }

    @Override
    public MvcClass excludes(final List<String> excludes) {
      this.excludes = excludes;
      return this;
    }

    @Override
    public MvcClass map(final Mapper<?> mapper) {
      this.mapper = mapper;
      return this;
    }

    public Route.Definition apply(final Route.Definition route) {
      attrs.build().forEach(route::attr);
      if (name != null) {
        route.name(name);
      }
      if (consumes != null) {
        route.consumes(consumes);
      }
      if (produces != null) {
        route.produces(produces);
      }
      if (excludes != null) {
        route.excludes(excludes);
      }
      if (mapper != null) {
        route.map(mapper);
      }
      return route;
    }
  }

  private static class EnvDep {
    Predicate<String> predicate;

    Consumer<Config> callback;

    public EnvDep(final Predicate<String> predicate, final Consumer<Config> callback) {
      this.predicate = predicate;
      this.callback = callback;
    }
  }

  static {
    // set pid as system property
    String pid = System.getProperty("pid", JvmInfo.pid() + "");
    System.setProperty("pid", pid);
  }

  /**
   * Keep track of routes.
   */
  private Set<Object> bag = new LinkedHashSet<>();

  /**
   * The override config. Optional.
   */
  private Config srcconf;

  private final AtomicBoolean started = new AtomicBoolean(false);

  /** Keep the global injector instance. */
  private Injector injector;

  /** Session store. */
  private Session.Definition session = new Session.Definition(Session.Mem.class);

  /** Env builder. */
  private Env.Builder env = Env.DEFAULT;

  /** Route's prefix. */
  private String prefix;

  /** startup callback . */
  private List<CheckedConsumer<Jooby>> onStart = new ArrayList<>();

  /** stop callback . */
  private List<CheckedConsumer<Jooby>> onStop = new ArrayList<>();

  public Jooby() {
    this(null);
  }

  /**
   * Creates a new application and prefix all the names of the routes with the given prefix. Useful,
   * for dynamic/advanced routing. See {@link Route.Chain#next(String, Request, Response)}.
   *
   * @param prefix Route name prefix.
   */
  public Jooby(final String prefix) {
    this.prefix = prefix;
    use(new ServerLookup());
  }

  /**
   * Import ALL the direct routes from the given app.
   *
   * <p>
   * PLEASE NOTE: that ONLY routes are imported.
   * </p>
   *
   * @param app Routes provider.
   * @return This jooby instance.
   */
  public Jooby use(final Jooby app) {
    return use(Optional.empty(), app);
  }

  /**
   * Import ALL the direct routes from the given app, under the given path.
   *
   * <p>
   * PLEASE NOTE: that ONLY routes are imported.
   * </p>
   *
   * @param path Path to mount the given app.
   * @param app Routes provider.
   * @return This jooby instance.
   */
  public Jooby use(final String path, final Jooby app) {
    return use(Optional.of(path), app);
  }

  /**
   * Import ALL the direct routes from the given app.
   *
   * <p>
   * PLEASE NOTE: that ONLY routes are imported.
   * </p>
   *
   * @param app Routes provider.
   * @return This jooby instance.
   */
  private Jooby use(final Optional<String> path, final Jooby app) {
    requireNonNull(app, "App is required.");

    Function<Route.Definition, Route.Definition> rewrite = r -> {
      return path.map(p -> {
        Route.Definition result = new Route.Definition(r.method(), p + r.pattern(), r.filter());
        result.consumes(r.consumes());
        result.produces(r.produces());
        result.excludes(r.excludes());
        return result;
      }).orElse(r);
    };

    app.bag.forEach(it -> {
      if (it instanceof Route.Definition) {
        this.bag.add(rewrite.apply((Definition) it));
      } else if (it instanceof Route.Group) {
        ((Route.Group) it).routes().forEach(r -> this.bag.add(rewrite.apply(r)));
      } else if (it instanceof MvcClass) {
        Object routes = path.<Object> map(p -> new MvcClass(((MvcClass) it).routeClass, p))
            .orElse(it);
        this.bag.add(routes);
      } else if (it instanceof EnvDep) {
        this.bag.add(it);
      }
    });
    return this;
  }

  /**
   * Define one or more routes under the same namespace:
   *
   * <pre>
   * {
   *   use("/pets")
   *     .get("/{id}", req {@literal ->} db.get(req.param("id").value()))
   *     .get(() {@literal ->} db.values());
   * }
   * </pre>
   *
   * @param pattern Global pattern to use.
   * @return A route namespace.
   */
  @Override
  public Route.Group use(final String pattern) {
    Route.Group group = new Route.Group(pattern, prefix);
    this.bag.add(group);
    return group;
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

  /**
   * Run code at application startup time.
   *
   * @param callback A callback to run.
   * @return This instance.
   */
  @Override
  public Jooby onStart(final CheckedRunnable callback) {
    requireNonNull(callback, "Callback is required.");
    return onStart(a -> callback.run());
  }

  /**
   * Run code at application startup time.
   *
   * @param callback A callback to run.
   * @return This instance.
   */
  @Override
  public Jooby onStart(final CheckedConsumer<Jooby> callback) {
    requireNonNull(callback, "Callback is required.");
    onStart.add(callback);
    return this;
  }

  /**
   * Run code at application shutdown time.
   *
   * @param callback A callback to run.
   * @return This instance.
   */
  @Override
  public Jooby onStop(final CheckedRunnable callback) {
    requireNonNull(callback, "Callback is required.");
    return onStop(e -> callback.run());
  }

  /**
   * Run code at application shutdown time.
   *
   * @param callback A callback to run.
   * @return This instance.
   */
  @Override
  public Jooby onStop(final CheckedConsumer<Jooby> callback) {
    requireNonNull(callback, "Callback is required.");
    onStop.add(callback);
    return this;
  }

  /**
   * Run the given callback if and only if, app runs in the given enviroment.
   *
   * <pre>
   * {
   *   on("dev", () {@literal ->} {
   *     use(new DevModule());
   *   });
   * }
   * </pre>
   *
   * @param env Environment where we want to run the callback.
   * @param callback An env callback.
   * @return This jooby instance.
   */
  public Jooby on(final String env, final Runnable callback) {
    requireNonNull(env, "Env is required.");
    return on(envpredicate(env), callback);
  }

  /**
   * Run the given callback if and only if, app runs in the given enviroment.
   *
   * <pre>
   * {
   *   on("dev", () {@literal ->} {
   *     use(new DevModule());
   *   });
   * }
   * </pre>
   *
   * @param env Environment where we want to run the callback.
   * @param callback An env callback.
   * @return This jooby instance.
   */
  public Jooby on(final String env, final Consumer<Config> callback) {
    requireNonNull(env, "Env is required.");
    return on(envpredicate(env), callback);
  }

  /**
   * Run the given callback if and only if, app runs in the given enviroment.
   *
   * <pre>
   * {
   *   on("dev", "test", () {@literal ->} {
   *     use(new DevModule());
   *   });
   * }
   * </pre>
   *
   * @param predicate Predicate to check the environment.
   * @param callback An env callback.
   * @return This jooby instance.
   */
  public Jooby on(final Predicate<String> predicate, final Runnable callback) {
    requireNonNull(predicate, "Predicate is required.");
    requireNonNull(callback, "Callback is required.");

    return on(predicate, conf -> callback.run());
  }

  /**
   * Run the given callback if and only if, app runs in the given enviroment.
   *
   * <pre>
   * {
   *   on("dev", "test", () {@literal ->} {
   *     use(new DevModule());
   *   });
   * }
   * </pre>
   *
   * @param predicate Predicate to check the environment.
   * @param callback An env callback.
   * @return This jooby instance.
   */
  public Jooby on(final Predicate<String> predicate, final Consumer<Config> callback) {
    requireNonNull(predicate, "Predicate is required.");
    requireNonNull(callback, "Callback is required.");
    this.bag.add(new EnvDep(predicate, callback));

    return this;
  }

  /**
   * Run the given callback if and only if, app runs in the given enviroment.
   *
   * <pre>
   * {
   *   on("dev", "test", "mock", () {@literal ->} {
   *     use(new DevModule());
   *   });
   * }
   * </pre>
   *
   * @param env1 Environment where we want to run the callback.
   * @param env2 Environment where we want to run the callback.
   * @param env3 Environment where we want to run the callback.
   * @param callback An env callback.
   * @return This jooby instance.
   */
  public Jooby on(final String env1, final String env2, final String env3,
      final Runnable callback) {
    on(envpredicate(env1).or(envpredicate(env2)).or(envpredicate(env3)), callback);
    return this;
  }

  /**
   * Ask Guice for the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  public <T> T require(final Class<T> type) {
    checkState(injector != null, "App didn't start yet");
    return injector.getInstance(type);
  }

  /**
   * Produces a deferred response, useful for async request processing.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(() {@literal ->} {
   *        try {
   *          deferred.resolve(...); // success value
   *        } catch (Exception ex) {
   *          deferred.reject(ex); // error value
   *        }
   *      });
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * Or with automatic error handler:
   * </p>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(() {@literal ->} {
   *        deferred.resolve(() {@literal ->} {
   *          Object value = ...
   *          return value;
   *        }); // success value
   *      });
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * Or as {@link Runnable} with automatic error handler:
   * </p>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(deferred.run(() {@literal ->} {
   *        Object value = ...
   *        return value;
   *      }); // success value
   *    }));
   *  }
   * </pre>
   *
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  public Route.OneArgHandler promise(final Deferred.Initializer initializer) {
    return req -> {
      return new Deferred(req, initializer);
    };
  }

  /**
   * Produces a deferred response, useful for async request processing.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(() {@literal ->} {
   *        try {
   *          deferred.resolve(...); // success value
   *        } catch (Exception ex) {
   *          deferred.reject(ex); // error value
   *        }
   *      });
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * Or with automatic error handler:
   * </p>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(() {@literal ->} {
   *        deferred.resolve(() {@literal ->} {
   *          Object value = ...
   *          return value;
   *        }); // success value
   *      });
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * Or as {@link Runnable} with automatic error handler:
   * </p>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise(deferred {@literal ->} {
   *      executor.execute(deferred.run(() {@literal ->} {
   *        Object value = ...
   *        return value;
   *      }); // success value
   *    }));
   *  }
   * </pre>
   *
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  public Route.OneArgHandler promise(final Deferred.Initializer0 initializer) {
    return req -> {
      return new Deferred(initializer);
    };
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
   * Register a new param converter. See {@link Parser} for more details.
   *
   * @param parser A parser.
   * @return This jooby instance.
   */
  public Jooby parser(final Parser parser) {
    bag.add(requireNonNull(parser, "A parser is required."));
    return this;
  }

  /**
   * Append a response {@link Renderer} for write HTTP messages.
   *
   * @param renderer A renderer renderer.
   * @return This jooby instance.
   */
  public Jooby renderer(final Renderer renderer) {
    this.bag.add(requireNonNull(renderer, "A renderer is required."));
    return this;
  }

  @Override
  public Route.Definition before(final String method, final String pattern,
      final Route.Before handler) {
    return appendDefinition(new Route.Definition(method, pattern, handler));
  }

  @Override
  public Route.Definition after(final String method, final String pattern,
      final Route.After handler) {
    return appendDefinition(new Route.Definition(method, pattern, handler));
  }

  @Override
  public Route.Definition complete(final String method, final String pattern,
      final Route.Complete handler) {
    return appendDefinition(new Route.Definition(method, pattern, handler));
  }

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
  public Route.Definition use(final String path,
      final Route.Handler handler) {
    return appendDefinition(new Route.Definition("*", path, handler));
  }

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  @Override
  public Route.Definition use(final String path,
      final Route.OneArgHandler handler) {
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
  @Override
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection get(final String path1, final String path2, final String path3,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
  public Route.Collection get(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
  public Route.Collection post(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
  public Route.Collection put(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final String path3, final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final String path3, final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
  public Route.Collection patch(final String path1, final String path2,
      final String path3, final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2,
      final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2,
      final String path3, final Route.Handler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final String path3,
      final Route.OneArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final String path3,
      final Route.ZeroArgHandler handler) {
    return new Route.Collection(
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
  @Override
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
  public Route.Collection delete(final String path1,
      final String path2, final String path3,
      final Route.Filter filter) {
    return new Route.Collection(
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
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
  @Override
  public Route.Definition assets(final String path) {
    return assets(path, "/");
  }

  /**
   * Send a static file.
   *
   * <p>
   * Basic example
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/");
   * </pre>
   *
   * A request for: <code>/js/jquery.js</code> will be translated to: <code>/lib/jquery.js</code>.
   *
   * <p>
   * Webjars example:
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/resources/webjars/{0}");
   * </pre>
   *
   * A request for: <code>/js/jquery/2.1.3/jquery.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * The <code>{0}</code> represent the <code>**</code> capturing group.
   *
   * <p>
   * Another webjars example:
   * </p>
   *
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
  @Override
  public Route.Definition assets(final String path, final String location) {
    return assets(path, new AssetHandler(location));
  }

  /**
   * Send a static file.
   *
   * <p>
   * Basic example
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/");
   * </pre>
   *
   * A request for: <code>/js/jquery.js</code> will be translated to: <code>/lib/jquery.js</code>.
   *
   * <p>
   * Webjars example:
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/resources/webjars/{0}");
   * </pre>
   *
   * A request for: <code>/js/jquery/2.1.3/jquery.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * The <code>{0}</code> represent the <code>**</code> capturing group.
   *
   * <p>
   * Another webjars example:
   * </p>
   *
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
   * @param handler Asset handler.
   * @return A new route definition.
   */
  @Override
  public Route.Definition assets(final String path, final AssetHandler handler) {

    AssetProxy router = new AssetProxy();
    Route.Definition asset = new Route.Definition("GET", path, router);
    on("*", conf -> {
      router.fwd(handler
          .cdn(conf.getString("assets.cdn"))
          .lastModified(conf.getBoolean("assets.lastModified"))
          .etag(conf.getBoolean("assets.etag")));
    });
    return appendDefinition(asset);
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
  @Override
  public Route.Collection use(final Class<?> routeClass) {
    requireNonNull(routeClass, "Route class is required.");
    MvcClass mvc = new MvcClass(routeClass, "");
    bag.add(mvc);
    return new Route.Collection(mvc);
  }

  /**
   * Keep track of routes in the order user define them.
   *
   * @param route A route definition to append.
   * @return The same route definition.
   */
  private Route.Definition appendDefinition(final Route.Definition route) {
    if (prefix != null) {
      route.name(prefix + "/" + route.name());
    }
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
    this.srcconf = requireNonNull(config, "A config is required.");
    return this;
  }

  /**
   * Setup a route error handler. Default error handler {@link Err.DefHandler} does content
   * negotation and this method allow to override/complement default handler.
   *
   * @param err A route error handler.
   * @return This jooby instance.
   */
  public Jooby err(final Err.Handler err) {
    this.bag.add(requireNonNull(err, "An err handler is required."));
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
  @Override
  public WebSocket.Definition ws(final String path,
      final WebSocket.Handler handler) {
    WebSocket.Definition ws = new WebSocket.Definition(path, handler);
    checkArgument(bag.add(ws), "Path is in use: '%s'", path);
    return ws;
  }

  @Override
  public Route.Definition sse(final String path, final Sse.Handler handler) {
    return appendDefinition(new Route.Definition("GET", path, handler)).consumes(MediaType.sse);
  }

  @Override
  public Route.Definition sse(final String path, final Sse.Handler1 handler) {
    return appendDefinition(new Route.Definition("GET", path, handler)).consumes(MediaType.sse);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Route.Collection with(final Runnable callback) {
    // hacky way of doing what we want... but we do simplify developer life
    int size = this.bag.size();
    callback.run();
    // collect latest routes and apply route props
    List<Route.Props> local = this.bag.stream()
        .skip(size)
        .filter(Predicates.instanceOf(Route.Props.class))
        .map(r -> (Route.Props) r)
        .collect(Collectors.toList());
    return new Route.Collection(local.toArray(new Route.Props[local.size()]));
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @param app App creator.
   * @param args App arguments.
   * @throws Throwable If something fails to start.
   */
  public static void run(final Supplier<? extends Jooby> app, final String... args)
      throws Throwable {
    Config conf = ConfigFactory.systemProperties()
        .withFallback(args(args));
    System.setProperty("logback.configurationFile", logback(conf));
    app.get().start(args);
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @param app App creator.
   * @param args App arguments.
   * @throws Throwable If something fails to start.
   */
  public static void run(final Class<? extends Jooby> app, final String... args)
      throws Throwable {
    run(() -> Try.of(() -> app.newInstance()).get(), args);
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @throws Throwable If something fails to start.
   */
  public void start() throws Throwable {
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @param routes Routes callback. Invoked once all app routes has been collected.
   * @throws Throwable If something fails to start.
   */
  public void start(final Consumer<List<Route.Definition>> routes) throws Throwable {
    start(new String[0], routes);
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @param args Application arguments. Using the <code>name=value</code> format, except for
   *        application.env where can be just: <code>myenv</code>.
   * @throws Throwable If something fails to start.
   */
  public void start(final String[] args) throws Throwable {
    start(args, null);
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
   * <li>A web server is started</li>
   * </ol>
   *
   * @param args Application arguments. Using the <code>name=value</code> format, except for
   *        application.env where can be just: <code>myenv</code>.
   * @param routes Routes callback. Invoked once all app routes has been collected.
   * @throws Throwable If something fails to start.
   */
  public void start(final String[] args, final Consumer<List<Route.Definition>> routes)
      throws Throwable {
    long start = System.currentTimeMillis();
    // shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

    this.injector = bootstrap(args(args), routes);

    started.set(true);

    Config config = injector.getInstance(Config.class);

    Logger log = LoggerFactory.getLogger(getClass());
    log.debug("config tree:\n{}", configTree(config.origin().description()));

    // start services
    for (CheckedConsumer<Jooby> onStart : this.onStart) {
      onStart.accept(this);
    }

    // Start server
    Server server = injector.getInstance(Server.class);
    String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();

    server.start();
    long end = System.currentTimeMillis();

    log.info("[{}@{}]: Server started in {}ms\n\n{}\n",
        config.getString("application.env"),
        serverName,
        end - start,
        injector.getInstance(AppPrinter.class));

    boolean join = config.hasPath("server.join") ? config.getBoolean("server.join") : true;
    if (join) {
      server.join();
    }
  }

  /**
   * Run app in javascript.
   *
   * @param jsargs Arguments, first arg must be the name of the javascript file.
   * @throws Throwable If app fails to start.
   */
  public static void main(final String[] jsargs) throws Throwable {
    String[] args = new String[Math.max(0, jsargs.length - 1)];
    if (args.length > 0) {
      System.arraycopy(jsargs, 1, args, 0, args.length);
    }
    String filename = jsargs.length > 0 ? jsargs[0] : "app.js";
    run(new JsJooby().run(new File(filename)), args);
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

  private static List<Object> normalize(final List<Object> services, final Env env,
      final RouteMetadata classInfo,
      final String prefix) {
    List<Object> result = new ArrayList<>();
    List<Object> snapshot = services;
    /** modules, routes, parsers, renderers and websockets */
    snapshot.forEach(candidate -> {
      if (candidate instanceof Route.Definition) {
        result.add(candidate);
      } else if (candidate instanceof Route.Group) {
        ((Route.Group) candidate).routes()
            .forEach(r -> result.add(r));
      } else if (candidate instanceof MvcClass) {
        MvcClass mvcRoute = ((MvcClass) candidate);
        Class<?> mvcClass = mvcRoute.routeClass;
        String path = ((MvcClass) candidate).path;
        MvcRoutes.routes(env, classInfo, path, mvcClass).forEach(route -> {
          mvcRoute.apply(route);
          if (prefix != null) {
            route.name(prefix + "/" + route.name());
          }
          result.add(route);
        });
      } else {
        result.add(candidate);
      }
    });
    return result;
  }

  private static List<Object> processEnvDep(final Set<Object> src, final Env env) {
    List<Object> result = new ArrayList<>();
    List<Object> bag = new ArrayList<>(src);
    bag.forEach(it -> {
      if (it instanceof EnvDep) {
        EnvDep envdep = (EnvDep) it;
        if (envdep.predicate.test(env.name())) {
          int from = src.size();
          envdep.callback.accept(env.config());
          int to = src.size();
          result.addAll(new ArrayList<>(src).subList(from, to));
        }
      } else {
        result.add(it);
      }
    });
    return result;
  }

  private Injector bootstrap(final Config args,
      final Consumer<List<Route.Definition>> rcallback) throws Exception {
    Config appconf = ConfigFactory.parseResources("application.conf");
    Config initconf = srcconf == null ? appconf : srcconf.withFallback(appconf);
    List<Config> modconf = modconf(this.bag);
    Config conf = buildConfig(initconf, args, modconf);

    final List<Locale> locales = LocaleUtils.parse(conf.getString("application.lang"));

    Env env = this.env.build(conf, this, locales.get(0));
    String envname = env.name();

    final Charset charset = Charset.forName(conf.getString("application.charset"));

    String dateFormat = conf.getString("application.dateFormat");
    ZoneId zoneId = ZoneId.of(conf.getString("application.tz"));
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter
        .ofPattern(dateFormat, locales.get(0))
        .withZone(zoneId);

    DecimalFormat numberFormat = new DecimalFormat(conf.getString("application.numberFormat"));

    // Guice Stage
    Stage stage = "dev".equals(envname) ? Stage.DEVELOPMENT : Stage.PRODUCTION;

    // expand and normalize bag
    RouteMetadata rm = new RouteMetadata(env);
    List<Object> realbag = processEnvDep(this.bag, env);
    List<Config> realmodconf = modconf(realbag);
    List<Object> bag = normalize(realbag, env, rm, prefix);

    // collect routes and fire route callback
    List<Route.Definition> routes = bag.stream()
        .filter(it -> it instanceof Route.Definition)
        .map(it -> (Route.Definition) it)
        .collect(Collectors.<Route.Definition> toList());
    if (rcallback != null) {
      rcallback.accept(routes);
    }

    // final config ? if we add a mod that depends on env
    Config finalConfig;
    Env finalEnv;
    if (modconf.size() != realmodconf.size()) {
      finalConfig = buildConfig(initconf, args, realmodconf);
      finalEnv = this.env.build(finalConfig, this, locales.get(0));
    } else {
      finalConfig = conf;
      finalEnv = env;
    }

    /** dependency injection */
    @SuppressWarnings("unchecked")
    Injector injector = Guice.createInjector(stage, binder -> {

      /** type converters */
      new TypeConverters().configure(binder);

      /** bind config */
      bindConfig(binder, finalConfig);

      /** bind env */
      binder.bind(Env.class).toInstance(finalEnv);

      /** bind charset */
      binder.bind(Charset.class).toInstance(charset);

      /** bind locale */
      binder.bind(Locale.class).toInstance(locales.get(0));
      TypeLiteral<List<Locale>> localeType = (TypeLiteral<List<Locale>>) TypeLiteral
          .get(Types.listOf(Locale.class));
      binder.bind(localeType).toInstance(locales);

      /** bind time zone */
      binder.bind(ZoneId.class).toInstance(zoneId);
      binder.bind(TimeZone.class).toInstance(TimeZone.getTimeZone(zoneId));

      /** bind date format */
      binder.bind(DateTimeFormatter.class).toInstance(dateTimeFormatter);

      /** bind number format */
      binder.bind(NumberFormat.class).toInstance(numberFormat);
      binder.bind(DecimalFormat.class).toInstance(numberFormat);

      /** bind ssl provider. */
      binder.bind(SSLContext.class).toProvider(SslContextProvider.class);

      /** routes */
      Multibinder<Route.Definition> definitions = Multibinder
          .newSetBinder(binder, Route.Definition.class);

      /** web sockets */
      Multibinder<WebSocket.Definition> sockets = Multibinder
          .newSetBinder(binder, WebSocket.Definition.class);

      /** tmp dir */
      File tmpdir = new File(finalConfig.getString("application.tmpdir"));
      tmpdir.mkdirs();
      binder.bind(File.class).annotatedWith(Names.named("application.tmpdir"))
          .toInstance(tmpdir);

      binder.bind(ParameterNameProvider.class).toInstance(rm);

      /** err handler */
      Multibinder<Err.Handler> ehandlers = Multibinder
          .newSetBinder(binder, Err.Handler.class);

      /** parsers & renderers */
      Multibinder<Parser> parsers = Multibinder
          .newSetBinder(binder, Parser.class);

      Multibinder<Renderer> renderers = Multibinder
          .newSetBinder(binder, Renderer.class);

      /** basic parser */
      parsers.addBinding().toInstance(BuiltinParser.Basic);
      parsers.addBinding().toInstance(BuiltinParser.Collection);
      parsers.addBinding().toInstance(BuiltinParser.Optional);
      parsers.addBinding().toInstance(BuiltinParser.Enum);
      parsers.addBinding().toInstance(BuiltinParser.Upload);
      parsers.addBinding().toInstance(BuiltinParser.Bytes);

      /** basic render */
      renderers.addBinding().toInstance(BuiltinRenderer.asset);
      renderers.addBinding().toInstance(BuiltinRenderer.bytes);
      renderers.addBinding().toInstance(BuiltinRenderer.byteBuffer);
      renderers.addBinding().toInstance(BuiltinRenderer.file);
      renderers.addBinding().toInstance(BuiltinRenderer.charBuffer);
      renderers.addBinding().toInstance(BuiltinRenderer.stream);
      renderers.addBinding().toInstance(BuiltinRenderer.reader);
      renderers.addBinding().toInstance(BuiltinRenderer.fileChannel);

      /** modules, routes, parsers, renderers and websockets */
      Set<Object> routeClasses = new HashSet<>();
      bag.forEach(it -> bindService(
          this.bag,
          finalConfig,
          finalEnv,
          rm,
          binder,
          definitions,
          sockets,
          ehandlers,
          parsers,
          renderers,
          routeClasses).accept(it));

      parsers.addBinding().toInstance(new DateParser(dateFormat));
      parsers.addBinding().toInstance(new LocalDateParser(dateTimeFormatter));
      parsers.addBinding().toInstance(new LocaleParser());
      parsers.addBinding().toInstance(new BeanParser());
      parsers.addBinding().toInstance(new StaticMethodParser("valueOf"));
      parsers.addBinding().toInstance(new StaticMethodParser("fromString"));
      parsers.addBinding().toInstance(new StaticMethodParser("forName"));
      parsers.addBinding().toInstance(new StringConstructorParser());

      binder.bind(ParserExecutor.class).in(Singleton.class);

      /** override(able) renderer */
      boolean stacktrace = finalConfig.hasPath("err.stacktrace")
          ? finalConfig.getBoolean("err.stacktrace")
          : "dev".equals(envname);
      renderers.addBinding().toInstance(new DefaulErrRenderer(stacktrace));
      renderers.addBinding().toInstance(BuiltinRenderer.text);

      binder.bind(HttpHandler.class).to(HttpHandlerImpl.class).in(Singleton.class);

      RequestScope requestScope = new RequestScope();
      binder.bind(RequestScope.class).toInstance(requestScope);
      binder.bindScope(RequestScoped.class, requestScope);

      /** session manager */
      binder.bind(SessionManager.class).asEagerSingleton();
      binder.bind(Session.Definition.class).toInstance(session);
      Object sstore = session.store();
      if (sstore instanceof Class) {
        binder.bind(Session.Store.class).to((Class<? extends Store>) sstore)
            .asEagerSingleton();
      } else {
        binder.bind(Session.Store.class).toInstance((Store) sstore);
      }

      binder.bind(Request.class).toProvider(Providers.outOfScope(Request.class))
          .in(RequestScoped.class);
      binder.bind(Response.class).toProvider(Providers.outOfScope(Response.class))
          .in(RequestScoped.class);
      /** server sent event */
      binder.bind(Sse.class).toProvider(Providers.outOfScope(Sse.class))
          .in(RequestScoped.class);

      binder.bind(Session.class).toProvider(Providers.outOfScope(Session.class))
          .in(RequestScoped.class);

      /** def err */
      ehandlers.addBinding().toInstance(new Err.DefHandler());
    });

    onStart.addAll(0, finalEnv.startTasks());
    onStop.addAll(finalEnv.stopTasks());

    // clear bag and freeze it
    this.bag.clear();
    this.bag = ImmutableSet.of();

    return injector;
  }

  private static Consumer<? super Object> bindService(final Set<Object> src,
      final Config conf,
      final Env env,
      final RouteMetadata rm,
      final Binder binder,
      final Multibinder<Route.Definition> definitions,
      final Multibinder<WebSocket.Definition> sockets,
      final Multibinder<Err.Handler> ehandlers,
      final Multibinder<Parser> parsers,
      final Multibinder<Renderer> renderers,
      final Set<Object> routeClasses) {
    return it -> {
      if (it instanceof Jooby.Module) {
        int from = src.size();
        install((Jooby.Module) it, env, conf, binder);
        int to = src.size();
        // collect any route a module might add
        if (to > from) {
          normalize(new ArrayList<>(src).subList(from, to), env, rm, null)
              .forEach(e -> bindService(src,
                  conf,
                  env,
                  rm,
                  binder,
                  definitions,
                  sockets,
                  ehandlers,
                  parsers,
                  renderers,
                  routeClasses).accept(e));
        }
      } else if (it instanceof Route.Definition) {
        Route.Definition rdef = (Definition) it;
        Route.Filter h = rdef.filter();
        if (h instanceof Route.MethodHandler) {
          Class<?> routeClass = ((Route.MethodHandler) h).method().getDeclaringClass();
          if (routeClasses.add(routeClass)) {
            binder.bind(routeClass);
          }
        }
        definitions.addBinding().toInstance(rdef);
      } else if (it instanceof WebSocket.Definition) {
        sockets.addBinding().toInstance((WebSocket.Definition) it);
      } else if (it instanceof Parser) {
        parsers.addBinding().toInstance((Parser) it);
      } else if (it instanceof Renderer) {
        renderers.addBinding().toInstance((Renderer) it);
      } else {
        ehandlers.addBinding().toInstance((Err.Handler) it);
      }
    };
  }

  private static List<Config> modconf(final Collection<Object> bag) {
    return bag.stream()
        .filter(it -> it instanceof Jooby.Module)
        .map(it -> ((Jooby.Module) it).config())
        .filter(c -> !c.isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * Stop the application, close all the modules and stop the web server.
   */
  public void stop() {
    if (started.compareAndSet(true, false)) {
      Logger log = LoggerFactory.getLogger(getClass());
      fireStop(injector, this, log, onStop);

      try {
        Server server = injector.getInstance(Server.class);
        String serverName = server.getClass().getSimpleName().replace("Server", "").toLowerCase();
        server.stop();
        log.info("[{}] Server stopped", serverName);
      } catch (Throwable ex) {
        log.error("Web server didn't stop normally", ex);
      }
      injector = null;
    }
  }

  private static void fireStop(final Injector injector, final Jooby app, final Logger log,
      final List<CheckedConsumer<Jooby>> onStop) {
    // stop services
    onStop.forEach(c -> {
      try {
        c.accept(app);
      } catch (Throwable x) {
        log.error("shutdown of {} resulted in error", c, x);
      }
    });
  }

  /**
   * Build configuration properties, it configure system, app and modules properties.
   *
   * @param source Source config to use.
   * @param args Args conf.
   * @param modules List of modules.
   * @return A configuration properties ready to use.
   */
  private Config buildConfig(final Config source, final Config args,
      final List<Config> modules) {
    // normalize tmpdir
    Config system = ConfigFactory.systemProperties();
    Config tmpdir = source.hasPath("java.io.tmpdir") ? source : system;

    // system properties
    system = system
        // file encoding got corrupted sometimes, override it.
        .withValue("file.encoding", fromAnyRef(System.getProperty("file.encoding")))
        .withValue("java.io.tmpdir",
            fromAnyRef(Paths.get(tmpdir.getString("java.io.tmpdir")).normalize().toString()));

    // set module config
    Config moduleStack = ConfigFactory.empty();
    for (Config module : ImmutableList.copyOf(modules).reverse()) {
      moduleStack = moduleStack.withFallback(module);
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
        .withFallback(args)
        .withFallback(config)
        .withFallback(moduleStack)
        .withFallback(MediaType.types)
        .withFallback(defaultConfig(config))
        .resolve();
  }

  /**
   * Build a conf from arguments.
   *
   * @param args Application arguments.
   * @return A conf.
   */
  static Config args(final String[] args) {
    if (args == null || args.length == 0) {
      return ConfigFactory.empty();
    }
    Map<String, String> conf = new HashMap<>();
    for (String arg : args) {
      String[] values = arg.split("=");
      String name;
      String value;
      if (values.length == 2) {
        name = values[0];
        value = values[1];
      } else {
        name = "application.env";
        value = values[0];
      }
      if (name.indexOf(".") == -1) {
        conf.put("application." + name, value);
      }
      conf.put(name, value);
    }
    return ConfigFactory.parseMap(conf, "args");
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
  static Config fileConfig(final String fname) {
    File dir = new File(System.getProperty("user.dir"));
    File froot = new File(dir, fname);
    if (froot.exists()) {
      return ConfigFactory.parseFile(froot);
    } else {
      File fconfig = new File(new File(dir, "conf"), fname);
      if (fconfig.exists()) {
        return ConfigFactory.parseFile(fconfig);
      }
    }
    return ConfigFactory.empty();
  }

  /**
   * Build default application.* properties.
   *
   * @param config A source config.
   * @return default properties.
   */
  private Config defaultConfig(final Config config) {
    String ns = getClass().getPackage().getName();
    String[] parts = ns.split("\\.");
    String appname = parts[parts.length - 1];

    // locale
    final List<Locale> locales;
    if (!config.hasPath("application.lang")) {
      locales = ImmutableList.of(Locale.getDefault());
    } else {
      locales = LocaleUtils.parse(config.getString("application.lang"));
    }
    Locale locale = locales.iterator().next();
    String lang = locale.toLanguageTag();

    // time zone
    final String tz;
    if (!config.hasPath("application.tz")) {
      tz = ZoneId.systemDefault().getId();
    } else {
      tz = config.getString("application.tz");
    }

    // number format
    final String nf;
    if (!config.hasPath("application.numberFormat")) {
      nf = ((DecimalFormat) DecimalFormat.getInstance(locale)).toPattern();
    } else {
      nf = config.getString("application.numberFormat");
    }

    int processors = Runtime.getRuntime().availableProcessors();
    String version = Optional.ofNullable(getClass().getPackage().getImplementationVersion())
        .orElse("0.0.0");
    Config defs = ConfigFactory.parseResources(Jooby.class, "jooby.conf")
        .withValue("application.name", ConfigValueFactory.fromAnyRef(appname))
        .withValue("application.version", ConfigValueFactory.fromAnyRef(version))
        .withValue("application.class", ConfigValueFactory.fromAnyRef(getClass().getName()))
        .withValue("application.ns", ConfigValueFactory.fromAnyRef(ns))
        .withValue("application.lang", ConfigValueFactory.fromAnyRef(lang))
        .withValue("application.tz", ConfigValueFactory.fromAnyRef(tz))
        .withValue("application.numberFormat", ConfigValueFactory.fromAnyRef(nf))
        .withValue("runtime.processors", ConfigValueFactory.fromAnyRef(processors))
        .withValue("runtime.processors-plus1", ConfigValueFactory.fromAnyRef(processors + 1))
        .withValue("runtime.processors-plus2", ConfigValueFactory.fromAnyRef(processors + 2))
        .withValue("runtime.processors-x2", ConfigValueFactory.fromAnyRef(processors * 2))
        .withValue("runtime.concurrencyLevel", ConfigValueFactory
            .fromAnyRef(Math.max(4, processors)));

    return defs;
  }

  /**
   * Install a {@link JoobyModule}.
   *
   * @param module The module to install.
   * @param env Application env.
   * @param config The configuration object.
   * @param binder A Guice binder.
   */
  private static void install(final Jooby.Module module, final Env env, final Config config,
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
        Type listType = values.size() == 0
            ? String.class
            : Types.listOf(values.iterator().next().getClass());
        Key<Object> key = (Key<Object>) Key.get(listType, Names.named(name));
        binder.bind(key).toInstance(values);
      } else {
        binder.bindConstant().annotatedWith(named).to(value.toString());
      }
    }
    // bind config
    binder.bind(Config.class).toInstance(config);
  }

  private static void traverse(final Binder binder, final String p, final ConfigObject root) {
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

  private static Predicate<String> envpredicate(final String candidate) {
    return env -> env.equalsIgnoreCase(candidate) || candidate.equals("*");
  }

  static String logback(final Config conf) {
    // Avoid warning message from logback when multiples files are present
    String logback;
    if (conf.hasPath("logback.configurationFile")) {
      logback = conf.getString("logback.configurationFile");
    } else {
      ImmutableList.Builder<File> files = ImmutableList.builder();
      File userdir = new File(System.getProperty("user.dir"));
      File confdir = new File(userdir, "conf");
      if (conf.hasPath("application.env")) {
        String env = conf.getString("application.env");
        files.add(new File(userdir, "logback." + env + ".xml"));
        files.add(new File(confdir, "logback." + env + ".xml"));
      }
      files.add(new File(userdir, "logback.xml"));
      files.add(new File(confdir, "logback.xml"));
      logback = files.build()
          .stream()
          .filter(f -> f.exists())
          .map(f -> f.getAbsolutePath())
          .findFirst()
          .orElse("logback.xml");
    }
    return logback;
  }

}
