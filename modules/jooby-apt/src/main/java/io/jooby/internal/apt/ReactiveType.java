/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.List;
import java.util.Set;

public class ReactiveType {

  private final String handlerType;
  private final String handler;
  private final Set<String> reactiveTypes;
  private final String mapOperator;

  private ReactiveType(
      String handlerType, String handler, Set<String> reactiveTypes, String mapOperator) {
    this.handlerType = handlerType;
    this.handler = handler;
    this.reactiveTypes = reactiveTypes;
    this.mapOperator = mapOperator;
  }

  public Set<String> reactiveTypes() {
    return reactiveTypes;
  }

  public String handlerType() {
    return handlerType;
  }

  public String handler() {
    return handler;
  }

  public String mapOperator() {
    return mapOperator;
  }

  public static List<ReactiveType> supportedTypes() {
    return List.of(
        new ReactiveType(
            "io.jooby.ReactiveSupport",
            "concurrent",
            Set.of("java.util.concurrent.Flow", "java.util.concurrent.CompletionStage"),
            ".thenApply(io.jooby.rpc.trpc.TrpcResponse::of)"),
        // Vertx
        new ReactiveType(
            "io.jooby.vertx.VertxHandler",
            "vertx",
            Set.of("io.vertx.core.Future", "io.vertx.core.Promise", "io.vertx.core.buffer.Buffer"),
            ".map(io.jooby.rpc.trpc.TrpcResponse::of)"),
        // Mutiny
        new ReactiveType(
            "io.jooby.mutiny.Mutiny",
            "mutiny",
            Set.of("io.smallrye.mutiny.Uni", "io.smallrye.mutiny.Multi"),
            ".map(io.jooby.rpc.trpc.TrpcResponse::of)"),
        // Reactor
        new ReactiveType(
            "io.jooby.reactor.Reactor",
            "reactor",
            Set.of("reactor.core.publisher.Flux", "reactor.core.publisher.Mono"),
            ".map(io.jooby.rpc.trpc.TrpcResponse::of)"),
        // Rxjava
        new ReactiveType(
            "io.jooby.rxjava3.Reactivex",
            "rx",
            Set.of(
                "io.reactivex.rxjava3.core.Flowable",
                "io.reactivex.rxjava3.core.Maybe",
                "io.reactivex.rxjava3.core.Single",
                "io.reactivex.rxjava3.core.Observable",
                "io.reactivex.rxjava3.core.Completable"),
            ".map(io.jooby.rpc.trpc.TrpcResponse::of)"));
  }
}
