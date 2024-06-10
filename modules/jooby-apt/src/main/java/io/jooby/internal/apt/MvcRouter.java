/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.AnnotationSupport.findAnnotationByName;
import static java.util.Collections.emptyList;

import java.util.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.squareup.javapoet.*;
import io.jooby.apt.MvcContext;

public class MvcRouter {
  private final MvcContext context;

  /** MVC router class. */
  private final TypeElement clazz;

  /** MVC route methods. */
  private final Map<String, MvcRoute> routes = new LinkedHashMap<>();

  public MvcRouter(MvcContext context, TypeElement clazz) {
    this.context = context;
    this.clazz = clazz;
  }

  public MvcRouter(TypeElement clazz, MvcRouter parent) {
    this.context = parent.context;
    this.clazz = clazz;
    for (var e : parent.routes.entrySet()) {
      this.routes.put(e.getKey(), new MvcRoute(context, this, e.getValue()));
    }
  }

  public TypeElement getTargetType() {
    return clazz;
  }

  public String getGeneratedType() {
    return context.generateRouterName(getTargetType().getQualifiedName().toString());
  }

  public MvcRouter put(TypeElement httpMethod, ExecutableElement route) {
    var routeKey = route.toString();
    var existing = routes.get(routeKey);
    if (existing == null) {
      routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
    } else {
      if (existing.getMethod().getEnclosingElement().equals(getTargetType())) {
        existing.addHttpMethod(httpMethod);
      } else {
        // Favor override version of same method
        routes.put(routeKey, new MvcRoute(context, this, route).addHttpMethod(httpMethod));
      }
    }
    return this;
  }

  public List<MvcRoute> getRoutes() {
    return routes.values().stream().toList();
  }

  public boolean isAbstract() {
    return clazz.getModifiers().contains(Modifier.ABSTRACT);
  }

  public String getPackageName() {
    var classname = getGeneratedType();
    var pkgEnd = classname.lastIndexOf('.');
    return pkgEnd > 0 ? classname.substring(0, pkgEnd) : "";
  }

