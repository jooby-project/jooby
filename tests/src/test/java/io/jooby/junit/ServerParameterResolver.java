package io.jooby.junit;

import io.jooby.ExecutionMode;
import io.jooby.Server;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ServerParameterResolver implements ParameterResolver {

  private final Supplier<Server> server;

  private final ExecutionMode executionMode;


  public ServerParameterResolver(Supplier<Server> server, ExecutionMode executionMode) {
    this.server = server;
    this.executionMode = executionMode;
  }

  @Override public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == ServerTestRunner.class;
  }

  @Override public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    Method test = extensionContext.getRequiredTestMethod();
    return new ServerTestRunner(test.getDeclaringClass().getSimpleName() + "." + test.getName(), server, executionMode);
  }
}
