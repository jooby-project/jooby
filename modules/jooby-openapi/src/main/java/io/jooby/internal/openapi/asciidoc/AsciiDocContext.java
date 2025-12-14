/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.OpenAPIExt;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.lexer.Syntax;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.DelegatingLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.media.Schema;

public class AsciiDocContext {
  public static final BiConsumer<String, Schema<?>> NOOP = (name, schema) -> {};
  public static final Schema EMPTY_SCHEMA = new Schema<>();

  private ObjectMapper json;

  private ObjectMapper yamlOpenApi;

  private ObjectMapper yamlOutput;

  private PebbleEngine engine;

  private OpenAPIExt openapi;

  private final AutoDataFakerMapper faker = new AutoDataFakerMapper();

  private final Map<Schema<?>, Map<String, Object>> examples = new HashMap<>();

  private final Instant now = Instant.now();

  public AsciiDocContext(Path baseDir, ObjectMapper json, ObjectMapper yaml, OpenAPIExt openapi) {
    this.json = json;
    this.yamlOpenApi = yaml;
    this.yamlOutput = newYamlOutput();
    this.openapi = openapi;
    this.engine = createEngine(baseDir, json, this);
  }

  public String generate(Path index) throws IOException {
    var template = engine.getTemplate(index.getFileName().toString());
    var writer = new StringWriter();
    var context = new HashMap<String, Object>();
    template.evaluate(writer, context);
    return writer.toString().trim();
  }

  public void export(Path input, Path outputDir) {
    try (var asciidoctor = Asciidoctor.Factory.create()) {

      var options =
          Options.builder()
              .backend("html5")
              .baseDir(input.getParent().toFile())
              .toDir(outputDir.toFile())
              .mkDirs(true)
              .safe(SafeMode.UNSAFE)
              .build();

      // Perform the conversion
      asciidoctor.convertFile(input.toFile(), options);
    }
  }

  public Instant getNow() {
    return now;
  }

  private ObjectMapper newYamlOutput() {
    var factory = new YAMLFactory();
    factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    return new ObjectMapper(factory);
  }

