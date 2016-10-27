/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * <h1>async request processing</h1>
 * <p>
 * A Deferred result, useful for async request processing.
 * </p>
 * <p>
 * Application can produces a result from a different thread. Once result is ready, a call to
 * {@link #resolve(Object)} is required. Please note, a call to {@link #reject(Throwable)} is
 * required in case of errors.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *    get("/async", deferred(() {@literal ->} {
 *      return "Success";
 *    }));
 *  }
 * </pre>
 *
 * From MVC route:
 *
 * <pre>{@code
 *
 *  public class Controller {
 *    &#64;GET
 *    &#64;Path("/async")
 *    public Deferred async() {
 *      return Deferred.deferred(() -> "Success");
 *    }
 *  }
 * }</pre>
 *
 * If you add the {@link AsyncMapper} then your controller method can return a {@link Callable}.
 *
 * Previous example runs in the default executor, which always run deferred results in the
 * same/caller thread.
 *
 * To effectively run a deferred result in new/different thread you need to provide an
 * {@link Executor}:
 *
 * <pre>{@code
 * {
 *   executor(new ForkJoinPool());
 * }
 * }</pre>
 *
 * This line override the default executor with a {@link ForkJoinPool}. You can add two or more
 * named executor:
 *
 * <pre>{@code
 * {
 *   executor(new ForkJoinPool());
 *
 *   executor("cached", Executors.newCachedExecutor());
 *
 *   get("/async", deferred("cached", () -> "Success"));
 * }
 * }</pre>
 *
 * A {@link Deferred} object works as a promise too, given you {@link #resolve(Object)} and
 * {@link #reject(Throwable)} methods. Examples:
 *
 * As promise using the default executor (execute promise in same/caller thread):
 * <pre>
 * {
 *    get("/async", promise(deferred {@literal ->} {
 *      try {
 *        deferred.resolve(...); // success value
 *      } catch (Throwable ex) {
 *        deferred.reject(ex); // error value
 *      }
 *    }));
 *  }
 * </pre>
 *
 * As promise using a custom executor:
 * <pre>
 * {
 *    executor(new ForkJoinPool());
 *
 *    get("/async", promise(deferred {@literal ->} {
 *      try {
 *        deferred.resolve(...); // success value
 *      } catch (Throwable ex) {
 *        deferred.reject(ex); // error value
 *      }
 *    }));
 *  }
 * </pre>
 *
 * As promise using an alternative executor:
 *
 * <pre>
 * {
 *    executor(new ForkJoinPool());
 *
 *    executor("cached", Executors.newCachedExecutor());
 *
 *    get("/async", promise("cached", deferred {@literal ->} {
 *      try {
 *        deferred.resolve(...); // success value
 *      } catch (Throwable ex) {
 *        deferred.reject(ex); // error value
 *      }
 *    }));
 *  }
 * </pre>
 *
 * @author edgar
 * @since 0.10.0
 */
public class Deferred extends Result {

  /**
   * Deferred initializer, useful to provide a more functional API.
   *
   * @author edgar
   * @since 0.10.0
   */
  public static interface Initializer0 {

    /**
     * Run the initializer block.
     *
     * @param deferred Deferred object.
     * @throws Exception If something goes wrong.
     */
    void run(Deferred deferred) throws Exception;
  }

  /**
   * Deferred initializer with {@link Request} access, useful to provide a more functional API.
   *
   * @author edgar
   * @since 0.10.0
   */
  public static interface Initializer {

    /**
     * Run the initializer block.
     *
     * @param req Current request.
     * @param deferred Deferred object.
     * @throws Exception If something goes wrong.
     */
    void run(Request req, Deferred deferred) throws Exception;
  }

  /**
   * A deferred handler. Application code should never use this class. INTERNAL USE ONLY.
   *
   * @author edgar
   * @since 0.10.0
   */
  public static interface Handler {
    void handle(Result result, Throwable exception);
  }

  /** Deferred initializer. Optional. */
  private Initializer initializer;

  /** Deferred handler. Internal. */
  private Handler handler;

  private String executor;

  private String callerThread;

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param executor Executor to use.
   * @param initializer An initializer.
   */
  public Deferred(final String executor, final Initializer0 initializer) {
    this(executor, (req, deferred) -> initializer.run(deferred));
  }

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param initializer An initializer.
   */
  public Deferred(final Initializer0 initializer) {
    this(null, initializer);
  }

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param initializer An initializer.
   */
  public Deferred(final Initializer initializer) {
    this(null, initializer);
  }

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param executor Executor to use.
   * @param initializer An initializer.
   */
  public Deferred(final String executor, final Initializer initializer) {
    this.executor = executor;
    this.initializer = requireNonNull(initializer, "Initializer is required.");
    this.callerThread = Thread.currentThread().getName();
  }

  /**
   * Creates a new {@link Deferred}.
   */
  public Deferred() {
  }

  /**
   * {@link #resolve(Object)} or {@link #reject(Throwable)} the given value.
   *
   * @param value Resolved value.
   */
  @Override
  public Result set(final Object value) {
    if (value instanceof Throwable) {
      reject((Throwable) value);
    } else {
      resolve(value);
    }
    return this;
  }

  /**
   * Get an executor to run this deferred result. If the executor is present, then it will be use it
   * to execute the deferred object. Otherwise it will use the global/application executor.
   *
   * @return Executor to use or fallback to global/application executor.
   */
  public Optional<String> executor() {
    return Optional.ofNullable(executor);
  }

  /**
   * Name of the caller thread (thread that creates this deferred object).
   *
   * @return Name of the caller thread (thread that creates this deferred object).
   */
  public String callerThread() {
    return callerThread;
  }

  /**
   * Resolve the deferred value and handle it. This method will send the response to a client and
   * cleanup and close all the resources.
   *
   * @param value A value for this deferred.
   */
  public void resolve(final Object value) {
    if (value == null) {
      handler.handle(null, null);
    } else {
      Result result;
      if (value instanceof Result) {
        result = (Result) value;
      } else {
        super.set(value);
        result = clone();
      }
      handler.handle(result, null);
    }
  }

  /**
   * Resolve the deferred with an error and handle it. This method will handle the given exception,
   * send the response to a client and cleanup and close all the resources.
   *
   * @param cause A value for this deferred.
   */
  public void reject(final Throwable cause) {
    handler.handle(null, cause);
  }

  /**
   * Setup a handler for this deferred. Application code should never call this method: INTERNAL USE
   * ONLY.
   *
   * @param req Current request.
   * @param handler A response handler.
   * @throws Exception If initializer fails to start.
   */
  public void handler(final Request req, final Handler handler) throws Exception {
    this.handler = requireNonNull(handler, "Handler is required.");
    if (initializer != null) {
      initializer.run(req, this);
    }
  }

  /**
   * Functional version of {@link Deferred#Deferred(Initializer)}.
   *
   * Using the default executor (current thread):
   *
   * <pre>{@code
   * {
   *   get("/fork", deferred(req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * Using a custom executor:
   *
   * <pre>{@code
   * {
   *   executor(new ForkJoinPool());
   *
   *   get("/fork", deferred(req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param handler Application block.
   * @return A new deferred handler.
   */
  public static Deferred deferred(final Route.OneArgHandler handler) {
    return deferred(null, handler);
  }

  /**
   * Functional version of {@link Deferred#Deferred(Initializer)}.
   *
   * Using the default executor (current thread):
   *
   * <pre>{@code
   * {
   *   get("/fork", deferred(() -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * Using a custom executor:
   *
   * <pre>{@code
   * {
   *   executor(new ForkJoinPool());
   *
   *   get("/fork", deferred(() -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param handler Application block.
   * @return A new deferred.
   */
  public static Deferred deferred(final Route.ZeroArgHandler handler) {
    return deferred(null, handler);
  }

  /**
   * Functional version of {@link Deferred#Deferred(Initializer)}. To use ideally with one
   * or more {@link Executor}:
   *
   * <pre>{@code
   * {
   *   executor("cached", Executors.newCachedExecutor());
   *
   *   get("/fork", deferred("cached", () -> {
   *     return "OK";
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param executor Executor to run the deferred.
   * @param handler Application block.
   * @return A new deferred handler.
   */
  public static Deferred deferred(final String executor, final Route.ZeroArgHandler handler) {
    return deferred(executor, req -> handler.handle());
  }

  /**
   * Functional version of {@link Deferred#Deferred(Initializer)}. To use ideally with one
   * or more {@link Executor}:
   *
   * <pre>{@code
   * {
   *   executor("cached", Executors.newCachedExecutor());
   *
   *   get("/fork", deferred("cached", req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param executor Executor to run the deferred.
   * @param handler Application block.
   * @return A new deferred handler.
   */
  public static Deferred deferred(final String executor, final Route.OneArgHandler handler) {
    return new Deferred(executor, (req, deferred) -> {
      try {
        deferred.resolve(handler.handle(req));
      } catch (Throwable x) {
        deferred.reject(x);
      }
    });
  }

}
