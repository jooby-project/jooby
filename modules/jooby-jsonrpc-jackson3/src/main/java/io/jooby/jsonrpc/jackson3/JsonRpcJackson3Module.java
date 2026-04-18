/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.jackson3;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.jsonrpc.jackson3.JacksonJsonRpcParser;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Implementation of jooby-jsonrpc using Jackson 3.x. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new Jackson3Module());
 *   install(new JsonRpcJackson3Module());
 *   install(new JsonRpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
public class JsonRpcJackson3Module implements Extension {
  @Override
  public void install(Jooby application) throws Exception {
    // JSON-RPC
    var services = application.getServices();
    services.put(
        JsonRpcParser.class, new JacksonJsonRpcParser(application.require(ObjectMapper.class)));
    services
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(StreamReadException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(MismatchedInputException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(DatabindException.class, JsonRpcErrorCode.INVALID_PARAMS);
  }
}
