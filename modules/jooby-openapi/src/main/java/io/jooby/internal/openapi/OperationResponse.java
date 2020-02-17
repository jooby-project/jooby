package io.jooby.internal.openapi;

import io.jooby.StatusCode;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class OperationResponse {
  private static final Set<String> ASYNC_TYPES =
      Arrays.asList(
          CompletionStage.class.getName(), CompletableFuture.class.getName(),
          "io.reactivex.Single", "io.reactivex.Maybe", "io.reactivex.Flowable",
          "io.reactivex.Observable",
          "reactor.core.publisher.Flux", "reactor.core.publisher.Mono"
      ).stream()
          .collect(Collectors.toSet());

  private String javaType;

  private String code = "200";

  private String description;

  public OperationResponse(String javaType) {
    this.javaType = ASYNC_TYPES.stream()
        .filter(type -> javaType.startsWith(type))
        .findFirst()
        .map(type ->
            javaType.equals(type)
                ? "java.lang.Object"
                : javaType.substring(type.length() + 1, javaType.length() - 1)
        )
        .orElse(javaType);
  }

  public OperationResponse() {
  }

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getDescription() {
    if (description == null) {
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

}
