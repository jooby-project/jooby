package io.jooby.internal.openapi;

import io.jooby.StatusCode;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.stream.Collectors.toSet;

public class Response extends ApiResponse {
  private static final Set<String> ASYNC_TYPES =
      Arrays.asList(
          CompletionStage.class.getName(), CompletableFuture.class.getName(),
          "io.reactivex.Single", "io.reactivex.Maybe", "io.reactivex.Flowable",
          "io.reactivex.Observable",
          "reactor.core.publisher.Flux", "reactor.core.publisher.Mono"
      ).stream().collect(toSet());

  private List<String> javaTypes = new ArrayList<>();

  private String code = "default";

  private String description;

  public String getJavaType() {
    return javaTypes.isEmpty() ? null : javaTypes.get(0);
  }

  public List<String> getJavaTypes() {
    return javaTypes;
  }

  public void setJavaTypes(List<String> javaTypes) {
    this.javaTypes = new ArrayList<>();
    if (javaTypes != null) {
      for (String javaType : javaTypes) {
        this.javaTypes.add(unwrapType(javaType));
      }
    }
  }

  public String getDescription() {
    if (description == null) {
      if ("default".equals(code)) {
        return StatusCode.OK.reason();
      }
      try {
        StatusCode statusCode = StatusCode.valueOf(Integer.parseInt(code));
        String reason = statusCode.reason();
        return reason.equals(code) ? null : reason;
      } catch (NumberFormatException x) {
        return null;
      }
    }
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override public String toString() {
    return getJavaType();
  }

  private String unwrapType(String javaType) {
    return ASYNC_TYPES.stream()
        .filter(type -> javaType.startsWith(type))
        .findFirst()
        .map(type ->
            javaType.equals(type)
                ? "java.lang.Object"
                : javaType.substring(type.length() + 1, javaType.length() - 1)
        )
        .orElse(javaType);
  }
}