  private static PebbleEngine createEngine(
      Path baseDir, ObjectMapper json, AsciiDocContext context) {
    List<Loader<?>> loaders =
        List.of(new FileLoader(baseDir.toAbsolutePath().toString()), new ClasspathLoader());
    return new PebbleEngine.Builder()
        .autoEscaping(false)
        .loader(new DelegatingLoader(loaders))
        .extension(
            new AbstractExtension() {
              @Override
              public Map<String, Object> getGlobalVariables() {
                Map<String, Object> openapiRoot = json.convertValue(context.openapi, Map.class);
                openapiRoot.put("openapi", context.openapi);
                openapiRoot.put("now", context.now);

                // Global/Default values:
                openapiRoot.put(
                    "error",
                    Map.of(
                        "statusCode",
                        "{{statusCode.code}}",
                        "reason",
                        "{{statusCode.reason}}",
                        "message",
                        "..."));
                // Routes
                var operations =
                    context.openapi.getOperations().stream()
                        .map(op -> new HttpRequest(context, op, Map.of()))
                        .toList();
                // so we can print routes without calling function: routes() vs routes
                openapiRoot.put("routes", operations);
                openapiRoot.put("operations", operations);

                // make in to work without literal
                openapiRoot.put("query", "query");
                openapiRoot.put("path", "path");
                openapiRoot.put("header", "header");
                openapiRoot.put("cookie", "cookie");

                openapiRoot.put("_asciidocContext", context);
                return openapiRoot;
              }

              @Override
              public Map<String, Function> getFunctions() {
                return Stream.of(Lookup.values())
                    .flatMap(it -> it.alias().stream().map(name -> Map.entry(name, it)))
                    .collect(Collectors.toMap(Map.Entry::getKey, it -> wrapFn(it.getValue())));
              }

              private static Function wrapFn(Lookup lookup) {
                return new Function() {
                  @Override
                  public List<String> getArgumentNames() {
                    return lookup.getArgumentNames();
                  }

                  @Override
                  public Object execute(
                      Map<String, Object> args,
                      PebbleTemplate self,
                      EvaluationContext context,
                      int lineNumber) {
                    try {
                      return lookup.execute(args, self, context, lineNumber);
                    } catch (PebbleException rethrow) {
                      throw rethrow;
                    } catch (Throwable cause) {
                      var path = Paths.get(self.getName());
                      throw new PebbleException(
                          cause,
                          "execution of `" + lookup.name() + "()` resulted in exception:",
                          lineNumber,
                          path.getFileName().toString().trim());
                    }
                  }
                };
              }

              private static Filter wrapFilter(String filterName, Filter filter) {
                return new Filter() {
                  @Override
                  public List<String> getArgumentNames() {
                    return filter.getArgumentNames();
                  }

                  @Override
                  public Object apply(
                      Object input,
                      Map<String, Object> args,
                      PebbleTemplate self,
                      EvaluationContext context,
                      int lineNumber)
                      throws PebbleException {
                    try {
                      return filter.apply(input, args, self, context, lineNumber);
                    } catch (PebbleException rethrow) {
                      throw rethrow;
                    } catch (Throwable cause) {
                      var path = Paths.get(self.getName());
                      throw new PebbleException(
                          cause,
                          "execution of `" + filterName + "()` resulted in exception:",
                          lineNumber,
                          path.getFileName().toString().trim());
                    }
                  }
                };
              }

              @Override
              public Map<String, Filter> getFilters() {
                return Stream.concat(Stream.of(Mutator.values()), Stream.of(Display.values()))
                    .collect(Collectors.toMap(Enum::name, it -> wrapFilter(it.name(), it)));
              }
            })
        .syntax(new Syntax.Builder().setEnableNewLineTrimming(false).build())
        .build();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> error(EvaluationContext context, Map<String, Object> args) {
    var error = context.getVariable("error");
    if (error instanceof Map<?, ?> errorMap) {
      var mutableMap = new TreeMap<String, Object>((Map<? extends String, ?>) errorMap);
      args.forEach(
          (key, value) -> {
            if (mutableMap.containsKey(key)) {
              mutableMap.put(key, value);
            }
          });
      var statusCode =
          StatusCode.valueOf(
              ((Number)
                      args.getOrDefault(
                          "code", args.getOrDefault("statusCode", StatusCode.SERVER_ERROR.value())))
                  .intValue());
      for (var entry : errorMap.entrySet()) {
        var value = entry.getValue();
        var template = String.valueOf(value);
        if (template.startsWith("{{") && template.endsWith("}}")) {
          var variable = template.substring(2, template.length() - 2).trim();
          value =
              switch (variable) {
                case "status.reason",
                    "statusCodeReason",
                    "statusCode.reason",
                    "code.reason",
                    "codeReason",
                    "reason" ->
                    statusCode.reason();
                case "status.code", "statusCode.code", "statusCode", "code" -> statusCode.value();
                default -> Optional.ofNullable(context.getVariable(variable)).orElse(template);
              };
          mutableMap.put((String) entry.getKey(), value);
        }
      }
      return mutableMap;
    }
    throw new ClassCastException("Global error must be a map: " + error);
  }

  public String schemaType(Schema<?> schema) {
    var resolved = resolveSchema(schema);
    return Optional.ofNullable(resolved.getFormat()).orElse(resolveType(resolved));
  }

  private String resolveType(Schema<?> schema) {
    var resolved = resolveSchema(schema);
    if (resolved.getType() == null) {
      return resolved.getTypes().iterator().next();
    }
    return resolved.getType();
  }

  public Schema<?> resolveSchema(Schema<?> schema) {
    if (schema.get$ref() != null) {
      return resolveSchemaInternal(schema.get$ref())
          .orElseThrow(() -> new NoSuchElementException("Schema not found: " + schema.get$ref()));
    }
    return schema;
  }

  public Map<String, Object> schemaProperties(Schema<?> schema) {
    return traverse(schema, NOOP);
  }

  @SuppressWarnings("rawtypes")
  public Schema<?> reduceSchema(Schema<?> schema) {
    var truncated = emptySchema(schema);
    var properties = new LinkedHashMap<String, Schema>();
    traverse(
        schema,
        (name, value) -> {
          var type = resolveType(value);
          if ("object".equals(type)) {
            var object = new Schema<>();
            object.setType(type);
            properties.put(name, object);
          } else if ("array".equals(type)) {
            var array = new Schema<>();
            array.setType(type);
            array.setItems(new Schema<>());
            properties.put(name, array);
          } else {
            properties.put(name, value);
          }
        });
    truncated.setProperties(properties);
    return truncated;
  }

  public Schema<?> emptySchema(Schema<?> schema) {
    var empty = new Schema<>();
    empty.setType(resolveType(schema));
    empty.setName(schema.getName());
    return empty;
  }

  public Map<String, Object> schemaExample(Schema<?> schema) {
    return examples.computeIfAbsent(
        schema,
        s ->
            traverse(
                new HashSet<>(),
                s,
                (parent, property) -> {
                  var enumItems = property.getEnum();
                  if (enumItems == null || enumItems.isEmpty()) {
                    var type = schemaType(property);
                    var gen = faker.getGenerator(parent.getName(), property.getName(), type, type);
                    return gen.get();
                  } else {
                    return enumItems.get(new Random().nextInt(enumItems.size())).toString();
                  }
                },
                NOOP,
                NOOP));
  }

  public void traverseSchema(Schema<?> schema, BiConsumer<String, Schema<?>> consumer) {
    traverse(schema, consumer);
  }

  private Map<String, Object> traverse(Schema<?> schema, BiConsumer<String, Schema<?>> consumer) {
    return traverse(schema, consumer, NOOP);
  }

  private Map<String, Object> traverse(
      Schema<?> schema,
      BiConsumer<String, Schema<?>> consumer,
      BiConsumer<String, Schema<?>> inner) {
    return traverse(
        new HashSet<>(), schema, (parent, property) -> schemaType(property), consumer, inner);
  }

  private Map<String, Object> traverse(
      Set<Object> visited,
      Schema<?> schema,
      SneakyThrows.Function2<Schema<?>, Schema<?>, String> valueMapper,
      BiConsumer<String, Schema<?>> consumer,
      BiConsumer<String, Schema<?>> inner) {
    if (schema == EMPTY_SCHEMA) {
      return Map.of();
    }
    var resolved = resolveSchema(schema);
    if (visited.add(resolved)) {
      var properties = resolved.getProperties();
      if (properties != null) {
        Map<String, Object> result = new LinkedHashMap<>();
        properties.forEach(
            (name, value) -> {
              var resolvedValue = resolveSchema(value);
              var valueType = resolveType(resolvedValue);
              consumer.accept(name, resolvedValue);
              if ("object".equals(valueType)) {
                result.put(name, traverse(visited, resolvedValue, valueMapper, inner, inner));
              } else if ("array".equals(valueType)) {
                var array =
                    ofNullable(resolvedValue.getItems())
                        .map(
                            items ->
                                traverse(visited, resolveSchema(items), valueMapper, inner, inner))
                        .map(List::of)
                        .orElse(List.of());
                result.put(name, array);
              } else {
                result.put(name, valueMapper.apply(resolved, resolvedValue));
              }
            });
        return result;
      }
    }
    return Map.of();
  }

  public Schema<?> resolveSchema(String path) {
    var segments = path.split("\\.");
    var schema =
        resolveSchemaInternal(segments[0])
            .orElseThrow(() -> new NoSuchElementException("Schema not found: " + path));

    for (int i = 1; i < segments.length; i++) {
      Schema<?> inner = (Schema<?>) schema.getProperties().get(segments[i]);
      if (inner == null) {
        throw new IllegalArgumentException(
            "Property not found: " + Stream.of(segments).limit(i).collect(Collectors.joining(".")));
      }
      if (inner.get$ref() != null) {
        inner =
            resolveSchemaInternal(inner.get$ref())
                .orElseThrow(() -> new NoSuchElementException("Schema not found: " + path));
      }
      schema = inner;
    }
    return schema;
  }

  private Optional<Schema<?>> resolveSchemaInternal(String name) {
    var components = openapi.getComponents();
    if (components == null || components.getSchemas() == null) {
      throw new NoSuchElementException("No schema found");
    }
    if (name.startsWith("#/components/schemas/")) {
      name = name.substring("#/components/schemas/".length());
    }
    return Optional.ofNullable((Schema<?>) components.getSchemas().get(name));
  }

  public PebbleEngine getEngine() {
    return engine;
  }

  public String toJson(Object input, boolean pretty) {
    try {
      var writer = pretty ? json.writer().withDefaultPrettyPrinter() : json.writer();
      return writer.writeValueAsString(input);
    } catch (JsonProcessingException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  public String toYaml(Object input) {
    try {
      return cleanYaml(
          input instanceof Map
              ? yamlOutput.writeValueAsString(input)
              : yamlOpenApi.writeValueAsString(input));
    } catch (JsonProcessingException e) {
      throw SneakyThrows.propagate(e);
    }
  }

  private String cleanYaml(String value) {
    return value.trim();
  }

  public ObjectMapper getJson() {
    return json;
  }

  public ObjectMapper getYaml() {
    return yamlOpenApi;
  }

  public OpenAPIExt getOpenApi() {
    return openapi;
  }

  public static AsciiDocContext from(EvaluationContext context) {
    return (AsciiDocContext) context.getVariable("_asciidocContext");
  }
}
