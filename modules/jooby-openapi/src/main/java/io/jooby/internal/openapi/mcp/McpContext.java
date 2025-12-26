/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.mcp;

import static io.jooby.SneakyThrows.throwingFunction;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OpenApiSupport;
import io.jooby.internal.openapi.OperationExt;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;

public class McpContext extends OpenApiSupport {

  static {
    // type vs types difference in v30 vs v31
    System.setProperty(Schema.BIND_TYPE_AND_TYPES, Boolean.TRUE.toString());
  }

  private final ObjectMapper json = Json31.mapper();

  public McpContext(OpenAPIExt openapi) {
    super(openapi);
  }

  public String generate() throws IOException {
    var output = new LinkedHashMap<String, Object>();
    var tools = new LinkedHashMap<String, Object>();
    var templates = new LinkedHashMap<String, Object>();
    output.put("resourceTemplates", templates);
    output.put("tools", tools);

    for (OperationExt operation : openapi.getOperations()) {
      if (isTool(operation)) {
        var inputSchema = inputSchema(operation);
        var properties = inputSchema.getProperties();
        var parameters = ofNullable(operation.getParameters()).orElse(List.of());
        var required = new ArrayList<String>();
        for (var parameter : parameters) {
          var schema = cloneSchema(parameter.getSchema());
          var doc = parameter.getDescription();
          if (doc != null) {
            schema.setDescription(doc);
          }
          if (parameter.getRequired() == Boolean.TRUE) {
            required.add(parameter.getName());
          }
          properties.put(parameter.getName(), schema);
        }
        var rsp = operation.getDefaultResponse();
        Schema<?> outputSchema = null;
        if (rsp != null) {
          outputSchema =
              schemaOf(rsp.getContent())
                  .map(this::resolveSchema)
                  .map(throwingFunction(this::cloneSchema))
                  .orElse(null);
        }
        inputSchema.setRequired(required);
        var tool =
            new McpTool(
                toolId(operation.getOperationId()),
                operation.getSummary(),
                selfContained(inputSchema),
                selfContained(outputSchema));
        tools.put(operation.getOperationId(), tool);
      } else if (isResourceTemplate(operation)) {
        var properties = new LinkedHashMap<String, Schema>();
        var required = new ArrayList<String>();
        for (var parameter : ofNullable(operation.getParameters()).orElse(List.of())) {
          var schema = cloneSchema(parameter.getSchema());
          var doc = parameter.getDescription();
          if (doc != null) {
            schema.setDescription(doc);
          }
          if (parameter.getRequired() == Boolean.TRUE) {
            required.add(parameter.getName());
          }
          properties.put(parameter.getName(), schema);
        }
        var uriSchema =
            CaseFormat.UPPER_CAMEL.to(
                CaseFormat.LOWER_HYPHEN,
                Optional.ofNullable(operation.getTags()).orElse(List.of()).stream()
                    .findFirst()
                    .orElse(operation.getController().name));
        var name =
            operation.getGlobalTags().stream()
                .map(Tag::getDescription)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(operation.getPathSummary());
        var mimeType = operation.getProduces().stream().findFirst().orElse(MediaType.JSON);
        var parameters = new Schema<>();
        parameters.setType("object");
        parameters.setProperties(properties);
        parameters.setRequired(required);
        templates.put(
            operation.getOperationId(),
            new McpResourceTemplate(
                uriSchema + ":/" + operation.getPath(),
                name,
                operation.getDescription(),
                mimeType,
                parameters));
      }
      // resource vs tool
    }
    return json.writer().withDefaultPrettyPrinter().writeValueAsString(output);
  }

  private Schema<?> selfContained(Schema<?> in) {
    if (in != null) {
      if ("object".equals(in.getType())) {
        if (in.getProperties() != null) {
          Map<String, Schema> properties = new LinkedHashMap<>();
          for (var entry : in.getProperties().entrySet()) {
            var propertyIn = entry.getValue();
            var propertyOut = selfContained(cloneSchema(propertyIn));
            properties.put(entry.getKey(), propertyOut);
          }
          in.setProperties(properties);
          return in;
        }
      } else if ("array".equals(in.getType())) {
        var items = in.getItems();
        in.setItems(selfContained(resolveSchema(items)));
      }
    }
    return in;
  }

  private Schema<?> inputSchema(OperationExt operation) {
    var requestBody = operation.getRequestBody();
    Schema<?> result = null;
    if (requestBody != null) {
      var content = requestBody.getContent();
      result = schemaOf(content).map(throwingFunction(this::cloneSchema)).orElse(null);
    }
    if (result == null) {
      result = new Schema<>();
      result.setType("object");
    }
    if (result.getProperties() == null) {
      result.setProperties(new LinkedHashMap<>());
    }
    return result;
  }

  private Optional<Schema<?>> schemaOf(Content content) {
    if (content != null && !content.isEmpty()) {
      return ofNullable(content.values().iterator().next().getSchema());
    }
    return Optional.empty();
  }

  private Schema<?> cloneSchema(Schema<?> in) {
    try {
      var source = json.valueToTree(resolveSchema(in));
      var clone = json.treeToValue(source, Schema.class);
      clone.setType(in.getType());
      clone.setTypes(in.getTypes());
      clone.setName(in.getName());
      return clone;
    } catch (JsonProcessingException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private boolean isTool(OperationExt operation) {
    var parameters = ofNullable(operation.getParameters()).orElse(List.of());
    // No path, TODO: probably check extensions: like, x-tool
    return parameters.stream().noneMatch(p -> p.getIn().equals("path"));
  }

  private boolean isResourceTemplate(OperationExt operation) {
    var parameters = ofNullable(operation.getParameters()).orElse(List.of());
    // No path, TODO: probably check extensions: like, x-resource-template
    return operation.getMethod().equals("GET")
        && parameters.stream().anyMatch(p -> p.getIn().equals("path"));
  }

  private String toolId(String operationId) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, operationId);
  }
}
