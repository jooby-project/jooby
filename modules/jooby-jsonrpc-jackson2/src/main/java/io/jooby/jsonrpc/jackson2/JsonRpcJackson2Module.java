/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc.jackson2;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.jsonrpc.jackson2.JacksonJsonRpcParser;
import io.jooby.jsonrpc.JsonRpcErrorCode;
import io.jooby.jsonrpc.JsonRpcParser;

/**
 * Implementation of jooby-jsonrpc using Jackson 2.x. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new JacksonModule());
 *   install(new JsonRpcJackson2Module());
 *   install(new JsonRpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
public class JsonRpcJackson2Module implements Extension {
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    // JSON-RPC
    var services = application.getServices();
    services.put(
        JsonRpcParser.class, new JacksonJsonRpcParser(application.require(ObjectMapper.class)));
    services
        .mapOf(Class.class, JsonRpcErrorCode.class)
        .put(MismatchedInputException.class, JsonRpcErrorCode.INVALID_PARAMS)
        .put(DatabindException.class, JsonRpcErrorCode.INVALID_PARAMS);
  }
}
