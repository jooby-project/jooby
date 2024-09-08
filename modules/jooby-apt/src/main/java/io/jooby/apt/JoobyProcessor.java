/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static io.jooby.apt.JoobyProcessor.Options.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

import io.jooby.internal.apt.*;

@SupportedOptions({
  HANDLER,
  DEBUG,
  INCREMENTAL,
  SERVICES,
  MVC_METHOD,
  RETURN_TYPE,
  SKIP_ATTRIBUTE_ANNOTATIONS
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class JoobyProcessor extends AbstractProcessor {
  public interface Options {
    String HANDLER = "jooby.handler";
    String DEBUG = "jooby.debug";
    String ROUTER_PREFIX = "jooby.routerPrefix";
    String ROUTER_SUFFIX = "jooby.routerSuffix";
    String INCREMENTAL = "jooby.incremental";
    String RETURN_TYPE = "jooby.returnType";
    String MVC_METHOD = "jooby.mvcMethod";
    String SERVICES = "jooby.services";
    String SKIP_ATTRIBUTE_ANNOTATIONS = "jooby.skipAttributeAnnotations";

    static boolean boolOpt(ProcessingEnvironment environment, String option, boolean defaultValue) {
      return Boolean.parseBoolean(
          environment.getOptions().getOrDefault(option, String.valueOf(defaultValue)));
    }

    static List<String> stringListOpt(ProcessingEnvironment environment, String option) {
      String value = string(environment, option, null);
      return value == null || value.isEmpty()
          ? List.of()
          : Stream.of(value.split(",")).filter(it -> !it.isBlank()).map(String::trim).toList();
    }

    static String string(ProcessingEnvironment environment, String option, String defaultValue) {
      String value = environment.getOptions().getOrDefault(option, defaultValue);
      return value == null || value.isEmpty() ? defaultValue : value;
    }
  }

  protected MvcContext context;
  private BiConsumer<Diagnostic.Kind, String> output;
  private final Set<Object> processed = new HashSet<>();

  public JoobyProcessor(BiConsumer<Diagnostic.Kind, String> output) {
    this.output = output;
  }

  public JoobyProcessor() {}

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    this.context =
        new MvcContext(
            processingEnvironment,
            ofNullable(output)
                .orElseGet(
                    () ->
                        (kind, message) ->
                            processingEnvironment.getMessager().printMessage(kind, message)));
    super.init(processingEnvironment);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      if (roundEnv.processingOver()) {
        context.debug("Output:");
        context.getRouters().forEach(it -> context.debug("  %s.java", it.getGeneratedType()));
        if (context.generateServices()) {
          doServices(context.getProcessingEnvironment().getFiler(), context.getRouters());
        }
        return false;
      } else {
        var routeMap = buildRouteRegistry(annotations, roundEnv);
        verifyBeanValidationDependency(routeMap.values());
        for (var router : routeMap.values()) {
          try {
            var sourceCode = router.toSourceCode(null);
            var sourceLocation = router.getGeneratedFilename();
            onGeneratedSource(toJavaFileObject(sourceLocation, sourceCode));
            context.debug("router %s: %s", router.getTargetType(), router.getGeneratedType());
            router.getRoutes().forEach(it -> context.debug("   %s", it));
            writeSource(router, sourceLocation, sourceCode);
            context.add(router);
          } catch (IOException cause) {
            throw new RuntimeException("Unable to generate: " + router.getTargetType(), cause);
          }
        }
        return true;
      }
    } catch (Exception cause) {
      context.error(
          Optional.ofNullable(cause.getMessage()).orElse("Unable to generate routes"), cause);
      throw sneakyThrow0(cause);
    }
  }

  private void writeSource(MvcRouter router, String sourceLocation, String sourceCode)
      throws IOException {
    var environment = context.getProcessingEnvironment();
    var filer = environment.getFiler();
    if (router.isKt()) {
      var kapt = environment.getOptions().get("kapt.kotlin.generated");
      if (kapt != null) {
        var output = Paths.get(kapt, sourceLocation);
        Files.createDirectories(output.getParent());
        Files.writeString(output, sourceCode);
      } else {
        var ktFile =
            filer.createResource(SOURCE_OUTPUT, "", sourceLocation, router.getTargetType());
        try (var writer = ktFile.openWriter()) {
          writer.write(sourceCode);
        }
      }
    } else {
      var javaFIle = filer.createSourceFile(router.getGeneratedType(), router.getTargetType());
      try (var writer = javaFIle.openWriter()) {
        writer.write(sourceCode);
      }
    }
  }

  private static JavaFileObject toJavaFileObject(String filename, String source) {
    var uri = URI.create(filename);
    return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
      private final long lastModified = System.currentTimeMillis();

      @Override
      public String getCharContent(boolean ignoreEncodingErrors) {
        return source;
      }

      @Override
      public InputStream openInputStream() {
        return new ByteArrayInputStream(getCharContent(true).getBytes(UTF_8));
      }

      @Override
      public long getLastModified() {
        return lastModified;
      }

      @Override
      public String toString() {
        return getCharContent(false);
      }
    };
  }

  protected void onGeneratedSource(JavaFileObject source) {}

  private void doServices(Filer filer, List<MvcRouter> routers) {
    try {
      var location = "META-INF/services/io.jooby.MvcFactory";
      context.debug(location);

      var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", location);
      var content = new StringBuilder();
      for (var router : routers) {
        var classname = router.getGeneratedType();
        context.debug("  %s", classname);
        content.append(classname).append(System.lineSeparator());
      }
      try (var writer = new PrintWriter(resource.openOutputStream())) {
        writer.println(content);
      }
    } catch (IOException cause) {
      throw propagate(cause);
    }
  }

  private Map<TypeElement, MvcRouter> buildRouteRegistry(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Map<TypeElement, MvcRouter> registry = new LinkedHashMap<>();

    for (var annotation : annotations) {
      context.debug("found annotation: %s", annotation);
      var elements = roundEnv.getElementsAnnotatedWith(annotation);
      // Element could be Class or Method, bc @Path can be applied to both of them
      // Also we need to expand lookup to external jars see #2486
      for (var element : elements) {
        context.debug("  %s", element);
        if (element instanceof TypeElement typeElement) {
          buildRouteRegistry(registry, typeElement);
        } else if (element instanceof ExecutableElement method) {
          buildRouteRegistry(registry, (TypeElement) method.getEnclosingElement());
        }
      }
    }
    // Remove all abstract router
    var abstractTypes =
        registry.entrySet().stream()
            .filter(it -> it.getValue().isAbstract())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    abstractTypes.forEach(registry::remove);

    // Generate unique method name by router
    for (var router : registry.values()) {
      // Initialize with supports/create method from MvcFactory (avoid name collision)
      var names = new HashSet<>();
      for (var route : router.getRoutes()) {
        if (!names.add(route.getMethodName())) {
          var paramsString =
              route.getRawParameterTypes(true).stream()
                  .map(it -> it.substring(Math.max(0, it.lastIndexOf(".") + 1)))
                  .map(it -> Character.toUpperCase(it.charAt(0)) + it.substring(1))
                  .collect(Collectors.joining());
          route.setGeneratedName(route.getMethodName() + paramsString);
        } else {
          route.setGeneratedName(route.getMethodName());
        }
      }
    }
    return registry;
  }

  /**
   * Scan routes from basType and any super class of it. It saves all route method found in current
   * type or super (inherited). Routes method from super types are also saved.
   *
   * <p>Abstract route method are ignored.
   *
   * @param registry Route registry.
   * @param currentType Base type.
   */
  private void buildRouteRegistry(Map<TypeElement, MvcRouter> registry, TypeElement currentType) {
    for (TypeElement superType : context.superTypes(currentType)) {
      if (processed.add(superType)) {
        // collect all declared methods
        superType.getEnclosedElements().stream()
            .filter(ExecutableElement.class::isInstance)
            .map(ExecutableElement.class::cast)
            .forEach(
                method -> {
                  if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                    context.debug("ignoring abstract method: %s %s", superType, method);
                  } else {
                    method.getAnnotationMirrors().stream()
                        .map(AnnotationMirror::getAnnotationType)
                        .map(DeclaredType::asElement)
                        .filter(TypeElement.class::isInstance)
                        .map(TypeElement.class::cast)
                        .filter(HttpMethod::hasAnnotation)
                        .forEach(
                            annotation -> {
                              Stream.of(currentType, superType)
                                  .distinct()
                                  .forEach(
                                      routerClass ->
                                          registry
                                              .computeIfAbsent(
                                                  routerClass, type -> new MvcRouter(context, type))
                                              .put(annotation, method));
                            });
                  }
                });
      } else {
        if (!currentType.equals(superType)) {
          // edge case when controller has no method and extends another class which has.
          registry.computeIfAbsent(currentType, key -> new MvcRouter(key, registry.get(superType)));
        }
      }
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    var supportedTypes = new HashSet<String>();
    supportedTypes.addAll(HttpPath.PATH.getAnnotations());
    supportedTypes.addAll(HttpMethod.annotations());
    return supportedTypes;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public Set<String> getSupportedOptions() {
    var options = new HashSet<>(super.getSupportedOptions());

    if (context.isIncremental()) {
      // Enables incremental annotation processing support in Gradle.
      // If service provider configuration is being generated,
      // only 'aggregating' mode is supported since it's likely that
      // more then one originating element is passed to the Filer
      // API on writing the resource file - isolating mode does not
      // allow this.
      options.add(
          String.format(
              "org.gradle.annotation.processing.%s",
              context.generateServices() ? "aggregating" : "isolating"));
    }

    return options;
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it
   * onwards. The exception is still thrown - javac will just stop whining about it.
   *
   * <p>Example usage:
   *
   * <pre>public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }</pre>
   *
   * <p>NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does
   * not know or care about the concept of a 'checked exception'. All this method does is hide the
   * act of throwing a checked exception from the java compiler.
   *
   * <p>Note that this method has a return type of {@code RuntimeException}; it is advised you
   * always call this method as argument to the {@code throw} statement to avoid compiler errors
   * regarding no return statement and similar problems. This method won't of course return an
   * actual {@code RuntimeException} - it never returns, it always throws the provided exception.
   *
   * @param x The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws
   *     an exception!
   */
  public static RuntimeException propagate(final Throwable x) {
    if (x == null) {
      throw new NullPointerException("x");
    }

    return sneakyThrow0(x);
  }

  /**
   * Make a checked exception un-checked and rethrow it.
   *
   * @param x Exception to throw.
   * @param <E> Exception type.
   * @throws E Exception to throw.
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> E sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }

  private void verifyBeanValidationDependency(Collection<MvcRouter> routers) {
    var hasBeanValidation = routers.stream().anyMatch(MvcRouter::hasBeanValidation);
    if (hasBeanValidation) {
      var missingDependency =
          Stream.of(
                  "io.jooby.hibernate.validator.HibernateValidatorModule",
                  "io.jooby.avaje.validator.AvajeValidatorModule",
                  "io.jooby.apt.validator.FakeValidatorModule")
              .map(name -> processingEnv.getElementUtils().getTypeElement(name))
              .filter(Objects::nonNull)
              .findFirst()
              .isEmpty();
      if (missingDependency) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "Unable to load 'BeanValidator' class. Bean validation usage (@Valid) was detected,"
                    + " but the appropriate dependency is missing. Please ensure that you have"
                    + " added the corresponding validation dependency (e.g.,"
                    + " jooby-hibernate-validator, jooby-avaje-validator).");
      }
    }
  }
}
