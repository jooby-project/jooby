/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.transport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Context;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractMcpTransportProvider implements McpServerTransportProvider {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final McpJsonMapper mcpJsonMapper;
  protected final McpTransportContextExtractor<Context> contextExtractor;
  protected final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
  protected final AtomicBoolean isClosing = new AtomicBoolean(false);
  protected McpServerSession.Factory sessionFactory;

  public AbstractMcpTransportProvider(
      McpJsonMapper mcpJsonMapper, McpTransportContextExtractor<Context> contextExtractor) {
    this.mcpJsonMapper = mcpJsonMapper;
    this.contextExtractor = contextExtractor;
  }

  protected abstract String transportName();

  @Override
  public void setSessionFactory(McpServerSession.Factory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mono<Void> notifyClients(String method, Object params) {
    if (sessions.isEmpty()) {
      log.debug("No active {} sessions to broadcast a message to", transportName());
      return Mono.empty();
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Attempting to broadcast to {} active {} sessions", sessions.size(), transportName());
    }

    return Flux.fromIterable(sessions.values())
        .flatMap(
            session ->
                session
                    .sendNotification(method, params)
                    .doOnError(
                        e ->
                            log.error(
                                "Failed to send message to {} session {}: {}",
                                transportName(),
                                session.getId(),
                                e.getMessage()))
                    .onErrorComplete())
        .then();
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Flux.fromIterable(sessions.values())
        .doFirst(
            () -> {
              isClosing.set(true);
              if (log.isDebugEnabled()) {
                log.debug(
                    "Initiating graceful shutdown for {} {} sessions",
                    sessions.size(),
                    transportName());
              }
            })
        .flatMap(McpServerSession::closeGracefully)
        .doFinally(signalType -> sessions.clear())
        .then();
  }
}
