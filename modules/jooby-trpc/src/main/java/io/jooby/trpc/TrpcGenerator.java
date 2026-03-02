/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Output;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import io.github.classgraph.ClassGraph;

/**
 * A generator that orchestrates {@code typescript-generator} to produce a tRPC-compatible
 * TypeScript API definition from compiled Jooby controllers.
 *
 * <p>This tool bypasses the standard REST scanner of {@code typescript-generator}. Instead, it:
 *
 * <ol>
 *   <li>Scans the compiled class directory (or explicitly added classes) for controllers marked
 *       with {@code @Trpc}.
 *   <li>Extracts only the input and return types (DTOs) of the matching methods.
 *   <li>Feeds those data models to the generator to produce clean TypeScript interfaces.
 *   <li>Uses a fast, recursive type resolver to accurately map Java methods to tRPC {@code { input,
 *       output }} shapes.
 *   <li>Appends a strict {@code AppRouter} definition to the generated file.
 * </ol>
 */
public class TrpcGenerator {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private Path buildClassesDir;
  private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
  private Path outputDir;
  private String outputFile = "trpc.d.ts";
  private boolean expandLookup = false;

  private final Set<Class<?>> manualControllers = new LinkedHashSet<>();

  private JsonLibrary jsonLibrary = JsonLibrary.jackson2;
  private Map<String, String> customTypeMappings = new LinkedHashMap<>();
  private Map<String, String> customTypeNaming = new LinkedHashMap<>();
  private List<String> importDeclarations = new ArrayList<>();
  private DateMapping mapDate = DateMapping.asString;
  private EnumMapping mapEnum = EnumMapping.asInlineUnion;

  /**
   * Executes the full TypeScript and tRPC generation pipeline.
   *
   * @throws IOException If an I/O error occurs reading classes or writing the output file.
   * @throws IllegalStateException If {@code outputDir} is not configured or if no controllers are
   *     found.
   */
  public void generate() throws IOException {
    if (outputDir == null) {
      throw new IllegalStateException("outputDir is required to generate the TypeScript file.");
    }

    var finalOutput = outputDir.resolve(outputFile);
    if (!Files.exists(outputDir)) {
      Files.createDirectories(outputDir);
    }

    var controllers = discoverControllers();
    controllers.addAll(manualControllers);

    if (controllers.isEmpty()) {
      throw new IllegalStateException(
          "No controllers were found to generate. "
              + "Ensure 'buildClassesDir' points to the compiled classes directory, "
              + "or use 'addController(Class)' to manually register controllers for unit testing.");
    }

    // 1. Extract ONLY the Data Models (Inputs/Outputs)
    var typesToGenerate = new LinkedHashSet<Type>();
    for (var controller : controllers) {
      for (var method : controller.getDeclaredMethods()) {
        boolean includeMethod = isTrpcAnnotated(method);
        if (!includeMethod && expandLookup) includeMethod = hasWebAnnotation(method);

        if (includeMethod) {
          typesToGenerate.add(method.getGenericReturnType());
          for (var param : method.getGenericParameterTypes()) {
            typesToGenerate.add(param);
          }
        }
      }
    }

    var settings = new Settings();
    settings.outputFileType = TypeScriptFileType.declarationFile;
    settings.outputKind = TypeScriptOutputKind.module;
    settings.classLoader = classLoader;
    settings.jsonLibrary = this.jsonLibrary;
    settings.mapDate = this.mapDate;
    settings.mapEnum = this.mapEnum;
    if (customTypeMappings != null) settings.customTypeMappings.putAll(customTypeMappings);
    if (customTypeNaming != null) settings.customTypeNaming.putAll(customTypeNaming);
    if (importDeclarations != null) settings.importDeclarations.addAll(importDeclarations);

    // 2. Generate standard interfaces (DTOs only)
    if (!typesToGenerate.isEmpty()) {
      TypeScriptGenerator.setLogger(asSlf4j(log));
      var generator = new TypeScriptGenerator(settings);
      var input = Input.from(typesToGenerate.toArray(new Type[0]));
      generator.generateTypeScript(input, Output.to(finalOutput.toFile()));
    }

    // Safety net: If typescript-generator skipped generation (e.g., only primitive types), create
    // the base file.
    if (!Files.exists(finalOutput)) {
      Files.writeString(finalOutput, "/* tslint:disable */\n/* eslint-disable */\n\n");
    }

    // 3. Append the exact tRPC AppRouter
    appendTrpcRouter(finalOutput, controllers);
  }

