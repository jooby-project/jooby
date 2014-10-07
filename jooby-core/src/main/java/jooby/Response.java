package jooby;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import jooby.fn.ExSupplier;

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
  interface Formatter {

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param supplier An object provider.
     * @return The current {@link Formatter}.
     */
    @Nonnull
    default Formatter when(final String mediaType, final ExSupplier<Object> supplier) {
      return when(MediaType.valueOf(mediaType), supplier);
    }

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param provider An object provider.
     * @return A {@link Formatter}.
     */
    @Nonnull
    Formatter when(MediaType mediaType, ExSupplier<Object> supplier);

    /**
     * Write the response and send it.
     *
     * @throws Exception If something fails.
     */
    void send() throws Exception;
  }

  void download(String filename, InputStream stream) throws Exception;

  void download(String filename, Reader reader) throws Exception;

  default void download(final String filename) throws Exception {
    download(filename, getClass().getResourceAsStream(filename));
  }

  default void download(final File file) throws Exception {
    download(file.getName(), new FileInputStream(file));
  }

  default Response cookie(final String name, final String value) {
    return cookie(new SetCookie(name, value));
  }

  Response cookie(SetCookie cookie);

  Response clearCookie(String name);

  /**
   * Get a header with the given name.
   *
   * @param name A name.
   * @return A HTTP header.
   */
  @Nonnull
  Variant header(@Nonnull String name);

  Response header(@Nonnull String name, char value);

  Response header(@Nonnull String name, byte value);

  Response header(@Nonnull String name, short value);

  Response header(@Nonnull String name, int value);

  Response header(@Nonnull String name, long value);

  Response header(@Nonnull String name, float value);

  Response header(@Nonnull String name, double value);

  Response header(@Nonnull String name, String value);

  Response header(@Nonnull String name, Date value);

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

  @Nonnull Formatter format();

  default void redirect(final String location) throws Exception {
    redirect(HttpStatus.FOUND, location);
  }

  void redirect(HttpStatus status, String location) throws Exception;

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

  boolean committed();

  Map<String, Object> locals();

  <T> T local(String name);

  Response local(String name, Object value);

}
