/**
 * Implementation of jooby-trpc using Jackson 2.x. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new JacksonModule());
 *   install(new TrpcJackson2Module());
 *   install(new TrpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
module io.jooby.trpc.jackson2 {
  exports io.jooby.trpc.jackson2;

  requires io.jooby;
  requires io.jooby.trpc;
  requires static org.jspecify;
  requires typesafe.config;
  requires com.fasterxml.jackson.databind;
}
