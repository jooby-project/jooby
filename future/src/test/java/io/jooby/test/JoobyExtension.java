package io.jooby.test;

import io.jooby.App;
import io.jooby.Server;
import org.jooby.funzy.Throwing;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.net.ServerSocket;

public class JoobyExtension implements AfterAllCallback,
    ParameterResolver, BeforeAllCallback {
  private Class sharedClass;
  private App shared;
  private int port;
  private WebClient client;

  @Override public void beforeAll(ExtensionContext context) throws Exception {
    context.getTestClass()
        .ifPresent(Throwing.throwingConsumer(klass -> {
          JoobyUnit metadata = klass.getAnnotation(JoobyUnit.class);
          if (App.class.isAssignableFrom(klass)) {
            sharedClass = klass;
          }
          if (metadata != null) {
            if (metadata.value() != App.class) {
              sharedClass = metadata.value();
            }
            port = metadata.port();
          }
//          if (port < 1000) {
//            try (ServerSocket socket = new ServerSocket(0)) {
//              port = socket.getLocalPort();
//            }
//          }
        }));
    if (sharedClass != null) {
      shared = (App) sharedClass.newInstance();
      shared.start();
    }
  }

  @Override public void afterAll(ExtensionContext ctx) {
    if (shared != null) {
      shared.stop();
    }
  }

  @Override public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(WebClient.class);
  }

  @Override public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(WebClient.class)) {
      if (client == null) {
        client = new WebClient(port);
      }
      return client;
    }
    return null;
  }
}
