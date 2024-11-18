/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.exception.MissingValueException;

/**
 * Unified API for HTTP value. This API plays two role:
 *
 * <p>- unify access to HTTP values, like query, path, form and header parameter - works as
 * validation API, because it is able to check for required and type-safe values
 *
 * <p>The value API is composed by three types:
 *
 * <p>- Single value - Object value - Sequence of values (array)
 *
 * <p>Single value are can be converted to string, int, boolean, enum like values. Object value is a
 * key:value structure (like a hash). Sequence of values are index based structure.
 *
 * <p>All these 3 types are modeled into a single Value class. At any time you can treat a value as
 * 1) single, 2) hash or 3) array of them.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface ValueNode extends Iterable<ValueNode>, Value {

  /**
   * Get a value at the given position.
   *
   * @param index Position.
   * @return A value at the given position.
   */
  @NonNull ValueNode get(int index);

  /**
   * Get a value that matches the given name.
   *
   * @param name Field name.
   * @return Field value.
   */
  @NonNull ValueNode get(@NonNull String name);

  /**
   * The number of values this one has. For single values size is <code>0</code>.
   *
   * @return Number of values. Mainly for array and hash values.
   */
  default int size() {
    return 0;
  }

  /**
   * Value iterator.
   *
   * @return Value iterator.
   */
  @NonNull default @Override Iterator<ValueNode> iterator() {
    return Collections.emptyIterator();
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("${foo}");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @return Resolved text.
   */
  @NonNull default String resolve(@NonNull String expression) {
    return resolve(expression, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("${missing}", true);
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @return Resolved text.
   */
  @NonNull default String resolve(@NonNull String expression, boolean ignoreMissing) {
    return resolve(expression, ignoreMissing, "${", "}");
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("<%missing%>", "<%", "%>");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  @NonNull default String resolve(
      @NonNull String expression, @NonNull String startDelim, @NonNull String endDelim) {
    return resolve(expression, false, startDelim, endDelim);
  }

  /**
   * Process the given expression and resolve value references.
   *
   * <pre>{@code
   * Value value = Value.single("foo", "bar");
   *
   * String output = value.resolve("<%missing%>", "<%", "%>");
   * System.out.println(output);
   * }</pre>
   *
   * @param expression Text expression.
   * @param ignoreMissing On missing values, keep the expression as it is.
   * @param startDelim Start delimiter.
   * @param endDelim End delimiter.
   * @return Resolved text.
   */
  @NonNull default String resolve(
      @NonNull String expression,
      boolean ignoreMissing,
      @NonNull String startDelim,
      @NonNull String endDelim) {
    if (expression.length() == 0) {
      return "";
    }

    BiFunction<Integer, BiFunction<Integer, Integer, RuntimeException>, RuntimeException> err =
        (start, ex) -> {
          String snapshot = expression.substring(0, start);
          int line = Math.max(1, (int) snapshot.chars().filter(ch -> ch == '\n').count());
          int column = start - snapshot.lastIndexOf('\n');
          return ex.apply(line, column);
        };

    StringBuilder buffer = new StringBuilder();
    int offset = 0;
    int start = expression.indexOf(startDelim);
    while (start >= 0) {
      int end = expression.indexOf(endDelim, start + startDelim.length());
      if (end == -1) {
        throw err.apply(
            start,
            (line, column) ->
                new IllegalArgumentException(
                    "found '"
                        + startDelim
                        + "' expecting '"
                        + endDelim
                        + "' at "
                        + line
                        + ":"
                        + column));
      }
      buffer.append(expression.substring(offset, start));
      String key = expression.substring(start + startDelim.length(), end);
      String value;
      try {
        // seek
        String[] path = key.split("\\.");
        ValueNode src = path[0].equals(name()) ? this : get(path[0]);
        for (int i = 1; i < path.length; i++) {
          src = src.get(path[i]);
        }
        value = src.value();
      } catch (MissingValueException x) {
        if (ignoreMissing) {
          value = expression.substring(start, end + endDelim.length());
        } else {
          throw err.apply(
              start,
              (line, column) ->
                  new NoSuchElementException(
                      "Missing " + startDelim + key + endDelim + " at " + line + ":" + column));
        }
      }
      buffer.append(value);
      offset = end + endDelim.length();
      start = expression.indexOf(startDelim, offset);
    }
    if (buffer.length() == 0) {
      return expression;
    }
    if (offset < expression.length()) {
      buffer.append(expression.substring(offset));
    }
    return buffer.toString();
  }
}