  private static cz.habarta.typescript.generator.Logger asSlf4j(Logger log) {
    return new cz.habarta.typescript.generator.Logger() {
      @Override
      protected void write(Level level, String message) {
        switch (level) {
          case Info -> log.info(message);
          case Warning -> log.warn(message.replace("Warning: ", ""));
          case Error -> log.error(message.replace("Error: ", ""));
          case Debug -> log.debug(message.replace("Debug: ", ""));
          case Verbose -> log.trace(message);
        }
      }
    };
  }

  /**
   * Constructs and appends the tRPC {@code AppRouter} mapping to the bottom of the generated file.
   *
   * @param finalOutput The path to the generated output file.
   * @param controllers The set of validated controller classes.
   * @throws IOException If file writing fails.
   */
  private void appendTrpcRouter(Path finalOutput, Set<Class<?>> controllers) throws IOException {
    var ts = new StringBuilder();

    ts.append("\n// --- tRPC Router Mapping ---\n\n");
    ts.append("export type AppRouter = {\n");

    for (var controller : controllers) {
      var namespace = extractNamespace(controller);
      String indent = "  "; // Default indent for root methods

      if (namespace != null) {
        ts.append("  ").append(namespace).append(": {\n");
        indent = "    "; // Increase indent for nested methods
      }

      for (var method : controller.getDeclaredMethods()) {
        boolean includeMethod = isTrpcAnnotated(method);
        if (!includeMethod && expandLookup) includeMethod = hasWebAnnotation(method);

        if (includeMethod) {
          var params = method.getGenericParameterTypes();
          String tsInput = "void";
          if (params.length == 1) {
            tsInput = resolveTsType(params[0]);
          } else if (params.length > 1) {
            var tuple = new ArrayList<String>();
            for (var p : params) tuple.add(resolveTsType(p));
            tsInput = "[" + String.join(", ", tuple) + "]";
          }

          String tsOutput = resolveTsType(method.getGenericReturnType());

          ts.append(indent)
              .append(method.getName())
              .append(": { input: ")
              .append(tsInput)
              .append("; output: ")
              .append(tsOutput)
              .append(" };\n");
        }
      }

      if (namespace != null) {
        ts.append("  };\n");
      }
    }

    ts.append("};\n");
    Files.writeString(finalOutput, ts.toString(), StandardOpenOption.APPEND);
  }

  /**
   * Fast, recursive type resolver to map Java types directly to TypeScript signatures. Understands
   * Jooby async types, standard collections, and primitive mappings.
   *
   * @param type The Java type to evaluate.
   * @return A valid TypeScript string representation of the type.
   */
  private String resolveTsType(Type type) {
    if (type == void.class || type == Void.class) return "void";

    if (type instanceof ParameterizedType pt) {
      var raw = pt.getRawType();
      var rawName = raw.getTypeName();

      // Unwrap async types (CompletableFuture, Mono, Single, Future)
      if (rawName.endsWith("CompletableFuture")
          || rawName.endsWith("Single")
          || rawName.endsWith("Mono")
          || rawName.endsWith("Future")) {
        return resolveTsType(pt.getActualTypeArguments()[0]);
      }

      if (raw instanceof Class<?> clazz) {
        if (java.util.Collection.class.isAssignableFrom(clazz)) {
          return resolveTsType(pt.getActualTypeArguments()[0]) + "[]";
        }
        if (java.util.Map.class.isAssignableFrom(clazz)) {
          return "{ [index: string]: " + resolveTsType(pt.getActualTypeArguments()[1]) + " }";
        }
        if (java.util.Optional.class.isAssignableFrom(clazz)) {
          return resolveTsType(pt.getActualTypeArguments()[0]) + " | null";
        }

        // Handle generic DTOs
        var args = pt.getActualTypeArguments();
        var tsArgs = new ArrayList<String>();
        for (var arg : args) tsArgs.add(resolveTsType(arg));
        return getClassName(clazz) + "<" + String.join(", ", tsArgs) + ">";
      }
    }

    if (type instanceof Class<?> clazz) {
      if (clazz.isArray()) {
        if (clazz.getComponentType() == byte.class)
          return "string"; // Common byte[] to base64 string
        return resolveTsType(clazz.getComponentType()) + "[]";
      }

      if (clazz == String.class
          || clazz == char.class
          || clazz == Character.class
          || clazz.getName().equals("java.util.UUID")) return "string";
      if (clazz == boolean.class || clazz == Boolean.class) return "boolean";
      if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) return "number";

      if (java.util.Date.class.isAssignableFrom(clazz)
          || clazz.getName().startsWith("java.time.")) {
        return mapDate == DateMapping.asString
            ? "string"
            : (mapDate == DateMapping.asNumber ? "number" : "Date");
      }

      return getClassName(clazz);
    }

