/**
 * Implementation of jooby-trpc using Jackson 3.x. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new Jackson3Module());
 *   install(new TrpcJackson3Module());
 *   install(new TrpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
module io.jooby.trpc.jackson3 {
  exports io.jooby.trpc.jackson3;

  requires io.jooby;
  requires io.jooby.trpc;
  requires static org.jspecify;
  requires typesafe.config;
  requires tools.jackson.core;
  requires tools.jackson.databind;
}
