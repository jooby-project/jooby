package io.jooby.openapi;

import io.jooby.internal.openapi.OpenAPIExt;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class OpenAPIExtension implements ParameterResolver, AfterEachCallback {
  @Override public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    return parameter.getType() == RouteIterator.class || parameter.getType() == OpenAPIResult.class;
  }

  @Override public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext context) throws ParameterResolutionException {
    AnnotatedElement method = context.getElement()
        .orElseThrow(() -> new IllegalStateException("Context: " + context));
    OpenAPITest metadata = method.getAnnotation(OpenAPITest.class);
    String classname = metadata.value().getName();
    Set<DebugOption> debugOptions = metadata.debug().length == 0
        ? Collections.emptySet()
        : EnumSet.copyOf(Arrays.asList(metadata.debug()));

    OpenAPIGenerator tool = newTool(debugOptions);
    Parameter parameter = parameterContext.getParameter();
    OpenAPIExt openAPI = (OpenAPIExt) tool.generate(classname);
    OpenAPIResult result = new OpenAPIResult(openAPI);
    if (parameter.getType() == OpenAPIResult.class) {
      return result;
    }
    RouteIterator iterator = result.iterator(metadata.ignoreArguments());
    getStore(context).put("iterator", iterator);
    return iterator;
  }

  @Override public void afterEach(ExtensionContext ctx) {
    RouteIterator iterator = (RouteIterator) getStore(ctx).get("iterator");
    if (iterator != null) {
      iterator.verify();
    }
  }

  private OpenAPIGenerator newTool(Set<DebugOption> debug) {
    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setDebug(debug);
    return tool;
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    Optional<Method> testMethod = context.getTestMethod();
    return context.getStore(
        ExtensionContext.Namespace.create(context.getRequiredTestClass(), testMethod.get()));
  }
}
