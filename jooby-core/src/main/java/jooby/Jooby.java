package jooby;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jooby.internal.StringMessageConverter;
import jooby.internal.mvc.Routes;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class Jooby {

  private Set<Object> routes = new LinkedHashSet<>();

  private Set<JoobyModule> modules = new LinkedHashSet<>();

  private Config config;

  public RouteDefinition get(final String path, final Route route) {
    return route(new RouteDefinition("GET", path, route));
  }

  public RouteDefinition post(final String path, final Route route) {
    return route(new RouteDefinition("POST", path, route));
  }

  public void route(final Class<?> route) {
    routes.add(route);
  }

  public RouteDefinition route(final RouteDefinition route) {
    routes.add(route);
    return route;
  }

  public Jooby use(final JoobyModule module) {
    modules.add(module);
    return this;
  }

  public Jooby use(final Config config) {
    this.config = requireNonNull(config, "A config is required.");
    return this;
  }

  public void start() throws Exception {
    config = buildConfig(Optional.ofNullable(config));

    // shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      modules.forEach(m -> {
        try {
          m.stop();
        } catch (Exception ex) {
          // TODO:
          ex.printStackTrace();
        }
      });
    }));

    final Charset charset = Charset.forName(config.getString("jooby.charset"));

    // dependency injection
    Injector injector = Guice.createInjector(new Module() {
      @Override
      public void configure(final Binder binder) {
        // bind config
        bindConfig(binder, config);

        // bind mode
        Mode mode = mode(config.getString("application.mode").toLowerCase());
        binder.bind(Mode.class).toInstance(mode);

        // bind charset
        binder.bind(Charset.class).toInstance(charset);

        // bind readers & writers
        Multibinder<BodyConverter> converters = Multibinder
            .newSetBinder(binder, BodyConverter.class);

        // Routes
        Multibinder<RouteDefinition> definitions = Multibinder
            .newSetBinder(binder, RouteDefinition.class);

        // Request Modules
        Multibinder.newSetBinder(binder, RequestModule.class);

        // Route Interceptors
        Multibinder.newSetBinder(binder, RouteInterceptor.class);

        // work dir
        binder.bind(File.class).annotatedWith(Names.named("jooby.workDir"))
            .toInstance(new File(config.getString("jooby.workDir")));

        // configure module
        modules.forEach(m -> {
          install(m, mode, config, binder);
        });

        // Routes
        routes.forEach(route -> {
          if (route instanceof RouteDefinition) {
            definitions.addBinding().toInstance((RouteDefinition) route);
          } else {
            Class<?> routeClass = (Class<?>) route;
            Routes.route(mode, routeClass)
                .forEach(mvcRoute -> definitions.addBinding().toInstance(mvcRoute));
          }
        });

        converters.addBinding().toInstance(new StringMessageConverter(MediaType.plain));
      }

    });

    // start modules
    for (JoobyModule module : modules) {
      module.start();
    }

    // Start server
    Server server = injector.getInstance(Server.class);

    server.start();
  }

  private Config buildConfig(final Optional<Config> appConfig) {
    Config sysProps = ConfigFactory.defaultOverrides()
        .withValue("file.encoding",
            ConfigValueFactory.fromAnyRef(System.getProperty("file.encoding")));

    // app configuration
    Supplier<Config> defaults = () -> ConfigFactory.parseResources("application.conf");
    Config config = sysProps
        .withFallback(appConfig.orElseGet(defaults));

    // set app anme
    if (!config.hasPath("application.name")) {
      config = config.withValue("application.name",
          ConfigValueFactory.fromAnyRef(getClass().getSimpleName()));
    }

    // set default mode
    if (!config.hasPath("application.mode")) {
      config = config.withValue("application.mode", ConfigValueFactory.fromAnyRef("dev"));
    }

    // set default charset, if app config didn't set it
    if (!config.hasPath("jooby.charset")) {
      config = config.withValue("jooby.charset",
          ConfigValueFactory.fromAnyRef(Charset.defaultCharset().name()));
    }

    // set work dir
    if (!config.hasPath("jooby.workDir")) {
      config = config.withValue("jooby.workDir",
          ConfigValueFactory.fromAnyRef(System.getProperty("java.io.tmpdir")));
    }

    // set module config
    for (JoobyModule module : modules) {
      config = config.withFallback(module.config());
    }

    // resolve config
    config = config
        .withFallback(ConfigFactory.parseResources("jooby.conf"));

    return config.resolve();
  }

  private void install(final JoobyModule module, final Mode mode, final Config config,
      final Binder binder) {
    try {
      module.configure(mode, config, binder);
    } catch (Exception ex) {
      throw new IllegalStateException("Module didn't start properly: " + module.name(), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private void bindConfig(final Binder binder, final Config config) {
    for (Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue().unwrapped();
      Class<Object> type = (Class<Object>) value.getClass();
      if (type.isAssignableFrom(CharSequence.class)) {
        binder.bindConstant().annotatedWith(Names.named(name)).to(value.toString());
      } else {
        binder.bind(Key.get(type, Names.named(name))).toInstance(value);
      }
    }
    // bind config
    binder.bind(Config.class).toInstance(config);
  }

  private static Mode mode(final String name) {
    return new Mode() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }
}
