/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.MediaType;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.jooby.internal.openapi.AsmUtils.boolValue;
import static io.jooby.internal.openapi.AsmUtils.enumValue;
import static io.jooby.internal.openapi.AsmUtils.intValue;
import static io.jooby.internal.openapi.AsmUtils.toMap;
import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static io.jooby.internal.openapi.AsmUtils.stringValue;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Complement openAPI output with swagger annotations.
 */
public class OpenAPIParser {
  public static void parse(ParserContext ctx, MethodNode method, OperationExt operation) {
    /** @Operation: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(io.swagger.v3.oas.annotations.Operation.class.getName())).stream()
        .findFirst()
        .ifPresent(a -> operation(ctx, operation, toMap(a)));

    /** SecurityRequirements: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(SecurityRequirements.class.getName()))
        .stream()
        .map(a ->
            (List<AnnotationNode>) toMap(a).getOrDefault("value", emptyList())
        )
        .forEach(a -> securityRequirements(operation, a));

    /** SecurityRequirement: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(io.swagger.v3.oas.annotations.security.SecurityRequirement.class.getName()))
        .stream()
        .findFirst()
        .ifPresent(a -> securityRequirements(operation, Collections.singletonList(a)));

    /** @ApiResponses: */
    findAnnotationByType(method.visibleAnnotations, singletonList(ApiResponses.class.getName()))
        .stream()
        .flatMap(a -> (
                (List<AnnotationNode>) toMap(a)
                    .getOrDefault("value", emptyList())
            ).stream()
        )
        .forEach(a -> operationResponse(ctx, operation, toMap(a)));

    /** @ApiResponse: */
    findAnnotationByType(method.visibleAnnotations, singletonList(ApiResponse.class.getName()))
        .stream()
        .findFirst()
        .ifPresent(a -> operationResponse(ctx, operation, toMap(a)));

    checkDefaultResponse(operation);
  }

  /**
   * This method removes the default response when there is an explicit success response.
   * @param operation
   */
  private static void checkDefaultResponse(OperationExt operation) {
    if (!operation.getResponseCodes().contains("200") &&
        operation.getResponses().keySet().stream().filter(StatusCodeParser::isSuccessCode).count()
            > 1) {
      operation.getResponses().remove("200");
    }
  }

  private static void operation(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    stringValue(annotation, "operationId", operation::setOperationId);

    stringValue(annotation, "method", operation::setMethod);

    boolValue(annotation, "deprecated", operation::setDeprecated);

    boolValue(annotation, "hidden", operation::setHidden);

    stringValue(annotation, "summary", operation::setSummary);

    stringValue(annotation, "description", operation::setDescription);

    List<String> tags = (List<String>) annotation.getOrDefault("tags", emptyList());
    tags.forEach(operation::addTagsItem);

    securityRequirements(operation,
        (List<AnnotationNode>) annotation.getOrDefault("security", Collections.emptyList()));

    parameters(ctx, operation,
        (List<AnnotationNode>) annotation.getOrDefault("parameters", Collections.emptyList()));

    requestBody(ctx, operation, toMap((AnnotationNode) annotation.get("requestBody")));

    responses(ctx, operation, annotation);
  }

  private static void securityRequirements(OperationExt operation,
      List<AnnotationNode> securityRequirements) {
    List<SecurityRequirement> requirements = new ArrayList<>();
    for (AnnotationNode annotation : securityRequirements) {
      Map<String, Object> securityMap = toMap(annotation);
      String name = (String) securityMap.get("name");
      List<String> scopes = (List<String>) securityMap
          .getOrDefault("scopes", Collections.emptyList());
      SecurityRequirement requirement = new SecurityRequirement();
      requirement.addList(name, scopes);

      requirements.add(requirement);
    }
    if (requirements.size() > 0) {
      operation.setSecurity(requirements);
    }
  }

  private static void requestBody(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    if (annotation.size() > 0) {
      RequestBodyExt requestBody = operation.getRequestBody();
      if (requestBody == null) {
        requestBody = new RequestBodyExt();
        operation.setRequestBody(requestBody);
      }
      stringValue(annotation, "description", requestBody::setDescription);
      boolValue(annotation, "required", requestBody::setRequired);
      String defaultMediaType = operation.getConsumes().stream().findFirst().orElse(MediaType.JSON);
      content(ctx, defaultMediaType, annotation).ifPresent(requestBody::setContent);
    }
  }

