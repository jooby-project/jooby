/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.camel;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.SimpleMain;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.camel.JoobyCamelContext;
import io.jooby.internal.camel.SingletonProvider;

/**
 * EIP using Camel: https://camel.apache.org.
 *
 * <pre>{@code
 * {
 *    install(new CamelModule(new MyRoutes()));
 *
 *    get("/", ctx -> {
 *
 *      Producer producer = require(Producer.class);
 *      ...
 *    });
 * }
 *
 * class MyRoutes extends RouteBuilder {
 *
 *   public void configure() throws Exception {
 *     from("direct://foo")
 *         .log(">>> ${body}");
 *   }
 * }
 *
 * }</pre>
 *
 * <p>Module integrates <code>application.conf</code> properties into Camel as well as {@link
 * ServiceRegistry} services. See https://jooby.io/modules/camel/
 *
 * @author edgar
 * @since 3.0.0
 */
public class CamelModule implements Extension {

  private Function<CamelContext, List<RouteBuilder>> routes;

  private CamelContext camel;

  /** Creates module using the {@link CamelContext}. */
  public CamelModule() {}

  /**
   * Creates module using the {@link CamelContext}.
   *
   * @param camel Camel context.
   */
  public CamelModule(@NonNull CamelContext camel) {
    this.camel = camel;
  }

  /**
   * Creates a new camel module adding one or more routes.
   *
   * @param route Route configuration.
   * @param routes Optional route configuration.
   */
  public CamelModule(@NonNull RouteBuilder route, RouteBuilder... routes) {
    this(null, route, routes);
  }

  /**
   * Creates a new camel module adding one or more routes.
   *
   * @param route Route configuration.
   * @param routes Optional route configuration.
   */
  public CamelModule(
      @NonNull CamelContext camel, @NonNull RouteBuilder route, RouteBuilder... routes) {
    this.camel = camel;
    this.routes = registry -> concat(route, routes).collect(Collectors.toList());
  }

  /**
   * Creates a new camel module adding one or more routes. Route provisioning is delegated to
   * Dependency Injection framework (if any), otherwise camel does basic/minimal injection using
   * {@link org.apache.camel.impl.engine.DefaultInjector}.
   *
   * @param route Route configuration.
   * @param routes Optional route configuration.
   */
  public CamelModule(
      @NonNull Class<? extends RouteBuilder> route,
      @NonNull Class<? extends RouteBuilder>... routes) {
    this(null, route, routes);
  }

  /**
   * Creates a new camel module adding one or more routes. Route provisioning is delegated to
   * Dependency Injection framework (if any), otherwise camel does basic/minimal injection using
   * {@link org.apache.camel.impl.engine.DefaultInjector}.
   *
   * @param route Route configuration.
   * @param routes Optional route configuration.
   */
  public CamelModule(
      @NonNull CamelContext camel,
      @NonNull Class<? extends RouteBuilder> route,
      Class<? extends RouteBuilder>... routes) {
    this.camel = camel;
    this.routes =
        context ->
            concat(route, routes)
                .map(type -> context.getInjector().newInstance(type))
                .collect(Collectors.toList());
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    // create a CamelContext
    if (this.camel == null) {
      this.camel = newCamelContext(application);
    }
    // Producer
    SingletonProvider<ProducerTemplate> producerTemplate =
        new SingletonProvider<>(camel::createProducerTemplate, ProducerTemplate::close);
    SingletonProvider<FluentProducerTemplate> fluentProducerTemplate =
        new SingletonProvider<>(camel::createFluentProducerTemplate, FluentProducerTemplate::stop);
    // Consumer
    SingletonProvider<ConsumerTemplate> consumerTemplate =
        new SingletonProvider<>(camel::createConsumerTemplate, ConsumerTemplate::stop);

    ServiceRegistry services = application.getServices();
    services.putIfAbsent(CamelContext.class, camel);
    if (camel instanceof DefaultCamelContext) {
      services.putIfAbsent(DefaultCamelContext.class, (DefaultCamelContext) camel);
    }
    // Producer
    services.putIfAbsent(ProducerTemplate.class, producerTemplate);
    services.putIfAbsent(FluentProducerTemplate.class, fluentProducerTemplate);
    // Consumer
    services.putIfAbsent(ConsumerTemplate.class, consumerTemplate);

    SimpleMain main = new SimpleMain(camel);

    application.onStarting(
        () -> {
          // build camel
          main.init();

          // Do routes
          if (routes != null) {
            List<RouteBuilder> routeList = routes.apply(camel);
            for (RouteBuilder route : routeList) {
              camel.addRoutes(route);
            }
          }
          // Start camel:
          main.start();
        });

    // Shutdown
    application.onStop(producerTemplate::close);
    application.onStop(fluentProducerTemplate::close);
    application.onStop(consumerTemplate::close);

    application.onStop(main::stop);
  }

  /**
   * Creates a new camel context from appplication.
   *
   * @param application Application.
   * @return CamelContext.
   */
  public static CamelContext newCamelContext(Jooby application) {
    return new JoobyCamelContext(application);
  }

  private static <T> Stream<T> concat(T route, T[] routes) {
    return Stream.concat(Stream.of(route), Stream.of(routes));
  }
}
