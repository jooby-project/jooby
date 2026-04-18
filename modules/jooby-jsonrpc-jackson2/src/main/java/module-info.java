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
module io.jooby.jsonrpc.jackson2 {
  exports io.jooby.jsonrpc.jackson2;

  provides com.fasterxml.jackson.databind.Module with
      io.jooby.internal.jsonrpc.jackson2.JacksonJsonRpcModule;

  requires io.jooby;
  requires io.jooby.jsonrpc;
  requires static org.jspecify;
  requires typesafe.config;
  requires com.fasterxml.jackson.databind;
}
