/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.StatusCode;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.stream.Collectors.toSet;

public class ResponseExt extends ApiResponse {
  private static final Set<String> ASYNC_TYPES =
      Arrays.asList(
          CompletionStage.class.getName(), CompletableFuture.class.getName(),
          "io.reactivex.Single", "io.reactivex.Maybe", "io.reactivex.Flowable",
          "io.reactivex.Observable",
          "reactor.core.publisher.Flux", "reactor.core.publisher.Mono"
      ).stream().collect(toSet());

  @JsonIgnore
  private List<String> javaTypes = new ArrayList<>();

  @JsonIgnore
  private String code;

  public ResponseExt(String code) {
    this.code = code;
  }

  public ResponseExt() {
    this("200");
  }

  @JsonIgnore
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
    String description = super.getDescription();
    if (description == null) {
      if ("200".equals(code)) {
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
