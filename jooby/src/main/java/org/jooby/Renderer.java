package org.jooby;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

public interface Renderer {

  /**
   * Write bytes to the HTTP Body.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Bytes {

    /**
     * Write bytes into the given {@link OutputStream}. The {@link OutputStream} will be close it
     * automatically after this call. Clients shouldn't worry about closing
     * the {@link OutputStream}.
     *
     * @param out The HTTP response body.
     * @throws Exception When the operation fails.
     */
    void write(OutputStream out) throws Exception;
  }

  /**
   * Write text to the HTTP Body and apply application/request charset.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Text {

    /**
     * Write text into the given {@link java.io.Writer}. The {@link java.io.Writer} will be
     * close it automatically after this call. Clients shouldn't worry about closing the
     * {@link Context}. The writer is configured with the application/request charset.
     *
     * @param writer The HTTP response body.
     * @throws Exception When the operation fails.
     */
    void write(java.io.Writer writer) throws Exception;
  }

  interface Context {

    Map<String, Object> locals();

    default boolean accepts(final String type) {
      return accepts(MediaType.valueOf(type));
    }

    default boolean accepts(final MediaType type) {
      return accepts(ImmutableList.of(type));
    }

    boolean accepts(List<MediaType> types);

    /**
     * Set the <code>Content-Type</code> header IF and ONLY IF, no <code>Content-Type</code> was set
     * yet.
     *
     * @param type A suggested type to use if one is missing.
     * @return This context.
     */
    Context type(MediaType type);

    /**
     * Set the <code>Content-Length</code> header IF and ONLY IF, no <code>Content-Length</code>
     * was set yet.
     *
     * @param length A suggested length to use if one is missing.
     * @return This context.
     */
    Context length(long length);

    Charset charset();

    /**
     * Write text into the HTTP response body using the {@link #charset()} and close the resources.
     * It applies the application/request charset.
     *
     * @param text A text strategy.
     * @throws Exception When the operation fails.
     */
    void text(Text text) throws Exception;

    /**
     * Write bytes into the HTTP response body and close the resources.
     *
     * @param bytes A bytes strategy.
     * @throws Exception When the operation fails.
     */
    void bytes(Bytes bytes) throws Exception;

  }

  void render(Object object, Context ctx) throws Exception;
}
