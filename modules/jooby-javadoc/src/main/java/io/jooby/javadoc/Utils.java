/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.javadoc;

public class Utils {
  /**
   * Two argument version of {@link java.util.function.BiConsumer}.
   *
   * <p>This class rethrow any exception using the {@link #propagate(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   */
  @FunctionalInterface
  public interface Consumer2<V1, V2> extends java.util.function.BiConsumer<V1, V2> {
    /**
     * Performs this operation on the given argument.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2) throws Exception;

    /**
     * Performs this operation on the given argument.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     */
    default void accept(V1 v1, V2 v2) {
      try {
        tryAccept(v1, v2);
      } catch (Exception x) {
        throw propagate(x);
      }
    }
  }

  /**
   * Three argument version of {@link java.util.function.Consumer}.
   *
   * <p>This class rethrow any exception using the {@link #propagate(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   */
  @FunctionalInterface
  public interface Consumer3<V1, V2, V3> {
    /**
     * Performs this operation on the given argument.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3) throws Exception;

    /**
     * Performs this operation on the given arguments and throw any exception using {@link
     * #propagate(Throwable)} method.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     */
    default void accept(V1 v1, V2 v2, V3 v3) {
      try {
        tryAccept(v1, v2, v3);
      } catch (Exception x) {
        throw propagate(x);
      }
    }
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it
   * onwards. The exception is still thrown - javac will just stop whining about it.
   *
   * <p>Example usage:
   *
   * <pre>public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }</pre>
   *
   * <p>NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does
   * not know or care about the concept of a 'checked exception'. All this method does is hide the
   * act of throwing a checked exception from the java compiler.
   *
   * <p>Note that this method has a return type of {@code RuntimeException}; it is advised you
   * always call this method as argument to the {@code throw} statement to avoid compiler errors
   * regarding no return statement and similar problems. This method won't of course return an
   * actual {@code RuntimeException} - it never returns, it always throws the provided exception.
   *
   * @param x The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws
   *     an exception!
   */
  public static RuntimeException propagate(final Throwable x) {
    if (x == null) {
      throw new NullPointerException("x");
    }

    throw sneakyThrow0(x);
  }

  /**
   * Make a checked exception un-checked and rethrow it.
   *
   * @param x Exception to throw.
   * @param <E> Exception type.
   * @throws E Exception to throw.
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> E sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }

  /**
   * Throwable version of {@link java.util.function.Function}.
   *
   * <p>The {@link #apply(Object)} method throws checked exceptions using {@link
   * #propagate(Throwable)} method.
   *
   * @param <V> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function<V, R> extends java.util.function.Function<V, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param value Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V value) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v Input argument.
     * @return Result.
     */
    @Override
    default R apply(V v) {
      try {
        return tryApply(v);
      } catch (Exception x) {
        throw propagate(x);
      }
    }
  }

  /**
   * Factory method for {@link Function} and {@link java.util.function.Function}.
   *
   * @param fn Function.
   * @param <V> Input value.
   * @param <R> Result value.
   * @return Same supplier.
   */
  public static <V, R> Function<V, R> throwingFunction(Function<V, R> fn) {
    return fn;
  }
}
