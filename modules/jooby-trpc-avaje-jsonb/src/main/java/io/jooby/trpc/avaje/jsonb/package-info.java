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
@org.jspecify.annotations.NullMarked
package io.jooby.trpc.avaje.jsonb;
