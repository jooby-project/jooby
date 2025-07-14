/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static io.jooby.SneakyThrows.throwingConsumer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.Server;
import io.jooby.SneakyThrows;

/**
 * JUnit extension that control lifecycle of Jooby applications on tests. The extension shouldn't
 * use it directly. Usage is done via {@link JoobyTest} annotation.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyExtension
    implements BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback,
        ParameterResolver,
        TestInstancePostProcessor {

  private static final int DEFAULT_PORT = 8911;

  static {
    System.setProperty("jooby.useShutdownHook", "false");
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    context
        .getElement()
        .ifPresent(
            throwingConsumer(
                element -> {
                  JoobyTest metadata = element.getAnnotation(JoobyTest.class);
                  if (metadata != null) {
                    startApp(context, metadata);
                  }
                }));
  }

  private Jooby startApp(ExtensionContext context, JoobyTest metadata) throws Exception {
    var server = Server.loadServer();
    var serverOptions = server.getOptions();
    serverOptions.setPort(port(metadata.port(), DEFAULT_PORT));
    server.setOptions(serverOptions);
    Jooby app;
    String factoryMethod = metadata.factoryMethod();
    if (factoryMethod.isEmpty()) {
      var defaultEnv = System.getProperty("application.env");
      System.setProperty("application.env", metadata.environment());
      app = Jooby.createApp(server, metadata.executionMode(), reflectionProvider(metadata.value()));
      if (defaultEnv != null) {
        System.setProperty("application.env", defaultEnv);
      } else {
        System.getProperties().remove("application.env");
      }
    } else {
      app = fromFactoryMethod(context, metadata, factoryMethod);
    }
    server.start(app);
    ExtensionContext.Store store = getStore(context);
    store.put("server", server);
    store.put("application", app);
    return app;
  }

  private static Supplier<Jooby> reflectionProvider(
      @NonNull Class<? extends Jooby> applicationType) {
    return () ->
        (Jooby)
            Stream.of(applicationType.getDeclaredConstructors())
                .filter(it -> it.getParameterCount() == 0)
                .findFirst()
                .map(SneakyThrows.throwingFunction(c -> c.newInstance()))
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Default constructor for: " + applicationType.getName()));
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    context
        .getElement()
        .ifPresent(
            throwingConsumer(
                element -> {
                  JoobyTest metadata = element.getAnnotation(JoobyTest.class);
                  if (metadata != null) {
                    startApp(context, metadata);
                  }
                }));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Server server = getStore(context).get("server", Server.class);
    if (server != null) {
      server.stop();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Server server = getStore(context).get("server", Server.class);
    if (server != null) {
      server.stop();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return joobyParameter(context, parameterContext) != null;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return joobyParameter(context, parameterContext).get();
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    Optional<Method> testMethod = context.getTestMethod();
    ExtensionContext.Namespace namespace =
        testMethod
            .map(m -> ExtensionContext.Namespace.create(context.getRequiredTestClass(), m))
            .orElseGet(() -> ExtensionContext.Namespace.create(context.getRequiredTestClass()));
    return context.getStore(namespace);
  }

  private Supplier<Object> joobyParameter(
      ExtensionContext context, ParameterContext parameterContext) {
    Parameter parameter = parameterContext.getParameter();
    return injectionPoint(context, parameter.getType(), () -> parameterName(parameter));
  }

  private String parameterName(Parameter parameter) {
    if (!parameter.isNamePresent()) {
      throw new IllegalStateException(
          "Parameter injection requires parameter names to be present "
              + "at runtime. Make sure compiler has the -parameters option");
    }
    return parameter.getName();
  }

  private Supplier<Object> injectionPoint(
      ExtensionContext context, Class type, Supplier<String> name) {
    if (Jooby.class.isAssignableFrom(type)) {
      return () -> application(context);
    }
    if (Environment.class.isAssignableFrom(type)) {
      return () -> application(context).getEnvironment();
    }
    if (Config.class.isAssignableFrom(type)) {
      return () -> application(context).getEnvironment().getConfig();
    }
    if (type == String.class && name.get().equals("serverPath")) {
      return () -> {
        var app = application(context);
        var server = server(context);
        return "http://localhost:" + server.getOptions().getPort() + app.getContextPath();
      };
    }
    if (type == int.class && name.get().equals("serverPort")) {
      return () -> {
        var server = server(context);
        return server.getOptions().getPort();
      };
    }
    return null;
  }

  private Jooby application(ExtensionContext context) {
    return service(context, "application", Jooby.class);
  }

  private Server server(ExtensionContext context) {
    return service(context, "server", Server.class);
  }

  private <T> T service(ExtensionContext context, String name, Class<T> type) {
    var service = getStore(context).get(name, type);
    if (service == null) {
      var parent =
          context
              .getParent()
              .orElseThrow(() -> new IllegalStateException("Not found: " + type.getSimpleName()));
      service = getStore(parent).get(name, type);
    }
    if (service == null) {
      throw new IllegalStateException("Not found: " + type.getSimpleName());
    }
    return service;
  }

  private int port(int port, int fallback) {
    return port == -1 ? fallback : port;
  }

  @Override
  public void postProcessTestInstance(Object instance, ExtensionContext context) throws Exception {
    for (Field field : instance.getClass().getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        Supplier<Object> injectionPoint =
            injectionPoint(context, field.getType(), () -> field.getName());
        if (injectionPoint != null) {
          field.setAccessible(true);
          field.set(instance, injectionPoint.get());
        }
      }
    }
  }

  private Jooby fromFactoryMethod(
      ExtensionContext context, JoobyTest metadata, String factoryMethod) throws Exception {
    Class<?> factoryClass = metadata.factoryClass();
    if (factoryClass == Object.class) {
      factoryClass = context.getRequiredTestClass();
    }
    try {
      Method factory = factoryClass.getMethod(factoryMethod);
      Class<?> returnType = factory.getReturnType();
      if (!Jooby.class.isAssignableFrom(returnType)) {
        throw new IllegalStateException(
            "Factory method must return a Jooby application: " + factory);
      }
      if (Modifier.isStatic(factory.getModifiers())) {
        return (Jooby) factory.invoke(null);

      } else {
        return (Jooby) factory.invoke(context.getRequiredTestInstance());
      }
    } catch (InvocationTargetException e) {
      throw SneakyThrows.propagate(e);
    }
  }
}
