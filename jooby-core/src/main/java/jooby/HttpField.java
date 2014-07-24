package jooby;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;
import com.google.inject.TypeLiteral;

/**
 * <p>
 * A type safe {@link HttpField} useful for reading parameters and headers. It let you retrieve a
 * HTTP value: <code>param</code> or <code>header</code> in a type safe manner.
 * </p>
 *
 * <pre>
 *   // int param
 *   int value = request.param("some").getInt();
 *
 *   // optional int param
 *   Optional<Integer> value = request.param("some").getOptional(Integer.class);

 *   // list param
 *   List<String> values = request.param("some").getList(String.class);
 *
 *   // file upload
 *   Upload upload = request.param("file").get(Upload.class);
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 * @see Request#param(String)
 * @see Request#header(String)
 */
@Beta
public interface HttpField {

  /**
   * @return Get a boolean when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  boolean getBoolean() throws Exception;

  /**
   * @return Get a byte when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  byte getByte() throws Exception;

  /**
   * @return Get a short when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  short getShort() throws Exception;

  /**
   * @return Get an int when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  int getInt() throws Exception;

  /**
   * @return Get a long when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  long getLong() throws Exception;

  /**
   * @return Get a string when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  String getString() throws Exception;

  /**
   * @return Get a float when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  float getFloat() throws Exception;

  /**
   * @return Get a double when possible.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  double getDouble() throws Exception;

  /**
   * @return Get an enum when possible.
   * @param type The enum type.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T extends Enum<T>> T getEnum(@Nonnull Class<T> type) throws Exception;

  /**
   * @return Get list of values when possible.
   * @param type The element type.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T> List<T> getList(@Nonnull Class<T> type) throws Exception;

  /**
   * @return Get set of values when possible.
   * @param type The element type.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T> Set<T> getSet(@Nonnull Class<T> type) throws Exception;

  /**
   * @return Get sorted set of values when possible.
   * @param type The element type.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T extends Comparable<T>> SortedSet<T> getSortedSet(@Nonnull Class<T> type) throws Exception;

  /**
   * @return Get an optional value when possible.
   * @param type The optional type.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T> Optional<T> getOptional(@Nonnull Class<T> type) throws Exception;

  /**
   * Get a value using one of the existing and specific converters or an arbitrary type that has:
   *
   * <ol>
   * <li>A constructor that accepts a {@link String}</li>
   * <li>A static method <code>valueOf</code> that takes a single {@link String} parameter.</li>
   * <li>A static method <code>fromString</code> that takes a single {@link String} parameter.</li>
   * </ol>
   *
   * @return Get a value when possible.
   * @param type The type to convert to.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  default <T> T get(final Class<T> type) throws Exception {
    return get(TypeLiteral.get(type));
  }

  /**
   * Get a value using one of the existing and specific converters or an arbitrary type that has:
   *
   * <ol>
   * <li>A constructor that accepts a {@link String}</li>
   * <li>A static method <code>valueOf</code> that takes a single {@link String} parameter.</li>
   * <li>A static method <code>fromString</code> that takes a single {@link String} parameter.</li>
   * </ol>
   *
   * @return Get a value when possible.
   * @param type The type to convert to.
   * @throws Exception A {@link HttpException} with a {@link HttpStatus#BAD_REQUEST} status code.
   */
  <T> T get(TypeLiteral<T> type) throws Exception;
}
