/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static java.util.Optional.ofNullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.lexer.Syntax;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public class AsciiDocContext {
  public static final BiConsumer<String, Schema<?>> NOOP = (name, schema) -> {};
  public static final Schema EMPTY_SCHEMA = new Schema<>();

  private ClassLoader classLoader;

  private ObjectMapper json;

  private ObjectMapper yamlOpenApi;

  private ObjectMapper yamlOutput;

  private PebbleEngine engine;

  private OpenAPIExt openapi;

  private Path baseDir;

  private Path outputDir;

  private final AutoDataFakerMapper faker = new AutoDataFakerMapper();

  private final Map<Schema<?>, Map<String, Object>> examples = new HashMap<>();

  public AsciiDocContext(
      ClassLoader classLoader,
      ObjectMapper json,
      ObjectMapper yaml,
      OpenAPIExt openapi,
      Path baseDir,
      Path outputDir) {
    this.classLoader = classLoader;
    this.json = json;
    this.yamlOpenApi = yaml;
    this.yamlOutput = newYamlOutput();
    this.engine = createEngine(json, openapi, this);
    this.openapi = openapi;
    this.baseDir = baseDir;
    this.outputDir = outputDir;
  }

  private ObjectMapper newYamlOutput() {
    var factory = new YAMLFactory();
    factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    return new ObjectMapper(factory);
  }

  private static PebbleEngine createEngine(
      ObjectMapper json, OpenAPI openapi, AsciiDocContext context) {
    return new PebbleEngine.Builder()
        .autoEscaping(false)
        .extension(
            new AbstractExtension() {
              @Override
              public Map<String, Object> getGlobalVariables() {
                Map<String, Object> openapiRoot = json.convertValue(openapi, Map.class);
                openapiRoot.put("openapi", openapi);
                openapiRoot.put("_asciidocContext", context);
                return openapiRoot;
              }

              @Override
              public Map<String, Function> getFunctions() {
                return Lookup.lookup();
              }

              @Override
              public Map<String, Filter> getFilters() {
                Map<String, Filter> filters = new HashMap<>();
                filters.putAll(Mutator.seek());
                filters.putAll(Display.display());
                return filters;
              }
            })
        .syntax(new Syntax.Builder().setEnableNewLineTrimming(false).build())
        .build();
  }

  public String schemaType(Schema<?> schema) {
    var resolved = resolveSchema(schema);
    return Optional.ofNullable(resolved.getFormat()).orElse(resolved.getType());
  }

  public Schema<?> resolveSchema(Schema<?> schema) {
    if (schema == EMPTY_SCHEMA) {
      return schema;
    }
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
          var type = value.getType();
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
    empty.setType(schema.getType());
    empty.setName(schema.getName());
    return empty;
  }

  public Map<String, Object> schemaExample(Schema<?> schema) {
    return examples.computeIfAbsent(
        schema,
        s ->
            traverse(
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

  public void traverseGraph(Schema<?> schema, BiConsumer<String, Schema<?>> consumer) {
    traverse(schema, consumer, consumer);
  }

  private Map<String, Object> traverse(Schema<?> schema, BiConsumer<String, Schema<?>> consumer) {
    return traverse(schema, consumer, NOOP);
  }

  private Map<String, Object> traverse(
      Schema<?> schema,
      BiConsumer<String, Schema<?>> consumer,
      BiConsumer<String, Schema<?>> inner) {
    return traverse(schema, (parent, property) -> schemaType(property), consumer, inner);
  }

  private Map<String, Object> traverse(
      Schema<?> schema,
      SneakyThrows.Function2<Schema<?>, Schema<?>, String> valueMapper,
      BiConsumer<String, Schema<?>> consumer,
      BiConsumer<String, Schema<?>> inner) {
    var resolved = resolveSchema(schema);
    var properties = resolved.getProperties();
    if (properties != null) {
      Map<String, Object> result = new LinkedHashMap<>();
      properties.forEach(
          (name, value) -> {
            value = resolveSchema(value);
            consumer.accept(name, value);
            if (value.getType().equals("object")) {
              result.put(name, traverse(value, valueMapper, inner, inner));
            } else if (value.getType().equals("array")) {
              var array =
                  ofNullable(value.getItems())
                      .map(items -> traverse(items, valueMapper, inner, inner))
                      .map(List::of)
                      .orElse(List.of());
              result.put(name, array);
            } else {
              result.put(name, valueMapper.apply(resolved, value));
            }
          });
      return result;
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

  public Path getBaseDir() {
    return baseDir;
  }

  public Path getOutputDir() {
    return outputDir;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
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
