/**
 * Implementation of jooby-jsonrpc using avaje-jsonb. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new AvajeJsonbModule());
 *   install(new JsonRpcAvajeJsonbModule());
 *   install(new JsonRpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
module io.jooby.jsonrpc.avaje.jsonb {
  exports io.jooby.jsonrpc.avaje.jsonb;

  provides io.avaje.jsonb.spi.JsonbComponent with
      io.jooby.jsonrpc.avaje.jsonb.JsonRpcExtension;

  requires io.jooby;
  requires io.jooby.jsonrpc;
  requires static org.jspecify;
  requires typesafe.config;
  requires org.slf4j;
  requires io.avaje.jsonb;
}
