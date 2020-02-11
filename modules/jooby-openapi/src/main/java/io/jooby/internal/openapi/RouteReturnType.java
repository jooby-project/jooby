package io.jooby.internal.openapi;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class RouteReturnType {
  private static final Set<String> ASYNC_TYPES =
      Arrays.asList(
          CompletionStage.class.getName(), CompletableFuture.class.getName(),
          "io.reactivex.Single", "io.reactivex.Maybe", "io.reactivex.Flowable",
          "io.reactivex.Observable",
          "reactor.core.publisher.Flux", "reactor.core.publisher.Mono"
      ).stream()
          .collect(Collectors.toSet());

  private String javaType;

  private String overrideJavaType;

  public RouteReturnType(String javaType) {
    this.javaType = javaType;
    this.overrideJavaType = ASYNC_TYPES.stream()
        .filter(type -> javaType.startsWith(type))
        .findFirst()
        .map(type ->
            javaType.equals(type)
                ? "java.lang.Object"
                : javaType.substring(type.length() + 1, javaType.length() - 1)
        )
        .orElse(null);
  }

  public String getJavaType() {
    return javaType;
  }

  public String getOverrideJavaType() {
    return overrideJavaType;
  }

  public void setOverrideJavaType(String overrideJavaType) {
    this.overrideJavaType = overrideJavaType;
  }

  @Override public String toString() {
    return getJavaType();
  }
}
