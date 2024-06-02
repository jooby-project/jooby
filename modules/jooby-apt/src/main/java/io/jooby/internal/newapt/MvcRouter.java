/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.newapt;

import static java.util.Collections.emptyList;

import java.util.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.squareup.javapoet.*;
import io.jooby.apt.MvcContext;
import io.jooby.internal.apt.TypeDefinition;

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

  public TypeElement getTargetType() {
    return clazz;
  }

  public String getGeneratedType() {
    return getTargetType().getQualifiedName().toString() + "_";
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

  public JavaFile toSourceCode() {
    var environment = context.getProcessingEnvironment();
    var elements = environment.getElementUtils();

    var joobyType = TypeName.get(elements.getTypeElement("io.jooby.Jooby").asType());
    var contextType = TypeName.get(elements.getTypeElement("io.jooby.Context").asType());
    var routerType = TypeName.get(getTargetType().asType());
    var providerType =
        ParameterizedTypeName.get(
            ClassName.get(elements.getTypeElement("java.util.function.Function")),
            contextType,
            routerType);
    var supplierType =
        ParameterizedTypeName.get(
            ClassName.get(elements.getTypeElement("jakarta.inject.Provider")), routerType);
    var classType =
        ParameterizedTypeName.get(
            ClassName.get(elements.getTypeElement("java.lang.Class")), routerType);

    var generateTypeName = getTargetType().getSimpleName() + "_";
    var source = TypeSpec.classBuilder(generateTypeName);
    source.addModifiers(Modifier.PUBLIC);
    source.addSuperinterface(
        environment.getElementUtils().getTypeElement("io.jooby.MvcExtension").asType());

    var instanceConstructor = MethodSpec.constructorBuilder();
    instanceConstructor.addModifiers(Modifier.PUBLIC);
    instanceConstructor.addParameter(ParameterSpec.builder(routerType, "instance").build());
    instanceConstructor.addStatement("this.$N = ctx -> $N", "provider", "instance");
    source.addMethod(instanceConstructor.build());

    var supplierConstructor = MethodSpec.constructorBuilder();
    supplierConstructor.addModifiers(Modifier.PUBLIC);
    supplierConstructor.addParameter(ParameterSpec.builder(supplierType, "supplier").build());
    supplierConstructor.addStatement("this.$N = ctx -> supplier.get()", "provider");
    source.addMethod(supplierConstructor.build());

    var classConstructor = MethodSpec.constructorBuilder();
    classConstructor.addModifiers(Modifier.PUBLIC);
    classConstructor.addParameter(ParameterSpec.builder(classType, "type").build());
    classConstructor.addStatement(
        "this.$N = ctx -> ctx.require($L.class)", "provider", getTargetType().getSimpleName());
    source.addMethod(classConstructor.build());

    source.addMethod(defaultConstructor().build());

    var field = FieldSpec.builder(providerType, "provider", Modifier.FINAL, Modifier.PROTECTED);
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
    source.addSuperinterface(
        environment.getElementUtils().getTypeElement("io.jooby.MvcFactory").asType());
    var supports = MethodSpec.methodBuilder("supports");
    supports.addModifiers(Modifier.PUBLIC);
    supports.addParameter(ParameterSpec.builder(Class.class, "type").build());
    supports.addStatement(CodeBlock.of("return type == $L.class", getTargetType().getSimpleName()));
    supports.returns(boolean.class);
    source.addMethod(supports.build());

    var create = MethodSpec.methodBuilder("create");
    create.addModifiers(Modifier.PUBLIC);
    var rawProvider =
        new TypeDefinition(
            context.getProcessingEnvironment().getTypeUtils(),
            elements.getTypeElement("jakarta.inject.Provider").asType());
    create.addParameter(
        ParameterSpec.builder(TypeName.get(rawProvider.getRawType()), "provider").build());
    create.addStatement("return new $L(provider)", generateTypeName);
    create.returns(TypeName.get(elements.getTypeElement("io.jooby.Extension").asType()));
    source.addMethod(create.build());

    return JavaFile.builder(getPackageName(), source.build()).build();
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
                    it.getAnnotationMirrors().stream()
                        .anyMatch(
                            annotation ->
                                injectAnnotations.contains(
                                    annotation.getAnnotationType().toString())));
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
