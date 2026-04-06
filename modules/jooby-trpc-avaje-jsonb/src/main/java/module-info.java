/**
 * Implementation of jooby-trpc using avaje-jsonb. It provides the parser, decoder, reader, and
 * serializer.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *   install(new AvajeJsonbModule());
 *   install(new TrpcAvajeJsonbModule());
 *   install(new TrpcModule());
 * }
 * }</pre>
 *
 * @since 4.3.0
 * @author edgar
 */
module io.jooby.trpc.avaje.jsonb {
  exports io.jooby.trpc.avaje.jsonb;

  requires io.jooby;
  requires io.jooby.trpc;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires io.avaje.jsonb;

  provides io.avaje.jsonb.spi.JsonbComponent with
      io.jooby.trpc.avaje.jsonb.TrpcJsonbExtension;
}
