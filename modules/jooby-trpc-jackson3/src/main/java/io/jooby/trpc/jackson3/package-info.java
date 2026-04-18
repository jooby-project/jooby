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
@org.jspecify.annotations.NullMarked
package io.jooby.trpc.jackson3;
