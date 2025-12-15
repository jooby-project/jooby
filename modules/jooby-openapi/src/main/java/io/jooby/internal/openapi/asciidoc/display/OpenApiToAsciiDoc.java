/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc.display;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import io.jooby.internal.openapi.EnumSchema;
import io.jooby.internal.openapi.asciidoc.AsciiDocContext;
import io.jooby.internal.openapi.asciidoc.ParameterList;
import io.jooby.internal.openapi.asciidoc.ToAsciiDoc;
import io.swagger.v3.oas.models.media.Schema;

public record OpenApiToAsciiDoc(
    AsciiDocContext context,
    Map<String, Schema<?>> properties,
    List<String> columns,
    Map<String, Object> additionalProperties)
    implements ToAsciiDoc {
  private static final String ROOT = "___root__";

  public static OpenApiToAsciiDoc schema(AsciiDocContext context, Schema<?> schema) {
    var columns =
        schema instanceof EnumSchema
            ? List.of("name", "description")
            : List.of("name", "type", "description");
    var properties = new LinkedHashMap<String, Schema<?>>();
    properties.put(OpenApiToAsciiDoc.ROOT, schema);
    context.traverseSchema(schema, properties::put);
    return new OpenApiToAsciiDoc(context, properties, columns, Map.of());
  }

  public static OpenApiToAsciiDoc parameters(AsciiDocContext context, ParameterList parameters) {
    var properties = new LinkedHashMap<String, Schema<?>>();
    parameters.forEach(p -> properties.put(p.getName(), p.getSchema()));
    Map<String, Object> additionalProperties = new LinkedHashMap<>();
    parameters.forEach(
        p -> {
          additionalProperties.put(p.getName() + ".in", p.getIn());
          additionalProperties.put(p.getName() + ".description", p.getDescription());
        });
    return new OpenApiToAsciiDoc(context, properties, parameters.includes(), additionalProperties);
  }

  public String list(Map<String, Object> options) {
    var isEnum = properties.get(ROOT) instanceof EnumSchema;
    var sb = new StringBuilder();
    if (isEnum) {
      var enumSchema = (EnumSchema) properties.remove(ROOT);
      for (var enumName : enumSchema.getEnum()) {
        sb.append(boldCell(enumName)).append("::").append('\n');
        var enumDesc = enumSchema.getDescription(enumName);
        if (enumDesc != null) {
          sb.append("* ").append(enumDesc);
        }
        sb.append('\n');
      }
    } else {
      properties.remove(ROOT);
      properties.forEach(
          (name, value) -> {
            sb.append(name).append("::").append('\n');
            sb.append("* ")
                .append("type")
                .append(": ")
                .append(monospaceCell(context.schemaType(value)))
                .append('\n');
            var in = additionalProperties.get(name + ".in");
            if (in != null) {
              sb.append("* ")
                  .append("in")
                  .append(": ")
                  .append(monospaceCell((String) in))
                  .append('\n');
            }
            var isEnumProperty = value instanceof EnumSchema;
            var description =
                isEnumProperty ? ((EnumSchema) value).getSummary() : value.getDescription();
            if (isEnumProperty) {
              sb.append("* ").append("description").append(":");
              if (description != null) {
                sb.append(" ").append(description);
              }
              sb.append('\n');
              var enumSchema = (EnumSchema) value;
              for (var enumName : enumSchema.getEnum()) {
                sb.append("** ").append(boldCell(enumName));
                var enumDesc = enumSchema.getDescription(enumName);
                if (enumDesc != null) {
                  sb.append(": ").append(enumDesc);
                }
                sb.append('\n');
              }
            } else {
              if (description != null) {
                sb.append("* ").append("description").append(": ").append(description).append('\n');
              }
            }
          });
    }
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  @SuppressWarnings({"unchecked"})
  public String table(Map<String, Object> options) {
    var isEnum = properties.get(ROOT) instanceof EnumSchema;
    var columns = (List<String>) options.getOrDefault("columns", this.columns);
    options.remove("columns");
    var colList = colList(columns);
    var sb = new StringBuilder();
    sb.append("|===").append('\n');
    sb.append(header(columns)).append('\n');
    if (isEnum) {
      var enumSchema = (EnumSchema) properties.remove(ROOT);
      for (var enumName : enumSchema.getEnum()) {
        sb.append("| ").append(boldCell(enumName)).append('\n');
        var enumDesc = enumSchema.getDescription(enumName);
        if (enumDesc != null) {
          sb.append("| ").append(enumDesc);
        }
        sb.append('\n');
      }
    } else {
      properties.remove(ROOT);
      properties.forEach(
          (name, value) -> {
            var isPropertyEnum = value instanceof EnumSchema;
            for (int i = 0; i < columns.size(); i++) {
              var column = columns.get(i);
              sb.append("|").append(row(column, name, value)).append("\n");
              if (isPropertyEnum && column.equals("description")) {
                colList.set(i, colList.get(i) + "a");
                var enumSchema = (EnumSchema) value;
                for (var enumValue : enumSchema.getEnum()) {
                  sb.append("\n* ").append(boldCell(enumValue));
                  var enumDesc = enumSchema.getDescription(enumValue);
                  if (enumDesc != null) {
                    sb.append(": ").append(enumDesc);
                  }
                }
                sb.append('\n');
              }
            }
            sb.append('\n');
          });
    }
    sb.append("|===");
    options.putIfAbsent("cols", colsToString(colList));
    if (options.size() == 1) {
      options.put("options", "header");
    }
    return options.entrySet().stream()
            .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
            .collect(Collectors.joining(", ", "[", "]"))
        + "\n"
        + sb;
  }

  private String colsToString(List<String> cols) {
    return String.join(",", cols);
  }

  private List<String> colList(List<String> names) {
    return names.stream()
        .map(it -> it.equals("description") ? "3" : "1")
        .collect(Collectors.toList());
  }

  private String header(List<String> names) {
    return names.stream()
        .map(it -> CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, it))
        .collect(Collectors.joining("|", "|", ""));
  }

  private static String monospaceCell(String value) {
    return value == null || value.trim().isEmpty() ? "" : "`+" + value + "+`";
  }

  private String boldCell(String value) {
    return value == null || value.trim().isEmpty() ? "" : "*" + value + "*";
  }

  private String nullSafe(String value) {
    return value == null || value.trim().isEmpty() ? "" : value;
  }

  private String row(String col, String property, Schema<?> schema) {
    return nullSafe(
        switch (col) {
          case "name" -> monospaceCell(property);
          case "type" -> monospaceCell(context.schemaType(schema));
          case "in" -> monospaceCell((String) additionalProperties.get(property + "." + col));
          case "description" ->
              (schema instanceof EnumSchema enumSchema
                  ? enumSchema.getSummary()
                  : (String)
                      additionalProperties.getOrDefault(
                          property + "." + col, schema.getDescription()));
          default -> throw new IllegalArgumentException("Unknown property: " + col);
        });
  }
}
