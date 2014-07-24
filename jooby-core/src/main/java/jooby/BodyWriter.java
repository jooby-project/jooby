package jooby;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

/**
 * Utility class to properly writing a HTTP response body. It provides methods for reading text and
 * bytes efficiently. Clients shouldn't worry about closing the HTTP response body.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface BodyWriter {

  /**
   * Write bytes to the HTTP Body.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Bytes {

    /**
     * Write bytes from the given {@link OutputStream}. The {@link OutputStream} will be close it
     * automatically after this call. Clients shouldn't worry about closing
     * the {@link OutputStream}.
     *
     * @param out The HTTP response body.
     * @throws Exception When the operation fails.
     */
    void write(OutputStream out) throws Exception;
  }

  /**
   * Write bytes to the HTTP Body.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Text {

    /**
     * Write bytes from the given {@link Writer}. The {@link Writer} will be close it automatically
     * after this call. Clients shouldn't worry about closing the {@link Writer}.
     *
     * @param writer The HTTP response body.
     * @throws Exception When the operation fails.
     */
    void write(Writer writer) throws Exception;
  }

  /**
   * @return A charset.
   */
  Charset charset();

  /**
   * Get/set a response header.
   *
   * @param name A header's name.
   * @return A response header.
   */
  @Nonnull HttpHeader header(@Nonnull String name);

  /**
   * Write text into the HTTP response body using the {@link #charset()} and close the resources.
   *
   * @param text A text strategy.
   * @throws Exception When the operation fails.
   */
  void text(@Nonnull Text text) throws Exception;

  /**
   * Write bytes into the HTTP response body and close the resources.
   *
   * @param bytes A bytes strategy.
   * @throws Exception When the operation fails.
   */
  void bytes(@Nonnull Bytes bytes) throws Exception;

}
