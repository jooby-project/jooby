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
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static io.jooby.internal.openapi.AsmUtils.boolValue;
import static io.jooby.internal.openapi.AsmUtils.enumValue;
import static io.jooby.internal.openapi.AsmUtils.intValue;
import static io.jooby.internal.openapi.AsmUtils.toMap;
import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static io.jooby.internal.openapi.AsmUtils.stringValue;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class OpenApiParser {
  public static void parse(ParserContext ctx, MethodNode method, OperationExt operation) {
    /** @Operation: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(io.swagger.v3.oas.annotations.Operation.class.getName())).stream()
        .findFirst()
        .ifPresent(a -> swaggerOperation(ctx, operation, toMap(a)));

    /** @ApiResponses: */
    List<ResponseExt> responses = findAnnotationByType(method.visibleAnnotations,
        singletonList(ApiResponses.class.getName()))
        .stream()
        .flatMap(a -> (
                (List<AnnotationNode>) toMap(a)
                    .getOrDefault("value", emptyList())
            ).stream()
        )
        .map(a -> operationResponse(ctx, operation, toMap(a)))
        .collect(Collectors.toList());

    if (responses.isEmpty()) {
      /** @ApiResponse: */
      findAnnotationByType(method.visibleAnnotations, singletonList(ApiResponse.class.getName()))
          .stream()
          .findFirst()
          .map(a -> operationResponse(ctx, operation, toMap(a)))
          .ifPresent(a -> operation.setResponses(apiResponses(a)));
    } else {
      operation.setResponses(apiResponses(responses));
    }
  }

  private static void swaggerOperation(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    stringValue(annotation, "operationId", operation::setOperationId);

    stringValue(annotation, "method", operation::setMethod);

    boolValue(annotation, "deprecated", operation::setDeprecated);

    boolValue(annotation, "hidden", operation::setHidden);

    stringValue(annotation, "summary", operation::setSummary);

    stringValue(annotation, "description", operation::setDescription);

    List<String> tags = (List<String>) annotation.getOrDefault("tags", emptyList());
    tags.forEach(operation::addTagsItem);

    operationParameter(ctx, operation,
        (List<AnnotationNode>) annotation.getOrDefault("parameters", Collections.emptyList()));

    List<ResponseExt> response = operationResponses(ctx, operation, annotation);
    if (response.size() > 0) {
      operation.setResponses(apiResponses(response));
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
  private static void operationParameter(ParserContext ctx, OperationExt operation,
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
    }
  }

  private static List<ResponseExt> operationResponses(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    List<AnnotationNode> responses = (List<AnnotationNode>) annotation
        .getOrDefault("responses", emptyList());
    if (responses.size() > 0) {
      // clear any detected response
      List<ResponseExt> returnTypes = responses.stream()
          .map(it -> toMap(it))
          .map(it -> operationResponse(ctx, operation, it))
          .collect(Collectors.toList());
      return returnTypes;
    }
    return Collections.emptyList();
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse)
  private static ResponseExt operationResponse(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    ResponseExt response = new ResponseExt();
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

    String code = (String) annotation.getOrDefault("responseCode", "default");
    String description = (String) annotation.getOrDefault("description", "");

    operationResponseContent(ctx, operation, response, annotation);

    if (description.trim().length() > 0) {
      response.setDescription(description.trim());
    }
    response.setCode(code);
    return response;
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse(content = @Content))
  private static void operationResponseContent(ParserContext ctx, OperationExt operation,
      ResponseExt response, Map<String, Object> annotation) {
    List<AnnotationNode> contents = (List<AnnotationNode>) annotation
        .getOrDefault("content", Collections.emptyList());
    contents.stream()
        .map(it -> toMap(it))
        .forEach(it -> responseContent(ctx, operation, response, it));
  }

  @ApiResponse(
      content = @Content(
          mediaType = "media/type",
          array = @ArraySchema(schema = @Schema(implementation = String.class))
      )
  )
  private static void responseContent(ParserContext ctx, OperationExt operation,
      ResponseExt response, Map<String, Object> contentMap) {
    Optional<io.swagger.v3.oas.models.media.Schema> schema = arrayOrSchema(ctx, contentMap);
    String mediaType = (String) contentMap
        .getOrDefault("mediaType",
            operation.getProduces().stream().findFirst().orElse(MediaType.JSON));
    io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
    schema.ifPresent(mediaTypeObject::setSchema);

    io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
    content.addMediaType(mediaType, mediaTypeObject);

    response.setContent(content);
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

  private static io.swagger.v3.oas.models.responses.ApiResponses apiResponses(
      ResponseExt... responses) {
    return apiResponses(Arrays.asList(responses));
  }

  private static io.swagger.v3.oas.models.responses.ApiResponses apiResponses(
      List<ResponseExt> responses) {
    io.swagger.v3.oas.models.responses.ApiResponses result = new io.swagger.v3.oas.models.responses.ApiResponses();
    responses.forEach(r -> result.addApiResponse(r.getCode(), r));
    return result;
  }
}
