package io.jooby.openapi;

import io.jooby.internal.openapi.DebugOption;
import io.jooby.internal.openapi.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class OpenApiExtension implements ParameterResolver, AfterEachCallback {
  @Override public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    return parameter.getType() == RouteIterator.class || parameter.getType() == String.class;
  }

  @Override public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext context) throws ParameterResolutionException {
    AnnotatedElement method = context.getElement()
        .orElseThrow(() -> new IllegalStateException("Context: " + context));
    OpenApiTest metadata = method.getAnnotation(OpenApiTest.class);
    requireNonNull(metadata, "Missing @" + OpenApiTest.class.getName());
    String classname = metadata.value().getName();
    Set<DebugOption> debugOptions = metadata.debug().length == 0
        ? Collections.emptySet()
        : EnumSet.copyOf(Arrays.asList(metadata.debug()));

    OpenApiTool tool = newTool(debugOptions);
    Parameter parameter = parameterContext.getParameter();
    List<Operation> operations = new ArrayList<>();
    OpenAPI spec = tool.process(classname, operations::add);
    if (parameter.getType() == String.class) {
      return tool.toYaml(spec);
    }
    RouteIterator iterator = new RouteIterator(operations, metadata.ignoreArguments());
    getStore(context).put("iterator", iterator);
    return iterator;
  }

  @Override public void afterEach(ExtensionContext ctx) {
    RouteIterator iterator = (RouteIterator) getStore(ctx).get("iterator");
    if (iterator != null) {
      iterator.verify();
    }
  }

  private OpenApiTool newTool(Set<DebugOption> debug) {
    OpenApiTool tool = new OpenApiTool();
    Path basedir = Paths.get(System.getProperty("user.dir"));
    Path targetDir = basedir.resolve("target").resolve("test-classes");
    tool.setTargetDir(targetDir);
    tool.setDebug(debug);
    return tool;
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    Optional<Method> testMethod = context.getTestMethod();
    return context.getStore(
        ExtensionContext.Namespace.create(context.getRequiredTestClass(), testMethod.get()));
  }
}
