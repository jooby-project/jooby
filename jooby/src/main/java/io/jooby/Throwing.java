/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * Collection of throwable interfaces to simplify exception handling, specially on lambdas.
 *
 * We do provide throwable and 100% compatible implementation of {@link java.util.function.Function},
 * {@link java.util.function.Consumer}, {@link java.lang.Runnable},
 * {@link java.util.function.Supplier}, {@link java.util.function.Predicate} and
 * {@link java.util.function.BiPredicate}.
 *
 * Examples:
 *
 * <pre>{@code
 *
 *  interface Query {
 *    Item findById(String id) throws IOException;
 *  }
 *
 *  Query query = ...
 *
 *  List<Item> items = Arrays.asList("1", "2", "3")
 *    .stream()
 *    .map(throwingFunction(query::findById))
 *    .collect(Collectors.toList());
 *
 * }</pre>
 *
 *
 * @author edgar
 */
public class Throwing {
  /**
   * Throwable version of {@link Predicate}.
   *
   * @param <V> Input type.
   */
  public interface Predicate<V> extends java.util.function.Predicate<V> {
    boolean tryTest(V v) throws Exception;

    @Override default boolean test(V v) {
      try {
        return tryTest(v);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link Predicate}.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   */
  public interface Predicate2<V1, V2> extends java.util.function.BiPredicate<V1, V2> {
    boolean tryTest(V1 v1, V2 v2) throws Exception;

    @Override default boolean test(V1 v1, V2 v2) {
      try {
        return tryTest(v1, v2);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link java.lang.Runnable}.
   */
  @FunctionalInterface
  public interface Runnable extends java.lang.Runnable {
    void tryRun() throws Exception;

    @Override default void run() {
      try {
        tryRun();
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link java.util.function.Supplier}.
   *
   * @param <V> Result type.
   */
  @FunctionalInterface
  public interface Supplier<V> extends java.util.function.Supplier<V> {

    V tryGet() throws Exception;

    @Override default V get() {
      try {
        return tryGet();
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link java.util.function.Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V> Input type.
   */
  @FunctionalInterface
  public interface Consumer<V> extends java.util.function.Consumer<V> {
    /**
     * Performs this operation on the given argument.
     *
     * @param value Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V value) throws Exception;

    @Override default void accept(V v) {
      try {
        tryAccept(v);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Two argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
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
     * @throws Throwable If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2) throws Exception;

    default void accept(V1 v1, V2 v2) {
      try {
        tryAccept(v1, v2);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Three argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
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

    default void accept(V1 v1, V2 v2, V3 v3) {
      try {
        tryAccept(v1, v2, v3);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Four argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   */
  @FunctionalInterface
  public interface Consumer4<V1, V2, V3, V4> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3, V4 v4) throws Exception;

    default void accept(V1 v1, V2 v2, V3 v3, V4 v4) {
      try {
        tryAccept(v1, v2, v3, v4);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Five argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   */
  @FunctionalInterface
  public interface Consumer5<V1, V2, V3, V4, V5> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5) throws Exception;

    default void accept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5) {
      try {
        tryAccept(v1, v2, v3, v4, v5);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Six argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   */
  @FunctionalInterface
  public interface Consumer6<V1, V2, V3, V4, V5, V6> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6) throws Exception;

    /**
     * Performs this operation on the given arguments and throw any exception using {@link #sneakyThrow(Throwable)} method.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     */
    default void accept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6) {
      try {
        tryAccept(v1, v2, v3, v4, v5, v6);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Seven argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   * @param <V7> Input type.
   */
  @FunctionalInterface
  public interface Consumer7<V1, V2, V3, V4, V5, V6, V7> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     * @param v7 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7) throws Exception;

    /**
     * Performs this operation on the given arguments and throw any exception using {@link #sneakyThrow(Throwable)} method.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     * @param v7 Argument.
     */
    default void accept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7) {
      try {
        tryAccept(v1, v2, v3, v4, v5, v6, v7);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Seven argument version of {@link Consumer}.
   *
   * This class rethrow any exception using the {@link #sneakyThrow(Throwable)} technique.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   * @param <V7> Input type.
   * @param <V8> Input type.
   */
  @FunctionalInterface
  public interface Consumer8<V1, V2, V3, V4, V5, V6, V7, V8> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     * @param v7 Argument.
     * @param v8 Argument.
     * @throws Exception If something goes wrong.
     */
    void tryAccept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7, V8 v8) throws Exception;

    /**
     * Performs this operation on the given arguments and throw any exception using {@link #sneakyThrow(Throwable)} method.
     *
     * @param v1 Argument.
     * @param v2 Argument.
     * @param v3 Argument.
     * @param v4 Argument.
     * @param v5 Argument.
     * @param v6 Argument.
     * @param v7 Argument.
     * @param v8 Argument.
     */
    default void accept(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7, V8 v8) {
      try {
        tryAccept(v1, v2, v3, v4, v5, v6, v7, v8);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link java.util.function.Function}.
   *
   * The {@link #apply(Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
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
    @Override default R apply(V v) {
      try {
        return tryApply(v);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Throwable version of {@link java.util.function.BiFunction}.
   *
   * The {@link #apply(Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function2<V1, V2, R> extends java.util.function.BiFunction<V1, V2, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @return Result.
     */
    @Override default R apply(V1 v1, V2 v2) {
      try {
        return tryApply(v1, v2);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with three arguments.
   *
   * The {@link #apply(Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function3<V1, V2, V3, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3) {
      try {
        return tryApply(v1, v2, v3);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with four arguments.
   *
   * The {@link #apply(Object, Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function4<V1, V2, V3, V4, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3, V4 v4) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3, V4 v4) {
      try {
        return tryApply(v1, v2, v3, v4);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with five arguments.
   *
   * The {@link #apply(Object, Object, Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function5<V1, V2, V3, V4, V5, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5) {
      try {
        return tryApply(v1, v2, v3, v4, v5);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with six arguments.
   *
   * The {@link #apply(Object, Object, Object, Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function6<V1, V2, V3, V4, V5, V6, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6) {
      try {
        return tryApply(v1, v2, v3, v4, v5, v6);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with seven arguments.
   *
   * The {@link #apply(Object, Object, Object, Object, Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   * @param <V7> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function7<V1, V2, V3, V4, V5, V6, V7, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @param v7 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @param v7 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7) {
      try {
        return tryApply(v1, v2, v3, v4, v5, v6, v7);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  /**
   * Function with seven arguments.
   *
   * The {@link #apply(Object, Object, Object, Object, Object, Object, Object, Object)} method throws checked exceptions using {@link #sneakyThrow(Throwable)} method.
   *
   * @param <V1> Input type.
   * @param <V2> Input type.
   * @param <V3> Input type.
   * @param <V4> Input type.
   * @param <V5> Input type.
   * @param <V6> Input type.
   * @param <V7> Input type.
   * @param <V8> Input type.
   * @param <R> Output type.
   */
  @FunctionalInterface
  public interface Function8<V1, V2, V3, V4, V5, V6, V7, V8, R> {
    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @param v7 Input argument.
     * @param v8 Input argument.
     * @return Result.
     * @throws Exception If something goes wrong.
     */
    R tryApply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7, V8 v8) throws Exception;

    /**
     * Apply this function to the given argument and produces a result.
     *
     * @param v1 Input argument.
     * @param v2 Input argument.
     * @param v3 Input argument.
     * @param v4 Input argument.
     * @param v5 Input argument.
     * @param v6 Input argument.
     * @param v7 Input argument.
     * @param v8 Input argument.
     * @return Result.
     */
    default R apply(V1 v1, V2 v2, V3 v3, V4 v4, V5 v5, V6 v6, V7 v7, V8 v8) {
      try {
        return tryApply(v1, v2, v3, v4, v5, v6, v7, v8);
      } catch (Exception x) {
        throw sneakyThrow(x);
      }
    }
  }

  public final static <V> Predicate<V> throwingPredicate(Predicate<V> predicate) {
    return predicate;
  }

  public final static <V1, V2> Predicate2<V1, V2> throwingPredicate(
      Predicate2<V1, V2> predicate) {
    return predicate;
  }

  /**
   * Factory method for {@link Runnable}.
   *
   * @param action Runnable.
   * @return Same runnable.
   */
  public final static Runnable throwingRunnable(Runnable action) {
    return action;
  }

  /**
   * Factory method for {@link Supplier}.
   *
   * @param fn Supplier.
   * @param <V> Resulting value.
   * @return Same supplier.
   */
  public final static <V> Supplier<V> throwingSupplier(Supplier<V> fn) {
    return fn;
  }

  /**
   * Factory method for {@link Function} and {@link java.util.function.Function}.
   *
   * @param fn Function.
   * @param <V> Input value.
   * @param <R> Result value.
   * @return Same supplier.
   */
  public final static <V, R> Function<V, R> throwingFunction(Function<V, R> fn) {
    return fn;
  }

  /**
   * Factory method for {@link Function2} and {@link java.util.function.BiFunction}.
   *
   * @param fn Function.
   * @param <V1> Input value.
   * @param <V2> Input value.
   * @param <R> Result value.
   * @return Same supplier.
   */
  public final static <V1, V2, R> Function2<V1, V2, R> throwingFunction(Function2<V1, V2, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, R> Function3<V1, V2, V3, R> throwingFunction(
      Function3<V1, V2, V3, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, V4, R> Function4<V1, V2, V3, V4, R> throwingFunction(
      Function4<V1, V2, V3, V4, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, V4, V5, R> Function5<V1, V2, V3, V4, V5, R> throwingFunction(
      Function5<V1, V2, V3, V4, V5, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, V4, V5, V6, R> Function6<V1, V2, V3, V4, V5, V6, R> throwingFunction(
      Function6<V1, V2, V3, V4, V5, V6, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, V4, V5, V6, V7, R> Function7<V1, V2, V3, V4, V5, V6, V7, R> throwingFunction(
      Function7<V1, V2, V3, V4, V5, V6, V7, R> fn) {
    return fn;
  }

  public final static <V1, V2, V3, V4, V5, V6, V7, V8, R> Function8<V1, V2, V3, V4, V5, V6, V7, V8, R> throwingFunction(
      Function8<V1, V2, V3, V4, V5, V6, V7, V8, R> fn) {
    return fn;
  }

  public final static <V> Consumer<V> throwingConsumer(Consumer<V> action) {
    return action;
  }

  public final static <V1, V2> Consumer2<V1, V2> throwingConsumer(Consumer2<V1, V2> action) {
    return action;
  }

  public final static <V1, V2, V3> Consumer3<V1, V2, V3> throwingConsumer(
      Consumer3<V1, V2, V3> action) {
    return action;
  }

  public final static <V1, V2, V3, V4> Consumer4<V1, V2, V3, V4> throwingConsumer(
      Consumer4<V1, V2, V3, V4> action) {
    return action;
  }

  public final static <V1, V2, V3, V4, V5> Consumer5<V1, V2, V3, V4, V5> throwingConsumer(
      Consumer5<V1, V2, V3, V4, V5> action) {
    return action;
  }

  public final static <V1, V2, V3, V4, V5, V6> Consumer6<V1, V2, V3, V4, V5, V6> throwingConsumer(
      Consumer6<V1, V2, V3, V4, V5, V6> action) {
    return action;
  }

  public final static <V1, V2, V3, V4, V5, V6, V7> Consumer7<V1, V2, V3, V4, V5, V6, V7> throwingConsumer(
      Consumer7<V1, V2, V3, V4, V5, V6, V7> action) {
    return action;
  }

  public final static <V1, V2, V3, V4, V5, V6, V7, V8> Consumer8<V1, V2, V3, V4, V5, V6, V7, V8> throwingConsumer(
      Consumer8<V1, V2, V3, V4, V5, V6, V7, V8> action) {
    return action;
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it
   * onwards.
   * The exception is still thrown - javac will just stop whining about it.
   * <p>
   * Example usage:
   * <pre>public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }</pre>
   * <p>
   * NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does not
   * know or care
   * about the concept of a 'checked exception'. All this method does is hide the act of throwing a
   * checked exception from the java compiler.
   * <p>
   * Note that this method has a return type of {@code RuntimeException}; it is advised you always
   * call this
   * method as argument to the {@code throw} statement to avoid compiler errors regarding no return
   * statement and similar problems. This method won't of course return an actual
   * {@code RuntimeException} -
   * it never returns, it always throws the provided exception.
   *
   * @param x The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws
   *         an exception!
   */
  public static RuntimeException sneakyThrow(final Throwable x) {
    if (x == null) {
      throw new NullPointerException("x");
    }

    sneakyThrow0(x);
    return null;
  }

  /**
   * True if the given exception is one of {@link InterruptedException}, {@link LinkageError},
   * {@link ThreadDeath}, {@link VirtualMachineError}.
   *
   * @param x Exception to test.
   * @return True if the given exception is one of {@link InterruptedException}, {@link LinkageError},
   *     {@link ThreadDeath}, {@link VirtualMachineError}.
   */
  public static boolean isFatal(Throwable x) {
    return x instanceof InterruptedException ||
        x instanceof LinkageError ||
        x instanceof ThreadDeath ||
        x instanceof VirtualMachineError;
  }

  /**
   * Make a checked exception un-checked and rethrow it.
   *
   * @param x Exception to throw.
   * @param <E> Exception type.
   * @throws E Exception to throw.
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
