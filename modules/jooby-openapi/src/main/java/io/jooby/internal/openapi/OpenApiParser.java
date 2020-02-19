package io.jooby.internal.openapi;

import io.jooby.MediaType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.jooby.internal.openapi.AsmUtils.arrayToMap;
import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class OpenApiParser {
  public static void parse(ExecutionContext ctx, MethodNode method, Operation operation) {
    /** @Operation: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(io.swagger.v3.oas.annotations.Operation.class.getName())).stream()
        .findFirst()
        .ifPresent(a -> swaggerOperation(ctx, operation, arrayToMap(a)));

    /** @ApiResponses: */
    List<Response> responses = findAnnotationByType(method.visibleAnnotations,
        singletonList(ApiResponses.class.getName()))
        .stream()
        .flatMap(a -> (
                (List<AnnotationNode>) arrayToMap(a)
                    .getOrDefault("value", emptyList())
            ).stream()
        )
        .map(a -> arrayToMap(a))
        .map(a -> operationResponse(ctx, operation, a))
        .collect(Collectors.toList());

    if (responses.isEmpty()) {
      /** @ApiResponse: */
      findAnnotationByType(method.visibleAnnotations, singletonList(ApiResponse.class.getName()))
          .stream()
          .findFirst()
          .map(a -> arrayToMap(a))
          .map(a -> operationResponse(ctx, operation, a))
          .ifPresent(a -> operation.setResponses(apiResponses(a)));
    } else {
      operation.setResponses(apiResponses(responses));
    }
  }

  private static void swaggerOperation(ExecutionContext ctx, Operation operation,
      Map<String, Object> annotation) {
    String operationId = (String) annotation.getOrDefault("operationId", "");
    if (operationId.trim().length() > 0) {
      operation.setOperationId(operationId.trim());
    }
    Boolean deprecated = (Boolean) annotation.get("deprecated");
    if (deprecated == Boolean.TRUE) {
      operation.setDeprecated(deprecated.booleanValue());
    }
    Boolean hidden = (Boolean) annotation.getOrDefault("hidden", false);
    operation.setHidden(hidden.booleanValue());

    String summary = (String) annotation.getOrDefault("summary", "");
    if (summary.trim().length() > 0) {
      operation.setSummary(summary.trim());
    }

    String desc = (String) annotation.getOrDefault("description", "");
    if (desc.trim().length() > 0) {
      operation.setDescription(desc.trim());
    }

    List<String> tags = (List<String>) annotation.getOrDefault("tags", emptyList());
    tags.forEach(operation::addTagsItem);

    List<Response> response = operationResponses(ctx, operation, annotation);
    if (response.size() > 0) {
      operation.setResponses(apiResponses(response));
    }
  }

  private static List<Response> operationResponses(ExecutionContext ctx, Operation operation,
      Map<String, Object> annotation) {
    List<AnnotationNode> responses = (List<AnnotationNode>) annotation
        .getOrDefault("responses", emptyList());
    if (responses.size() > 0) {
      // clear any detected response
      List<Response> returnTypes = responses.stream()
          .map(it -> arrayToMap(it))
          .map(it -> operationResponse(ctx, operation, it))
          .collect(Collectors.toList());
      return returnTypes;
    }
    return Collections.emptyList();
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse)
  private static Response operationResponse(ExecutionContext ctx, Operation operation,
      Map<String, Object> annotation) {
    Response response = new Response();
    Map<String, Header> headers = new LinkedHashMap<>();

    ((List<AnnotationNode>) annotation.getOrDefault("headers", Collections.emptyList())).stream()
        .map(a -> arrayToMap(a))
        .forEach(a -> {
          String name = (String) a.get("name");
          Header h = new Header();
          Optional.ofNullable(a.get("description")).map(Objects::toString)
              .ifPresent(h::setDescription);
          Optional.ofNullable(a.get("description")).map(Objects::toString)
              .ifPresent(h::setDescription);
          h.setSchema(schema(ctx, (AnnotationNode) a.get("schema")));
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

  private static io.swagger.v3.oas.models.media.Schema schema(ExecutionContext ctx,
      AnnotationNode annotation) {
    if (annotation == null) {
      return new StringSchema();
    }
    Type type = (Type) arrayToMap(annotation).get("implementation");
    if (type == null) {
      return new StringSchema();
    }
    return ctx.schema(type.getClassName());
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse(content = @Content))
  private static void operationResponseContent(ExecutionContext ctx, Operation operation,
      Response response, Map<String, Object> annotation) {
    List<AnnotationNode> contents = (List<AnnotationNode>) annotation
        .getOrDefault("content", Collections.emptyList());
    contents.stream()
        .map(it -> arrayToMap(it))
        .forEach(it -> responseContent(ctx, operation, response, it));
  }

  @ApiResponse(
      content = @Content(
          mediaType = "media/type",
          array = @ArraySchema(schema = @Schema(implementation = String.class))
      )
  )
  private static void responseContent(ExecutionContext ctx, Operation operation,
      Response response, Map<String, Object> contentMap) {
    Map<String, Object> schemaMap;
    String arrayType = null;
    AnnotationNode e = (AnnotationNode) contentMap.get("array");
    if (e != null) {
      Map<String, Object> array = arrayToMap(e);
      Boolean unique = (Boolean) array.getOrDefault("uniqueItems", false);
      AnnotationNode s = (AnnotationNode) array.get("schema");
      schemaMap = arrayToMap(s);
      arrayType = unique.booleanValue() ? Set.class.getName() : List.class.getName();
    } else {
      AnnotationNode s = (AnnotationNode) contentMap.get("schema");
      schemaMap =  arrayToMap(s);
    }
    io.swagger.v3.oas.models.media.Schema schema;
    if (arrayType != null) {
      Type implementation = (Type) schemaMap.get("implementation");
      if (implementation != null) {
        schema = ctx.schema(arrayType + "<" + implementation.getClassName() + ">");
      } else {
        throw new IllegalStateException(
            "ApiResponse content schema is set to array, but implementation type is not present");
      }
    } else {
      Type implementation = (Type) schemaMap.get("implementation");
      if (implementation != null) {
        schema = ctx.schema(implementation.getClassName());
      } else {
        ComposedSchema composed = new ComposedSchema();

        schemaType(ctx, schemaMap, "oneOf", composed::oneOf);
        schemaType(ctx, schemaMap, "allOf", composed::allOf);
        schemaType(ctx, schemaMap, "anyOf", composed::anyOf);
        schemaType(ctx, schemaMap, "not", list -> composed.not(list.get(0)));

        schema = composed;
      }
    }
    String mediaType = (String) contentMap
        .getOrDefault("mediaType", operation.getProduces().stream().findFirst().orElse(MediaType.JSON));
    io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
    mediaTypeObject.setSchema(schema);

    io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
    content.addMediaType(mediaType, mediaTypeObject);

    response.setContent(content);
  }

  private static void schemaType(ExecutionContext ctx, Map<String, Object> schema, String property,
      Consumer<List<io.swagger.v3.oas.models.media.Schema>> consumer) {
    List<Type> types = (List<Type>) schema.get(property);
    if (types != null && types.size() > 0) {
      List<io.swagger.v3.oas.models.media.Schema> schemas = types.stream()
          .map(Type::getClassName)
          .map(ctx::schema)
          .collect(Collectors.toList());
      consumer.accept(schemas);
    }
  }

  private static io.swagger.v3.oas.models.responses.ApiResponses apiResponses(
      Response... responses) {
    return apiResponses(Arrays.asList(responses));
  }

  private static io.swagger.v3.oas.models.responses.ApiResponses apiResponses(
      List<Response> responses) {
    io.swagger.v3.oas.models.responses.ApiResponses result = new io.swagger.v3.oas.models.responses.ApiResponses();
    responses.forEach(r -> result.addApiResponse(r.getCode(), r));
    return result;
  }
}
