/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JUnit extension that control lifecycle of Jooby applications on tests. The extension shouldn't
 * use it directly. Usage is done via {@link JoobyTest} annotation.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JoobyExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback,
    AfterEachCallback, ParameterResolver, TestInstancePostProcessor {

  private static final int DEFAULT_PORT = 8911;

  static {
    System.setProperty("jooby.useShutdownHook", "false");
  }

  @Override public void beforeAll(ExtensionContext context) throws Exception {
    context.getElement().ifPresent(element -> {
      JoobyTest metadata = element.getAnnotation(JoobyTest.class);
      if (metadata != null) {
        startApp(context, metadata.value(), metadata.environment(),
            port(metadata.port(), DEFAULT_PORT), metadata.executionMode());
      }
    });
  }

  private Jooby startApp(ExtensionContext context, Class applicationClass,
      String environment, int port, ExecutionMode executionMode) {
    Jooby app = Jooby.createApp(new String[]{environment}, executionMode, applicationClass);
    ServerOptions serverOptions = app.getServerOptions();
    if (serverOptions == null) {
      serverOptions = new ServerOptions();
      app.setServerOptions(serverOptions);
    }
    serverOptions.setPort(port);
    Server server = app.start();
    ExtensionContext.Store store = getStore(context);
    store.put("server", server);
    store.put("application", app);
    return app;
  }

  @Override public void beforeEach(ExtensionContext context) throws Exception {
    context.getElement().ifPresent(element -> {
      JoobyTest metadata = element.getAnnotation(JoobyTest.class);
      if (metadata != null) {
        startApp(context, metadata.value(), metadata.environment(),
            port(metadata.port(), 0), metadata.executionMode());
      }
    });
  }

  @Override public void afterAll(ExtensionContext context) throws Exception {
    Server server = getStore(context).get("server", Server.class);
    if (server != null) {
      server.stop();
    }
  }

  @Override public void afterEach(ExtensionContext context) throws Exception {
    Server server = getStore(context).get("server", Server.class);
    if (server != null) {
      server.stop();
    }
  }

  @Override public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext context) throws ParameterResolutionException {
    return joobyParameter(context, parameterContext) != null;
  }

  @Override public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext context) throws ParameterResolutionException {
    return joobyParameter(context, parameterContext).get();
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    Optional<Method> testMethod = context.getTestMethod();
    ExtensionContext.Namespace namespace = testMethod.isPresent()
        ? ExtensionContext.Namespace.create(context.getRequiredTestClass(), testMethod.get())
        : ExtensionContext.Namespace.create(context.getRequiredTestClass());
    return context.getStore(namespace);
  }

  private Supplier<Object> joobyParameter(ExtensionContext context,
      ParameterContext parameterContext) {
    Parameter parameter = parameterContext.getParameter();
    return injectionPoint(context, parameter.getType(), () -> parameterName(parameter));
  }

  private String parameterName(Parameter parameter) {
    if (!parameter.isNamePresent()) {
      throw new IllegalStateException("Parameter injection requires parameter names to be present "
          + "at runtime. Make sure compiler has the -parameters option");
    }
    return parameter.getName();
  }

  private Supplier<Object> injectionPoint(ExtensionContext context,
      Class type, Supplier<String> name) {
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
        Jooby app = application(context);
        return "http://localhost:" + app.getServerOptions().getPort() + app.getContextPath();
      };
    }
    if (type == int.class && name.get().equals("serverPort")) {
      return () -> {
        Jooby app = application(context);
        return app.getServerOptions().getPort();
      };
    }
    return null;
  }

  private Jooby application(ExtensionContext context) {
    Jooby application = getStore(context).get("application", Jooby.class);
    if (application == null) {
      ExtensionContext parent = context.getParent()
          .orElseThrow(() -> new IllegalStateException("Application not found"));
      application = getStore(parent).get("application", Jooby.class);
    }
    if (application == null) {
      throw new IllegalStateException("Application not found");
    }
    return application;
  }

  private int port(int port, int fallback) {
    return port == -1 ? fallback : port;
  }

  @Override public void postProcessTestInstance(Object instance, ExtensionContext context)
      throws Exception {
    for (Field field : instance.getClass().getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        Supplier<Object> injectionPoint = injectionPoint(context, field.getType(),
            () -> field.getName());
        if (injectionPoint != null) {
          field.setAccessible(true);
          field.set(instance, injectionPoint.get());
        }
      }
    }
  }
}
