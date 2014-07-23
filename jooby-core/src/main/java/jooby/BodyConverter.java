package jooby;

import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

/**
 * Read or write messages from/to HTTP Body. A body converter must provider a set of compatibles
 * media types. Media types are used during content negotiation of incoming HTTP request.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public interface BodyConverter {

  /**
   * Produces the list of compatibles media types. At least one must be provided.
   *
   * @return A list of of compatibles media types. At least one must be provided.
   */
  @Nonnull
  List<MediaType> types();

  /**
   * Test if the HTTP request body can be converted to the given type.
   *
   * @param type The candidate Type.
   * @return True if the converter can read the HTTP request body.
   */
  boolean canRead(@Nonnull Type type);

  boolean canWrite(@Nonnull Class<?> type);

  /**
   * Attempt to read a message from HTTP request body.
   * <p>
   * For text format (json, yaml, xml, etc.) a converter usually call to
   * {@link BodyReader#text(jooby.BodyReader.Text)} in order to set charset and close resources.
   * </p>
   *
   * <p>
   * For binary format a converter usually call to {@link BodyReader#bytes(jooby.BodyReader.Bytes)}
   * in order to close resources.
   * </p>
   *
   * @param type The type of message.
   * @param reader The reading context.
   * @return The body message.
   * @throws Exception If body can't be read it.
   */
  <T> T read(Class<T> type, BodyReader reader) throws Exception;

  /**
   * Attempt to write a message into the HTTP response body.
   *
   * <p>
   * For text format (json, yaml, xml, etc.) a converter usually call to
   * {@link BodyWriter#text(jooby.BodyWriter.Text)} in order to set charset and close resources.
   * </p>
   *
   * <p>
   * For binary format a converter usually call to {@link BodyWriter#bytes(jooby.BodyWriter.Binary)}
   * in order to close resources.
   * </p>
   *
   * @param message The body message.
   * @param writer The writing context.
   * @throws Exception If the body can't be write it.
   */
  void write(Object message, BodyWriter writer) throws Exception;

}
