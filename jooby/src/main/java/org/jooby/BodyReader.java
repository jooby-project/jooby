package org.jooby;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

/**
 * Utility class to properly reading a HTTP request body or parameters. It provides methods for
 * reading text and bytes efficiently.
 * Clients shouldn't worry about closing the HTTP request body.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface BodyReader {

  /**
   * Read bytes from HTTP Body.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Bytes {

    /**
     * Read bytes from the given {@link InputStream}. The {@link InputStream} will be close it
     * automatically after this call. Clients shouldn't worry about closing the
     * {@link InputStream}.
     *
     * @param in The HTTP request body.
     * @return A body result (usually) converted to something else.
     * @throws Exception When the operation fails.
     */
    Object read(InputStream in) throws Exception;
  }

  /**
   * Read text from HTTP body.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Text {

    /**
     * Read text from the given {@link Reader}. The {@link Reader} will be close it automatically
     * after this call. Clients shouldn't worry about closing the {@link Reader}.
     *
     * @param reader The HTTP request body.
     * @return A body result (usually) converted to something else.
     * @throws Exception When the operation fails.
     */
    Object read(Reader reader) throws Exception;

  }

  /**
   * Convert a HTTP request body to something else. The body must be read it using the request
   * {@link Charset}.
   *
   * @param text A text reading strategy.
   * @return A HTTP body converted to something else.
   * @throws Exception When the operation fails.
   */
  @Nonnull <T> T text(@Nonnull Text text) throws Exception;

  /**
   * Convert a HTTP request body to something else.
   *
   * @param bytes A bytes reading strategy.
   * @return A HTTP body converted to something else.
   * @throws Exception When the operation fails.
   */
  @Nonnull <T> T bytes(@Nonnull Bytes bytes) throws Exception;

}
