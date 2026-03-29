/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mcp.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpServerTransport;
import reactor.core.publisher.Mono;

public abstract class AbstractMcpTransport implements McpServerTransport {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final McpJsonMapper mcpJsonMapper;

  public AbstractMcpTransport(McpJsonMapper mcpJsonMapper) {
    this.mcpJsonMapper = mcpJsonMapper;
  }

  @Override
  public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
    return mcpJsonMapper.convertValue(data, typeRef);
  }

  @Override
  public Mono<Void> closeGracefully() {
    return Mono.fromRunnable(this::close);
  }
}
