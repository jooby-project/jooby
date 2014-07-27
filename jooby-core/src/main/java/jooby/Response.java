package jooby;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

/**
 * Give you access to the actual HTTP response. You can read/write headers and write HTTP body.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface Response {

  /**
   * Handling content negotiation for inline-routes. For example:
   *
   * <pre>
   *  {{
   *      get("/", (req, resp) -> {
   *        Object model = ...;
   *        resp.when("text/html", () -> Viewable.of("view", model))
   *            .when("application/json", () -> model)
   *            .send();
   *      });
   *  }}
   * </pre>
   *
   * The example above will render a view when accept header is "text/html" or just send a text
   * version of model when the accept header is "application/json".
   *
   * @author edgar
   * @since 0.1.0
   */
  interface ContentNegotiation {

    /**
     * Provider capable of throwing an exception.
     *
     * @author edgar
     * @since 0.1.0
     */
    interface Provider {

      /**
       * Apply the provider and return a non-null value.
       *
       * @return Object result.
       * @throws Exception If something fails.
       */
      @Nonnull
      Object apply() throws Exception;
    }

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param provider An object provider.
     * @return A {@link ContentNegotiation}.
     */
    @Nonnull
    default ContentNegotiation when(final String mediaType, final Provider provider) {
      return when(MediaType.valueOf(mediaType), provider);
    }

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param provider An object provider.
     * @return A {@link ContentNegotiation}.
     */
    @Nonnull
    ContentNegotiation when(MediaType mediaType, Provider fn);

    /**
     * Write the response and send it.
     *
     * @throws Exception If something fails.
     */
    void send() throws Exception;
  }

  /**
   * Get a header with the given name.
   *
   * @param name A name.
   * @return A HTTP header.
   */
  @Nonnull
  HttpHeader header(@Nonnull String name);

  /**
   * @return Charset for text responses.
   */
  @Nonnull
  Charset charset();

  /**
   * Set the {@link Charset} to use.
   *
   * @param charset A charset.
   * @return This response.
   */
  @Nonnull
  Response charset(@Nonnull Charset charset);

  /**
   * @return Get the response type.
   */
  Optional<MediaType> type();

  /**
   * Set the response media type.
   *
   * @param type A media type.
   * @return This response.
   */
  @Nonnull
  Response type(@Nonnull MediaType type);

  /**
   * Responsible of writing the given body into the HTTP response. The {@link BodyConverter} that
   * best matches the <code>Accept</code> header will be selected for writing the response.
   *
   * @param body The HTTP body.
   * @throws Exception If the response write fails.
   * @see BodyConverter
   */
  void send(@Nonnull Object body) throws Exception;

  /**
   * Responsible of writing the given body into the HTTP response.
   *
   * @param body The HTTP body.
   * @param converter The convert to use.
   * @throws Exception If the response write fails.
   * @see BodyConverter
   */
  void send(@Nonnull Object body, @Nonnull BodyConverter converter) throws Exception;

  /**
   * Add a new when clause for a custom media-type.
   *
   * @param mediaType A media type to test for.
   * @param provider An object provider.
   * @return A {@link ContentNegotiation}.
   */
  @Nonnull ContentNegotiation when(String type, ContentNegotiation.Provider provider);

  /**
   * Add a new when clause for a custom media-type.
   *
   * @param mediaType A media type to test for.
   * @param provider An object provider.
   * @return A {@link ContentNegotiation}.
   */
  @Nonnull ContentNegotiation when(MediaType type, ContentNegotiation.Provider provider);

  /**
   * @return A HTTP status.
   */
  @Nonnull HttpStatus status();

  /**
   * Set the HTTP status.
   *
   * @param status A HTTP status.
   * @return This response.
   */
  @Nonnull Response status(@Nonnull HttpStatus status);

}