    return "any";
  }

  /**
   * Evaluates the custom mappings to determine the appropriate TypeScript interface name.
   *
   * @param clazz The Java class being resolved.
   * @return The target TypeScript interface name.
   */
  private String getClassName(Class<?> clazz) {
    var fqn = clazz.getName();
    if (customTypeMappings != null && customTypeMappings.containsKey(fqn)) {
      return customTypeMappings.get(fqn);
    }
    if (customTypeNaming != null && customTypeNaming.containsKey(fqn)) {
      return customTypeNaming.get(fqn);
    }
    return clazz.getSimpleName();
  }

  /**
   * Scans the build output directory to load and validate controller classes.
   *
   * @return A set of valid controller classes found on disk.
   * @throws IOException If the directory scan fails.
   */
  private Set<Class<?>> discoverControllers() {
    var controllers = new LinkedHashSet<Class<?>>();

    // We scope the scan strictly to the build directory for maximum speed
    var classGraph =
        new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .enableMethodInfo()
            .ignoreClassVisibility();

    if (buildClassesDir != null && Files.exists(buildClassesDir)) {
      classGraph.overrideClasspath(buildClassesDir.toUri().toString());
    } else if (classLoader != null) {
      classGraph.overrideClassLoaders(classLoader);
    } else {
      return controllers;
    }

    try (var scanResult = classGraph.scan()) {
      for (var classInfo : scanResult.getAllClasses()) {
        try {
          var clazz = classInfo.loadClass(false); // loads without initializing!

          boolean includeClass = isTrpcAnnotated(clazz);
          if (!includeClass && expandLookup) includeClass = hasWebAnnotation(clazz);

          if (includeClass) {
            controllers.add(clazz);
          } else {
            // Check methods if the class itself isn't annotated
            for (var method : clazz.getDeclaredMethods()) {
              boolean includeMethod = isTrpcAnnotated(method);
              if (!includeMethod && expandLookup) includeMethod = hasWebAnnotation(method);

              if (includeMethod) {
                controllers.add(clazz);
                break;
              }
            }
          }
        } catch (Throwable ignored) {
          // Safely ignore classes that throw LinkageError or NoClassDefFoundError
        }
      }
    }

    return controllers;
  }

  /**
   * ClassLoader-agnostic check to see if an element has the Trpc annotation.
   *
   * @param element The class or method to inspect.
   * @return True if annotated with {@code io.jooby.annotation.Trpc}.
   */
  private boolean isTrpcAnnotated(AnnotatedElement element) {
    for (Annotation a : element.getAnnotations()) {
      if (a.annotationType().getName().equals("io.jooby.annotation.Trpc")) {
        return true;
      }
    }
    return false;
  }

  /**
   * ClassLoader-agnostic check for standard web routing annotations.
   *
   * @param element The class or method to inspect.
   * @return True if a JAX-RS or Jooby web annotation is present.
   */
  private boolean hasWebAnnotation(AnnotatedElement element) {
    for (Annotation a : element.getAnnotations()) {
      var name = a.annotationType().getName();
      if (name.startsWith("io.jooby.annotation.")
          || name.startsWith("jakarta.ws.rs.")
          || name.startsWith("javax.ws.rs.")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts the target namespace for the tRPC router based on the controller. If the class is not
   * annotated with @Trpc, it returns null (indicating root-level).
   *
   * @param controller The controller class.
   * @return The determined namespace string, or null for root-level.
   */
  private String extractNamespace(Class<?> controller) {
    boolean hasClassLevelTrpc = false;

    for (Annotation a : controller.getAnnotations()) {
      if (a.annotationType().getName().equals("io.jooby.annotation.Trpc")) {
        hasClassLevelTrpc = true;
        try {
          var method = a.annotationType().getMethod("value");
          var value = (String) method.invoke(a);
          // Explicit namespace provided: @Trpc("myNamespace")
          if (value != null && !value.isBlank()) return value;
        } catch (Exception ignored) {
        }
      }
    }

    // No class-level annotation means these methods sit at the root of the router
    if (!hasClassLevelTrpc) {
      return null;
    }

    // Class is annotated, but no explicit value provided. Derive from class name.
    var name = controller.getSimpleName();
    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    return name.replace("Controller", "").replace("Resource", "");
  }

  // --- Configuration API (Getters, Setters, and Builders) ---

  /**
   * Explicitly adds a controller class to the generation pipeline. Highly recommended for unit
   * testing to avoid classpath scanning issues.
   *
   * @param controller The controller class to analyze.
   */
  public void addController(Class<?> controller) {
    this.manualControllers.add(controller);
  }

  /**
   * @return The directory where compiled class files are located.
   */
  public Path getBuildClassesDir() {
    return buildClassesDir;
  }

  /**
   * @param buildClassesDir The directory where compiled class files are located.
   */
  public void setBuildClassesDir(Path buildClassesDir) {
    this.buildClassesDir = buildClassesDir;
  }

  /**
   * @return The class loader used to load compiled controllers.
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * @param classLoader The class loader used to load compiled controllers. Defaults to context
   *     class loader.
   */
  public void setClassLoader(ClassLoader classLoader) {
    if (classLoader != null) this.classLoader = classLoader;
  }

  /**
   * @return The destination directory for the generated TypeScript file.
   */
  public Path getOutputDir() {
    return outputDir;
  }

  /**
   * @param outputDir The destination directory for the generated TypeScript file.
   */
  public void setOutputDir(Path outputDir) {
    this.outputDir = outputDir;
  }

  /**
   * @return The name of the generated TypeScript file.
   */
  public String getOutputFile() {
    return outputFile;
  }

  /**
   * @param outputFile The name of the generated TypeScript file. Defaults to {@code trpc.d.ts}.
   */
  public void setOutputFile(String outputFile) {
    if (outputFile != null && !outputFile.isBlank()) this.outputFile = outputFile;
  }

  /**
   * @return True if standard Jooby and JAX-RS annotations are included in the generation.
   */
  public boolean isExpandLookup() {
    return expandLookup;
  }

  /**
   * @param expandLookup Set to true to generate endpoints for standard web annotations even
   *     without @Trpc.
   */
  public void setExpandLookup(boolean expandLookup) {
    this.expandLookup = expandLookup;
  }

  /**
   * @return The target JSON library for data model generation.
   */
  public JsonLibrary getJsonLibrary() {
    return jsonLibrary;
  }

  /**
   * @param jsonLibrary The target JSON library used to parse field annotations. Defaults to Jackson
   *     2.
   */
  public void setJsonLibrary(JsonLibrary jsonLibrary) {
    if (jsonLibrary != null) this.jsonLibrary = jsonLibrary;
  }

  /**
   * @return Custom mapping overrides translating Java types to raw TypeScript strings.
   */
  public Map<String, String> getCustomTypeMappings() {
    return customTypeMappings;
  }

  /**
   * @param customTypeMappings Custom mapping overrides translating Java types to raw TypeScript
   *     strings.
   */
  public void setCustomTypeMappings(Map<String, String> customTypeMappings) {
    this.customTypeMappings = customTypeMappings;
  }

  /**
   * @return Custom overrides for generating specific TypeScript interface names.
   */
  public Map<String, String> getCustomTypeNaming() {
    return customTypeNaming;
  }

  /**
   * @param customTypeNaming Custom overrides for generating specific TypeScript interface names.
   */
  public void setCustomTypeNaming(Map<String, String> customTypeNaming) {
    this.customTypeNaming = customTypeNaming;
  }

  /**
   * @return Raw import statements appended to the top of the generated file.
   */
  public List<String> getImportDeclarations() {
    return importDeclarations;
  }

  /**
   * @param importDeclarations Raw import statements appended to the top of the generated file.
   */
  public void setImportDeclarations(List<String> importDeclarations) {
    this.importDeclarations = importDeclarations;
  }

  /**
   * @return The mapping strategy applied to Java date types.
   */
  public DateMapping getMapDate() {
    return mapDate;
  }

  /**
   * @param mapDate The mapping strategy applied to Java date types.
   */
  public void setMapDate(DateMapping mapDate) {
    this.mapDate = mapDate;
  }

  /**
   * @return The mapping strategy applied to Java enum types.
   */
  public EnumMapping getMapEnum() {
    return mapEnum;
  }

  /**
   * @param mapEnum The mapping strategy applied to Java enum types.
   */
  public void setMapEnum(EnumMapping mapEnum) {
    this.mapEnum = mapEnum;
  }
}
