package jooby;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import com.google.inject.TypeLiteral;

/**
 * A type safe {@link HttpField} useful for reading parameters and headers.
 *
 * @author edgar
 * @see Request#param(String)
 * @see Request#header(String)
 */
public interface HttpField {

  boolean getBoolean() throws Exception;

  byte getByte() throws Exception;

  short getShort() throws Exception;

  int getInt() throws Exception;

  long getLong() throws Exception;

  String getString() throws Exception;

  float getFloat() throws Exception;

  double getDouble() throws Exception;

  <T extends Enum<T>> T getEnum(Class<T> type) throws Exception;

  <T> List<T> getList(Class<T> type) throws Exception;

  <T> Set<T> getSet(Class<T> type) throws Exception;

  <T extends Comparable<T>> SortedSet<T> getSortedSet(Class<T> type) throws Exception;

  <T> Optional<T> getOptional(Class<T> type) throws Exception;

  default <T> T get(final Class<T> type) throws Exception {
    return get(TypeLiteral.get(type));
  }

  <T> T get(TypeLiteral<T> type) throws Exception;
}
