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
package org.jooby.camel;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.ThreadPoolProfile;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.camel.CamelFinalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Camel for Jooby. Exposes a {@link CamelContext}, {@link ProducerTemplate} and
 * {@link ConsumerTemplate}.
 *
 * <p>
 * NOTE: This module was designed to provide a better integration with Jooby. This module doesn't
 * depend on <a href="http://camel.apache.org/guice.html">camel-guice</a>, but it provides similar
 * features.
 * </p>
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Camel()
 *     .routes((rb, config) {@literal ->} {
 *       rb.from("direct:noop").to("mock:out");
 *     })
 *   );
 *
 *   get("/noop", req {@literal ->} {
 *     req.require(ProducerTemplate.class).sendBody("direct:noop", "NOOP");
 *     return "/noop";
 *   });
 *
 * }
 * </pre>
 *
 * <p>
 * Previous example, add a direct route using the Java DSL. A route builder can be created and
 * injected by Guice, see next section.
 * </p>
 *
 * <h1>camel routes</h1>
 * <pre>
 * public class MyRoutes extends RouteBuilder {
 *
 *   &#64;Inject
 *   public MyRoutes(Service service) {
 *     this.service = service;
 *   }
 *
 *   public void configure() {
 *     from("direct:noop").to("mock:out").bean(service);
 *   }
 * }
 *
 * ...
 * {
 *   use(new Camel().routes(MyRoutes.class));
 * }
 * </pre>
 *
 * <p>
 * or without extending RouteBuilder:
 * </p>
 *
 * <pre>
 * public class MyRoutes {
 *
 *   &#64;Inject
 *   public MyRoutes(RouteBuilder router, Service service) {
 *     router.from("direct:noop").to("mock:out").bean(service);
 *   }
 *
 * }
 *
 * ...
 * {
 *   use(new Camel().routes(MyRoutes.class));
 * }
 * </pre>
 *
 * <h1>configuration</h1>
 * <p>
 * Custom configuration is achieved in two ways:
 * </p>
 *
 * <h2>application.conf</h2>
 * <p>
 * A {@link CamelContext} can be configured from your <code>application.conf</code>:
 * </p>
 *
 * <pre>
 * camel.handleFault = false
 *
 * camel.shutdownRoute = Default
 *
 * camel.shutdownRunningTask = CompleteCurrentTaskOnly
 *
 * camel.streamCaching.enabled = false
 *
 * camel.tracing = false
 *
 * camel.autoStartup = true
 *
 * camel.allowUseOriginalMessage = false
 *
 * camel.jmx = false
 * </pre>
 *
 * <p>
 * Same for {@link ShutdownStrategy}:
 * </p>
 *
 * <pre>
 * camel.shutdown.shutdownRoutesInReverseOrder = true
 *
 * camel.shutdown.timeUnit = SECONDS
 *
 * camel.shutdown.timeout = 10
 * </pre>
 *
 * <p>
 * {@link ThreadPoolProfile}:
 * </p>
 *
 * <pre>
 * camel.threads.poolSize = ${runtime.processors-plus1}
 * camel.threads.maxPoolSize = ${runtime.processors-x2}
 * camel.threads.keepAliveTime = 60
 * camel.threads.timeUnit = SECONDS
 * camel.threads.rejectedPolicy = CallerRuns
 * camel.threads.maxQueueSize = 1000
 * camel.threads.id = default
 * camel.threads.defaultProfile = true
 * </pre>
 *
 * And {@link StreamCachingStrategy}:
 *
 * <pre>
 * camel.streamCaching.enabled = false
 * camel.streamCaching.spoolDirectory = ${application.tmpdir}${file.separator}"camel"${file.separator}"#uuid#"
 * </pre>
 *
 * <h2>programmatically</h2>
 * <p>
 * Using the {@link #doWith(Configurer)} method:
 * </p>
 *
 * <pre>
 * {
 *   use(new Camel().doWith((ctx, config) {@literal ->} {
 *     // set/override any other property.
 *   }));
 * }
 * </pre>
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
public class Camel implements Jooby.Module {

  public interface Configurer<T> {

    void configure(T ctx, Config config) throws Exception;

  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Configurer<CamelContext> configurer;

  private Configurer<RouteBuilder> routes;

  private List<Class<?>> routeList = new ArrayList<>();

  /**
   * Add a route builder (or alike) which will be injected by Guice and call it before starting the
   * {@link CamelContext}.
   *
   * <pre>
   * public class MyRoutes extends RouteBuilder {
   *
   *   &#64;Inject
   *   public MyRoutes(Service service) {
   *     this.service = service;
   *   }
   *
   *   public void configure() {
   *     from("direct:noop").to("mock:out").bean(service);
   *   }
   * }
   *
   * ...
   * {
   *   use(new Camel().routes(MyRoutes.class));
   * }
   * </pre>
   *
   * <p>
   * or without extending RouteBuilder:
   * </p>
   *
   * <pre>
   * public class MyRoutes {
   *
   *   &#64;Inject
   *   public MyRoutes(RouteBuilder router, Service service) {
   *     router.from("direct:noop").to("mock:out").bean(service);
   *   }
   *
   * }
   *
   * ...
   * {
   *   use(new Camel().routes(MyRoutes.class));
   * }
   * </pre>
   *
   * @param routeClass Type of routes.
   * @return This camel module.
   */
  public Camel routes(final Class<?> routeClass) {
    routeList.add(routeClass);
    return this;
  }

  public Camel doWith(final Configurer<CamelContext> configurer) {
    this.configurer = requireNonNull(configurer, "A configurer is required.");
    return this;
  }

  /**
   * Register one or more routes:
   *
   * <pre>
   * {
   *   use(new Camel()
   *     .routes((rb, config) {@literal ->} {
   *       rb.from("direct:noop").to("mock:out");
   *     })
   *   );
   *
   *   get("/noop", req {@literal ->} {
   *     req.require(ProducerTemplate.class).sendBody("direct:noop", "NOOP");
   *     return "/noop";
   *   });
   *
   * }
   * </pre>
   *
   * @param routes Route callback.
   * @return This camel module.
   */
  public Camel routes(final Configurer<RouteBuilder> routes) {
    this.routes = requireNonNull(routes, "Route configurer is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Config $camel = config.getConfig("camel");
    DefaultCamelContext ctx = configure(new DefaultCamelContext(), $camel
        .withoutPath("shutdown")
        .withoutPath("threads")
        .withoutPath("jmx")
        .withoutPath("streamCaching")
        );

    if (!$camel.getBoolean("jmx")) {
      ctx.disableJMX();
    }

    /**
     * Executor and thread poll
     */
    ThreadPoolProfile threadPool = configure(new ThreadPoolProfile(), $camel.getConfig("threads"));
    ctx.getExecutorServiceManager().setDefaultThreadPoolProfile(threadPool);

    /**
     * Shutdown options.
     */
    configure(ctx.getShutdownStrategy(), $camel.getConfig("shutdown"));

    if ($camel.getBoolean("streamCaching.enabled")) {
      ctx.setStreamCaching(true);
      configure(ctx.getStreamCachingStrategy(), $camel.getConfig("streamCaching"));
    } else {
      ctx.setStreamCaching(false);
    }

    /**
     * Components etc..
     */
    if (configurer != null) {
      try {
        configurer.configure(ctx, config);
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IllegalStateException("Context configurer resulted in error", ex);
      }
    }

    /**
     * Routes
     */
    if (routes != null) {
      try {
        routes(routes, ctx, config);
      } catch (Exception ex) {
        throw new IllegalStateException("Route builder resulted in error", ex);
      }
    }

    /**
     * Properties
     */
    PropertiesComponent properties = new PropertiesComponent(config.root().origin().description());
    properties.setIgnoreMissingLocation(true);
    properties.setPropertiesResolver((context, ignoreMissingLocation, location) -> {
      Properties props = new Properties();
      config.entrySet()
          .forEach(e -> props.setProperty(e.getKey(), e.getValue().unwrapped().toString()));
      return props;
    });
    properties.setPrefixToken("${");
    properties.setSuffixToken("}");
    ctx.addComponent("properties", properties);

    env.lifeCycle(CamelFinalizer.class);

    /**
     * Guice!
     */
    binder.bind(CamelContext.class).toInstance(ctx);
    binder.bind(DefaultCamelContext.class).toInstance(ctx);
    binder.bind(ProducerTemplate.class).toInstance(ctx.createProducerTemplate());
    binder.bind(ConsumerTemplate.class).toInstance(ctx.createConsumerTemplate());

    binder.bind(CamelFinalizer.class).asEagerSingleton();

    binder.bind(RouteBuilder.class).toInstance(rb());

    Multibinder<Object> routesBinder = Multibinder
        .newSetBinder(binder, Object.class, Names.named("camel.routes"));

    routeList.forEach(routeType -> routesBinder.addBinding().to(routeType));
  }

  private static RouteBuilder rb() {
    return new RouteBuilder() {

      @Override
      public void configure() throws Exception {
      }
    };
  }

  private static void routes(final Configurer<RouteBuilder> callback,
      final DefaultCamelContext ctx, final Config config) throws Exception {
    ctx.addRoutes(new RouteBuilder() {

      @Override
      public void configure() throws Exception {
        callback.configure(this, config);
      }
    });
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "camel.conf");
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private <T> T configure(final T source, final Config config) {
    List<Method> methods = Lists.newArrayList(source.getClass().getMethods());
    config.entrySet().forEach(o -> {
      String key = o.getKey();
      String setter = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, key);
      Object value = o.getValue().unwrapped();
      Optional<Method> result = methods.stream()
          .filter(m -> m.getName().equals(setter))
          .findFirst();
      if (result.isPresent()) {
        Method method = result.get();
        Class type = method.getParameterTypes()[0];
        if (Enum.class.isAssignableFrom(type)) {
          value = Enum.valueOf(type, value.toString());
        }
        if (Long.class.isAssignableFrom(type)) {
          value = ((Number) value).longValue();
        }
        if (File.class.isAssignableFrom(type)) {
          value = new File(value.toString());
        }
        try {
          method.invoke(source, value);
        } catch (Exception ex) {
          throw new IllegalArgumentException("Bad option: <" + value + "> for: " + method, ex);
        }
      } else {
        log.error("Unknown option camel.{} = {}", key, value);
      }
    });
    return source;
  }

}