  @io.swagger.v3.oas.annotations.Operation(parameters =
  @Parameter(
      name = "p",
      description = "des",
      in = ParameterIn.COOKIE,
      required = false,
      deprecated = true,
      allowEmptyValue = true,
      allowReserved = false,
      hidden = false,
      explode = Explode.TRUE,
      ref = "Pet"
  )
  )
  private static void parameters(ParserContext ctx, OperationExt operation,
      List<AnnotationNode> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      Map<String, Object> parameterMap = toMap(parameters.get(i));
      String name = (String) parameterMap.get("name");
      io.swagger.v3.oas.models.parameters.Parameter parameter;
      if (name != null) {
        int index = i;
        parameter = operation.getParameters().stream()
            .filter(it -> it.getName().equals(name))
            .findFirst()
            .orElseGet(() -> operation.getParameter(index));
      } else {
        parameter = operation.getParameter(i);
      }
      if (parameter == null) {
        throw new IllegalArgumentException(
            "Parameter not found: " + name + " at  position: " + i + " for annotation: "
                + parameterMap);
      }
      Optional.ofNullable(name).ifPresent(parameter::setName);
      stringValue(parameterMap, "description", parameter::setDescription);
      enumValue(parameterMap, "in", in -> parameter.setIn(in.toLowerCase()));
      boolValue(parameterMap, "required", parameter::setRequired);
      boolValue(parameterMap, "deprecated", parameter::setDeprecated);
      boolValue(parameterMap, "allowEmptyValue", parameter::setAllowEmptyValue);
      boolValue(parameterMap, "allowReserved", parameter::setAllowReserved);
      // NOTE: Hidden is not present on parameter
      //boolValue(parameterMap, "hidden", parameter::setHidden);
      enumValue(parameterMap, "explode", value -> parameter.setExample(Boolean.valueOf(value)));
      stringValue(parameterMap, "ref", ref -> parameter.set$ref(RefUtils.constructRef(ref)));
      arrayOrSchema(ctx, parameterMap).ifPresent(parameter::setSchema);
      examples(parameterMap, parameter::setExample, parameter::setExamples);
    }
  }

  private static void examples(Map<String, Object> annotation, Consumer<String> exampleConsumer,
      Consumer<Map<String, Example>> consumer) {
    List<Map<String, Object>> annotations = ((List<AnnotationNode>) annotation
        .getOrDefault("examples", emptyList())).stream()
        .map(AsmUtils::toMap)
        .collect(Collectors.toList());

    Map<String, Example> examples = new LinkedHashMap<>();
    stringValue(annotation, "example", exampleConsumer);

    for (Map<String, Object> e : annotations) {
      Example example = new Example();
      stringValue(e, "summary", example::setSummary);
      stringValue(e, "description", example::setDescription);
      stringValue(e, "value", example::setValue);
      stringValue(e, "externalValue", example::setExternalValue);
      String key = (String) e.getOrDefault("name", "example" + examples.size());
      examples.put(key, example);
    }
    if (examples.size() > 0) {
      consumer.accept(examples);
    }
  }

  private static void responses(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    List<AnnotationNode> responses = (List<AnnotationNode>) annotation
        .getOrDefault("responses", emptyList());
    responses.stream()
        .map(it -> toMap(it))
        .forEach(it -> operationResponse(ctx, operation, it));
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse)
  private static void operationResponse(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    String code = ((String) annotation.getOrDefault("responseCode", "200"))
        .replace("default", "200");

    ResponseExt response = operation.addResponse(code);
    Map<String, Header> headers = new LinkedHashMap<>();

    ((List<AnnotationNode>) annotation.getOrDefault("headers", Collections.emptyList())).stream()
        .map(a -> toMap(a))
        .forEach(a -> {
          String name = (String) a.get("name");
          Header h = new Header();
          stringValue(a, "description", h::setDescription);
          io.swagger.v3.oas.models.media.Schema schema = toSchema(ctx,
              toMap((AnnotationNode) a.get("schema")))
              .orElseGet(StringSchema::new);
          h.setSchema(schema);
          headers.put(name, h);
        });

    if (headers.size() > 0) {
      response.setHeaders(headers);
    }

    String description = (String) annotation.getOrDefault("description", "");

    String defaultMediaType = operation.getProduces().stream().findFirst().orElse(MediaType.JSON);
    content(ctx, defaultMediaType, annotation).ifPresent(response::setContent);

    if (description.trim().length() > 0) {
      response.setDescription(description.trim());
    }
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse(content = @Content))
  private static Optional<io.swagger.v3.oas.models.media.Content> content(ParserContext ctx,
      String defaultMediaType,
      Map<String, Object> annotation) {
    io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
    ((List<AnnotationNode>) annotation.getOrDefault("content", Collections.emptyList()))
        .stream()
        .map(n -> toMap(n))
        .forEach(a -> mediaType(ctx, content, defaultMediaType, a));
    return content.isEmpty() ? Optional.empty() : Optional.of(content);
  }

  @ApiResponse(
      content = @Content(
          mediaType = "media/type",
          array = @ArraySchema(schema = @Schema(implementation = String.class))
      )
  )
  private static void mediaType(ParserContext ctx, io.swagger.v3.oas.models.media.Content content,
      String defaultMediaType, Map<String, Object> contentMap) {
    if (contentMap == null || contentMap.isEmpty()) {
      return;
    }
    Optional<io.swagger.v3.oas.models.media.Schema> schema = arrayOrSchema(ctx, contentMap);
    String mediaType = (String) contentMap.getOrDefault("mediaType", defaultMediaType);
    io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
    schema.ifPresent(mediaTypeObject::setSchema);

    content.addMediaType(mediaType, mediaTypeObject);
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> arrayOrSchema(ParserContext ctx,
      Map<String, Object> annotation) {
    AnnotationNode e = (AnnotationNode) annotation.get("array");
    if (e != null) {
      return toArraySchema(ctx, toMap(e));
    } else {
      return toSchema(ctx, toMap((AnnotationNode) annotation.get("schema")));
    }
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> toArraySchema(ParserContext ctx,
      Map<String, Object> annotation) {
    io.swagger.v3.oas.models.media.ArraySchema arraySchema = new io.swagger.v3.oas.models.media.ArraySchema();
    boolValue(annotation, "uniqueItems", arraySchema::setUniqueItems);
    intValue(annotation, "maxItems", arraySchema::setMaxItems);
    intValue(annotation, "minItems", arraySchema::setMinItems);
    if (annotation.containsKey("arraySchema")) {
      toArraySchema(ctx, toMap((AnnotationNode) annotation.get("arraySchema")))
          .ifPresent(arraySchema::setItems);
    } else {
      toSchema(ctx, toMap((AnnotationNode) annotation.get("schema")))
          .ifPresent(arraySchema::setItems);
    }
    return Optional.of(arraySchema);
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> toSchema(ParserContext ctx,
      Map<String, Object> annotation) {
    Map<String, List<io.swagger.v3.oas.models.media.Schema>> schemaMap = new HashMap<>();

    schemaType(ctx, annotation, "implementation", schemaMap::put);
    schemaType(ctx, annotation, "not", schemaMap::put);
    schemaType(ctx, annotation, "anyOf", schemaMap::put);
    schemaType(ctx, annotation, "oneOf", schemaMap::put);
    schemaType(ctx, annotation, "allOf", schemaMap::put);

    if (schemaMap.isEmpty()) {
      return Optional.empty();
    }

    List<io.swagger.v3.oas.models.media.Schema> schemas = schemaMap.get("implementation");
    io.swagger.v3.oas.models.media.Schema schema;
    if (schemas.isEmpty()) {
      ComposedSchema composedSchema = new ComposedSchema();

      Optional.ofNullable(schemaMap.get("anyOf")).ifPresent(composedSchema::anyOf);
      Optional.ofNullable(schemaMap.get("oneOf")).ifPresent(composedSchema::oneOf);
      Optional.ofNullable(schemaMap.get("allOf")).ifPresent(composedSchema::allOf);

      schema = composedSchema;
    } else {
      schema = schemas.get(0);
    }

    Optional.ofNullable(schemaMap.get("not")).ifPresent(not -> schema.not(not.get(0)));

    return Optional.of(schema);
  }

  private static void schemaType(ParserContext ctx, Map<String, Object> schema, String property,
      BiConsumer<String, List<io.swagger.v3.oas.models.media.Schema>> consumer) {
    Object value = schema.get(property);
    List<Type> types;
    if (value instanceof List) {
      types = (List) value;
    } else if (value instanceof Type) {
      types = Collections.singletonList((Type) value);
    } else {
      types = Collections.emptyList();
    }
    if (types.size() > 0) {
      List<io.swagger.v3.oas.models.media.Schema> schemas = types.stream()
          .map(Type::getClassName)
          .map(ctx::schema)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      consumer.accept(property, schemas);
    }
  }
}