  /**
   * Generate the controller extension for MVC controller:
   *
   * <pre>{@code
   * public class Controller_ implements MvcExtension, MvcFactory {
   *     ....
   * }
   *
   * }</pre>
   *
   * @return
   */
  public JavaFile toSourceCode() {
    var environment = context.getProcessingEnvironment();
    var elements = environment.getElementUtils();

    var joobyType = TypeName.get(elements.getTypeElement("io.jooby.Jooby").asType());
    var contextType = TypeName.get(elements.getTypeElement("io.jooby.Context").asType());
    var mvcExtension = TypeName.get(elements.getTypeElement("io.jooby.MvcExtension").asType());
    var routerType = TypeName.get(getTargetType().asType());
    var functionProvider =
        ParameterizedTypeName.get(
            ClassName.get("java.util.function", "Function"), contextType, routerType);
    var jakartaProvider =
        ParameterizedTypeName.get(ClassName.get("jakarta.inject", "Provider"), routerType);
    var classType = ParameterizedTypeName.get(ClassName.get("java.lang", "Class"), routerType);

    var generateTypeName = context.generateRouterName(getTargetType().getSimpleName().toString());
    var source = TypeSpec.classBuilder(generateTypeName);
    source.addAnnotation(
        AnnotationSpec.builder(ClassName.get("io.jooby.annotation", "Generated"))
            .addMember("value", "$L.class", routerType.toString())
            .build());
    source.addModifiers(Modifier.PUBLIC);
    source.addSuperinterface(mvcExtension);

    // new Controller_() {}
    source.addMethod(defaultConstructor().build());

    // new Controller_(Controller instance) {}
    var instanceConstructor = MethodSpec.constructorBuilder();
    instanceConstructor.addModifiers(Modifier.PUBLIC);
    instanceConstructor.addParameter(ParameterSpec.builder(routerType, "instance").build());
    instanceConstructor.addStatement("this(ctx -> $N)", "instance");
    source.addMethod(instanceConstructor.build());

    // new Controller_(Class<Controller> type) {}
    var classConstructor = MethodSpec.constructorBuilder();
    classConstructor.addModifiers(Modifier.PUBLIC);
    classConstructor.addParameter(ParameterSpec.builder(classType, "type").build());
    classConstructor.addStatement("this(ctx -> ctx.require($L))", "type");
    source.addMethod(classConstructor.build());

    // new Controller_(Provider<Controller> provider) {}
    var jakartaProviderConstructor = MethodSpec.constructorBuilder();
    jakartaProviderConstructor.addModifiers(Modifier.PUBLIC);
    jakartaProviderConstructor.addParameter(
        ParameterSpec.builder(jakartaProvider, "provider").build());
    jakartaProviderConstructor.addStatement("this(ctx -> provider.get())");
    source.addMethod(jakartaProviderConstructor.build());

    // new Controller_(Function<Context, Controller> factory) {}
    var factoryConstructor = MethodSpec.constructorBuilder();
    factoryConstructor.addModifiers(Modifier.PUBLIC);
    factoryConstructor.addParameter(ParameterSpec.builder(functionProvider, "factory").build());
    factoryConstructor.addStatement("this.$1N = $1N", "factory");
    source.addMethod(factoryConstructor.build());

    var field = FieldSpec.builder(functionProvider, "factory", Modifier.FINAL, Modifier.PROTECTED);
    source.addField(field.build());

    var install = MethodSpec.methodBuilder("install");
    install.addModifiers(Modifier.PUBLIC);
    install.addException(Exception.class);
    install.addParameter(ParameterSpec.builder(joobyType, "app").build());

    var routes = this.routes.values();
    routes.stream().flatMap(it -> it.generateMapping().stream()).forEach(install::addCode);
    source.addMethod(install.build());

    routes.stream().map(MvcRoute::generateHandlerCall).forEach(source::addMethod);

    // TODO: remove at some point
    var mvcFactory = ParameterizedTypeName.get(ClassName.get("io.jooby", "MvcFactory"), routerType);
    source.addSuperinterface(mvcFactory);
    var supports = MethodSpec.methodBuilder("supports");
    supports.addModifiers(Modifier.PUBLIC);
    supports.addParameter(ParameterSpec.builder(classType, "type").build());
    supports.addStatement(CodeBlock.of("return type == $L.class", getTargetType().getSimpleName()));
    supports.returns(boolean.class);
    source.addMethod(supports.build());

    var create = MethodSpec.methodBuilder("create");
    create.addModifiers(Modifier.PUBLIC);
    create.addParameter(ParameterSpec.builder(jakartaProvider, "provider").build());
    create.addStatement("return new $L(provider)", generateTypeName);
    create.returns(TypeName.get(elements.getTypeElement("io.jooby.Extension").asType()));
    source.addMethod(create.build());

    var javaFile = JavaFile.builder(getPackageName(), source.build());
    context.generateStaticImports(
        this,
        (classname, fn) ->
            javaFile.addStaticImport(
                ClassName.get(environment.getElementUtils().getTypeElement(classname)), fn));
    return javaFile.build();
  }

  private MethodSpec.Builder defaultConstructor() {
    var injectAnnotations = Set.of("javax.inject.Inject", "jakarta.inject.Inject");
    var defaultConstructor = MethodSpec.constructorBuilder();
    defaultConstructor.addModifiers(Modifier.PUBLIC);
    var constructors =
        getTargetType().getEnclosedElements().stream()
            .filter(it -> it.getKind() == ElementKind.CONSTRUCTOR)
            .toList();
    var hasDefaultConstructor =
        constructors.stream()
            .map(ExecutableElement.class::cast)
            .anyMatch(
                it -> it.getParameters().isEmpty() && it.getModifiers().contains(Modifier.PUBLIC));
    var inject =
        constructors.stream()
            .anyMatch(
                it ->
                    injectAnnotations.stream()
                        .anyMatch(annotation -> findAnnotationByName(it, annotation) != null));
    if (inject || !hasDefaultConstructor) {
      defaultConstructor.addStatement("this($L.class)", getTargetType().getSimpleName());
    } else {
      defaultConstructor.addStatement("this(new $L())", getTargetType().getSimpleName());
    }
    return defaultConstructor;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    var annotations = Optional.ofNullable(clazz.getAnnotationMirrors()).orElse(emptyList());
    annotations.forEach(
        annotation -> {
          buffer
              .append("@")
              .append(annotation.getAnnotationType().asElement().getSimpleName())
              .append("(");
          buffer.append(annotation.getElementValues()).append(") ");
        });
    buffer.append(clazz.asType().toString()).append(" {\n");
    routes.forEach(
        (httpMethod, route) -> {
          buffer.append("  ").append(route).append("\n");
        });
    buffer.append("}");
    return buffer.toString();
  }
}
