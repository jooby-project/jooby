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
module io.jooby.jsonrpc.jackson3 {
  exports io.jooby.jsonrpc.jackson3;

  provides tools.jackson.databind.JacksonModule with
      io.jooby.internal.jsonrpc.jackson3.JacksonJsonRpcModule;

  requires io.jooby;
  requires io.jooby.jsonrpc;
  requires static org.jspecify;
  requires typesafe.config;
  requires tools.jackson.databind;
}
