package jooby;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

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

  Charset charset();

  HttpMutableField header(String name);

  void text(Text text) throws Exception;

  void bytes(Bytes bytes) throws Exception;

}
