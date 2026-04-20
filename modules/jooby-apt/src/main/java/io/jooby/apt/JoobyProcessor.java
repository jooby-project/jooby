/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static io.jooby.apt.JoobyProcessor.Options.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static javax.tools.StandardLocation.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.*;

import io.jooby.internal.apt.*;

import io.jooby.internal.apt.ws.WsRouter;

/** Process jooby/jakarta annotation and generate source code from MVC controllers. */
@SupportedOptions({
  DEBUG,
  INCREMENTAL,
  MVC_METHOD,
  ROUTER_PREFIX,
  ROUTER_SUFFIX,
  SKIP_ATTRIBUTE_ANNOTATIONS
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JoobyProcessor extends AbstractProcessor {
  /** Available options. */
  public interface Options {
    /** Run code generator in debug mode. */
    String DEBUG = "jooby.debug";

    /** Add custom prefix to generated class. Default: none/empty */
    String ROUTER_PREFIX = "jooby.routerPrefix";

    /** Add custom suffix to generated class. Default: _ */
    String ROUTER_SUFFIX = "jooby.routerSuffix";

    /** Gradle options to run in incremental mode. */
    String INCREMENTAL = "jooby.incremental";

    /** Turn on/off generation of method metadata. */
    String MVC_METHOD = "jooby.mvcMethod";

    /** Control which annotations are translated to route attributes. */
    String SKIP_ATTRIBUTE_ANNOTATIONS = "jooby.skipAttributeAnnotations";

    /**
     * Read a boolean option.
     *
     * @param environment Annotation processing environment.
     * @param option Option's name.
     * @param defaultValue Default value.
     * @return Option's value.
     */
    static boolean boolOpt(ProcessingEnvironment environment, String option, boolean defaultValue) {
      return Boolean.parseBoolean(
          environment.getOptions().getOrDefault(option, String.valueOf(defaultValue)));
    }

    /**
     * Read a string list option.
     *
     * @param environment Annotation processing environment.
     * @param option Option's name.
     * @return Option's value.
     */
    static List<String> stringListOpt(ProcessingEnvironment environment, String option) {
      String value = string(environment, option, null);
      return value == null || value.isEmpty()
          ? List.of()
          : Stream.of(value.split(",")).filter(it -> !it.isBlank()).map(String::trim).toList();
    }

    /**
     * Read a string option.
     *
     * @param environment Annotation processing environment.
     * @param option Option's name.
     * @param defaultValue Default value.
     * @return Option's value.
     */
    static String string(ProcessingEnvironment environment, String option, String defaultValue) {
      String value = environment.getOptions().getOrDefault(option, defaultValue);
      return value == null || value.isEmpty() ? defaultValue : value;
    }
  }

  protected MvcContext context;
  private BiConsumer<Diagnostic.Kind, String> output;

  JoobyProcessor(BiConsumer<Diagnostic.Kind, String> output) {
    this.output = output;
  }

  /** Default constructor. */
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
        context.getRouters().forEach(it -> context.debug("  %s", it.getGeneratedType()));
        return false;
      } else {
        // Discover all unique Controller classes
        var controllers = findControllers(annotations, roundEnv);

        // Factory Pattern: Build specific routers for each class based on method annotations
        List<WebRouter<?>> activeRouters = new ArrayList<>();
        for (var controller : controllers) {
          if (controller.getModifiers().contains(Modifier.ABSTRACT)) continue;

          var restRouter = RestRouter.parse(context, controller);
          if (!restRouter.isEmpty()) {
            activeRouters.add(restRouter);
          }

          var jsonRpcRouter = JsonRpcRouter.parse(context, controller);
          if (!jsonRpcRouter.isEmpty()) {
            activeRouters.add(jsonRpcRouter);
          }

          var mcpRouter = McpRouter.parse(context, controller);
          if (!mcpRouter.isEmpty()) {
            activeRouters.add(mcpRouter);
          }

          var trpcRouter = TrpcRouter.parse(context, controller);
          if (!trpcRouter.isEmpty()) {
            activeRouters.add(trpcRouter);
          }

          var wsRouter = WsRouter.parse(context, controller);
          if (!wsRouter.isEmpty()) {
            activeRouters.add(wsRouter);
          }
        }

        verifyBeanValidationDependency(activeRouters);

        // Generate Code Iteratively!
        for (var router : activeRouters) {
          try {
            context.add(router);

            var sourceCode = router.toSourceCode(router.isKt());
            var sourceLocation = router.getGeneratedFilename();
            var generatedType = router.getGeneratedType();

            onGeneratedSource(generatedType, toJavaFileObject(sourceLocation, sourceCode));
            context.debug("router %s: %s", router.getTargetType(), generatedType);

            writeSource(
                router.isKt(), generatedType, sourceLocation, sourceCode, router.getTargetType());
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

  private Set<TypeElement> findControllers(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<TypeElement> controllers = new LinkedHashSet<>();
    for (var annotation : annotations) {
      for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
        if (element instanceof TypeElement typeElement
            && !typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
          controllers.add(typeElement);
        } else if (element instanceof ExecutableElement method) {
          controllers.add((TypeElement) method.getEnclosingElement());
        }
      }
    }
    return controllers;
  }

  private void writeSource(
      boolean isKt,
      String className,
      String sourceLocation,
      String sourceCode,
      Element... originatingElements)
      throws IOException {
    var environment = context.getProcessingEnvironment();
    var filer = environment.getFiler();
    if (isKt) {
      var kapt = environment.getOptions().get("kapt.kotlin.generated");
      if (kapt != null) {
        var output = Paths.get(kapt, sourceLocation);
        Files.createDirectories(output.getParent());
        Files.writeString(output, sourceCode);
      } else {
        var ktFile = filer.createResource(SOURCE_OUTPUT, "", sourceLocation, originatingElements);
        try (var writer = ktFile.openWriter()) {
          writer.write(sourceCode);
        }
      }
    } else {
      var javaFIle = filer.createSourceFile(className, originatingElements);
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

  protected void onGeneratedSource(String className, JavaFileObject source) {}

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    var supportedTypes = new HashSet<String>();
    supportedTypes.addAll(HttpPath.PATH.getAnnotations());
    supportedTypes.addAll(HttpMethod.annotations());
    // Add Rcp annotations
    supportedTypes.add("io.jooby.annotation.trpc.Trpc");
    supportedTypes.add("io.jooby.annotation.trpc.Trpc.Mutation");
    supportedTypes.add("io.jooby.annotation.trpc.Trpc.Query");
    supportedTypes.add("io.jooby.annotation.jsonrpc.JsonRpc");
    // Add MCP Annotations
    supportedTypes.add("io.jooby.annotation.mcp.McpCompletion");
    supportedTypes.add("io.jooby.annotation.mcp.McpTool");
    supportedTypes.add("io.jooby.annotation.mcp.McpPrompt");
    supportedTypes.add("io.jooby.annotation.mcp.McpResource");
    supportedTypes.add("io.jooby.annotation.mcp.McpServer");
    // Add WS Annotations
    supportedTypes.add("io.jooby.annotation.ws.WebSocketRoute");
    supportedTypes.add("io.jooby.annotation.ws.OnConnect");
    supportedTypes.add("io.jooby.annotation.ws.OnClose");
    supportedTypes.add("io.jooby.annotation.ws.OnMessage");
    supportedTypes.add("io.jooby.annotation.ws.OnError");
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
      options.add("org.gradle.annotation.processing.isolating");
    }

    return options;
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

  private void verifyBeanValidationDependency(Collection<WebRouter<?>> routers) {
    var hasBeanValidation = routers.stream().anyMatch(WebRouter::hasBeanValidation);
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
