/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Paths;
import java.util.*;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.jooby.internal.openapi.OpenAPIExt;

public class OpenAPIExtension implements ParameterResolver, AfterEachCallback {

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    return parameter.getType() == RouteIterator.class
        || parameter.getType() == OpenAPIResult.class
        || parameter.getType() == OpenAPIExt.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    AnnotatedElement method =
        context.getElement().orElseThrow(() -> new IllegalStateException("Context: " + context));
    OpenAPITest metadata = method.getAnnotation(OpenAPITest.class);
    var klass = metadata.value();
    String classname = klass.getName();
    Set<DebugOption> debugOptions =
        metadata.debug().length == 0
            ? Collections.emptySet()
            : EnumSet.copyOf(Arrays.asList(metadata.debug()));

    OpenAPIGenerator tool = newTool(debugOptions);
    tool.setSpecVersion(metadata.version().name());
    String templateName = metadata.templateName();
    if (templateName.isEmpty()) {
      templateName = classname.replace(".", "/").toLowerCase() + ".yaml";
    }
    tool.setTemplateName(templateName);
    if (!metadata.includes().isEmpty()) {
      tool.setIncludes(metadata.includes());
    }
    if (!metadata.excludes().isEmpty()) {
      tool.setExcludes(metadata.excludes());
    }
    Parameter parameter = parameterContext.getParameter();
    OpenAPIResult result;
    try {
      OpenAPIExt openAPI = (OpenAPIExt) tool.generate(classname);
      result = new OpenAPIResult(tool.jsonMapper(), tool.yamlMapper(), openAPI);
    } catch (RuntimeException re) {
      result = OpenAPIResult.failure(re);
    }
    if (parameter.getType() == OpenAPIResult.class) {
      return result;
    }
    if (parameter.getType() == OpenAPIExt.class) {
      return result.getOpenAPI();
    }
    RouteIterator iterator = result.iterator(metadata.ignoreArguments());
    getStore(context).put("iterator", iterator);
    return iterator;
  }

  @Override
  public void afterEach(ExtensionContext ctx) {
    RouteIterator iterator = (RouteIterator) getStore(ctx).get("iterator");
    if (iterator != null) {
      iterator.verify();
    }
  }

  private OpenAPIGenerator newTool(Set<DebugOption> debug) {
    OpenAPIGenerator tool = new OpenAPIGenerator();
    tool.setDebug(debug);
    var baseDir = Paths.get(System.getProperty("user.dir"));
    if (!baseDir.getFileName().toString().endsWith("openapi")) {
      baseDir = baseDir.resolve("modules").resolve("jooby-openapi");
    }
    tool.setSources(List.of(baseDir.resolve("src").resolve("test").resolve("java")));
    return tool;
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    Optional<Method> testMethod = context.getTestMethod();
    return context.getStore(
        ExtensionContext.Namespace.create(context.getRequiredTestClass(), testMethod.get()));
  }
}
