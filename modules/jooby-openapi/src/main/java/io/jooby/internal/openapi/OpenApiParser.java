package io.jooby.internal.openapi;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jooby.internal.openapi.AsmUtils.arrayToMap;
import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class OpenApiParser {
  public static void parse(MethodNode method, Operation operation) {
    /** @Operation: */
    findAnnotationByType(method.visibleAnnotations,
        singletonList(io.swagger.v3.oas.annotations.Operation.class.getName())).stream()
        .findFirst()
        .ifPresent(a -> swaggerOperation(operation, arrayToMap(a.values)));

    /** @ApiResponses: */
    List<OperationResponse> responses = findAnnotationByType(method.visibleAnnotations,
        singletonList(ApiResponses.class.getName()))
        .stream()
        .flatMap(a -> (
                (List<AnnotationNode>) arrayToMap(a.values)
                    .getOrDefault("value", emptyList())
            ).stream()
        )
        .map(a -> arrayToMap(a.values))
        .map(a -> operationResponse(a))
        .collect(Collectors.toList());

    if (responses.isEmpty()) {
      /** @ApiResponse: */
      findAnnotationByType(method.visibleAnnotations, singletonList(ApiResponse.class.getName()))
          .stream()
          .findFirst()
          .map(a -> arrayToMap(a.values))
          .map(a -> operationResponse(a))
          .ifPresent(a -> operation.setResponse(Collections.singletonList(a)));
    } else {
      operation.setResponse(responses);
    }
  }

  private static <R> void swaggerOperation(Operation operation, Map<String, Object> annotation) {
    String operationId = (String) annotation.getOrDefault("operationId", "");
    if (operationId.trim().length() > 0) {
      operation.setId(operationId.trim());
    }
    Boolean deprecated = (Boolean) annotation.get("deprecated");
    if (deprecated == Boolean.TRUE) {
      operation.setDeprecated(deprecated.booleanValue());
    }
    Boolean hidden = (Boolean) annotation.getOrDefault("deprecated", false);
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
    tags.forEach(operation::addTag);

    List<OperationResponse> operationResponses = operationResponses(annotation);
    if (operationResponses.size() > 0) {
      operation.setResponse(operationResponses);
    }
  }

  private static List<OperationResponse> operationResponses(Map<String, Object> annotation) {
    List<AnnotationNode> responses = (List<AnnotationNode>) annotation
        .getOrDefault("responses", emptyList());
    if (responses.size() > 0) {
      // clear any detected response
      List<OperationResponse> returnTypes = responses.stream()
          .map(it -> arrayToMap(it.values))
          .map(it -> operationResponse(it))
          .collect(Collectors.toList());
      return returnTypes;
    }
    return Collections.emptyList();
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse)
  private static OperationResponse operationResponse(Map<String, Object> annotation) {
    String code = (String) annotation.getOrDefault("responseCode", "200");
    String description = (String) annotation.getOrDefault("description", "");

    List<String> javaTypes = operationResponseContent(annotation);

    OperationResponse response = new OperationResponse();
    if (javaTypes.size() > 0) {
      response.setJavaType(javaTypes.get(0));
    }
    if (description.trim().length() > 0) {
      response.setDescription(description.trim());
    }
    response.setCode(code);
    return response;
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse(content = @Content))
  private static List<String> operationResponseContent(Map<String, Object> annotation) {
    List<AnnotationNode> contents = (List<AnnotationNode>) annotation
        .getOrDefault("content", Collections.emptyList());
    return contents.stream()
        .map(it -> arrayToMap(it.values))
        .map(it -> responseContent(it))
        .collect(Collectors.toList());
  }

  @ApiResponse(
      content = @Content(array = @ArraySchema(
          schema = @Schema(implementation = String.class))
      )
  )
  private static String responseContent(Map<String, Object> apiResponse) {
    Map<String, Object> schema;
    String arrayType = null;
    AnnotationNode e = (AnnotationNode) apiResponse.get("array");
    if (e != null) {
      Map<String, Object> array = arrayToMap(e.values);
      Boolean unique = (Boolean) array.getOrDefault("uniqueItems", false);
      AnnotationNode s = (AnnotationNode) array.get("schema");
      schema = arrayToMap(s.values);
      arrayType = unique.booleanValue() ? Set.class.getName() : List.class.getName();
    } else {
      AnnotationNode s = (AnnotationNode) apiResponse.get("schema");
      schema = arrayToMap(s.values);
    }
    Type implementation = (Type) schema.get("implementation");
    if (arrayType != null) {
      return arrayType + "<" + implementation.getClassName() + ">";
    }
    return implementation.getClassName();
  }
}
