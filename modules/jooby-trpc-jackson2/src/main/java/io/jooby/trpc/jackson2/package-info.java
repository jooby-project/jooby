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
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.trpc.jackson2;
