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

import java.util.concurrent.Callable;

/**
 * <h1>async request processing</h1>
 * A Deferred result, useful for async request processing. Application can produces a result from a
 * thread of its choice.
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *    ExecutorService executor = ...;
 *
 *    get("/async", promise(deferred {@literal ->} {
 *      executor.execute(() {@literal ->} {
 *        try {
 *          deferred.resolve(...); // success value
 *        } catch (Exception ex) {
 *          deferred.reject(ex); // error value
 *        }
 *      });
 *    }));
 *  }
 * </pre>
 *
 * <p>
 * Or with automatic error handler:
 * </p>
 *
 * <pre>
 * {
 *    ExecutorService executor = ...;
 *
 *    get("/async", promise(deferred {@literal ->} {
 *      executor.execute(() {@literal ->} {
 *        deferred.resolve(() {@literal ->} {
 *          Object value = ...
 *          return value;
 *        }); // success value
 *      });
 *    }));
 *  }
 * </pre>
 *
 * <p>
 * Or as {@link Runnable} with automatic error handler:
 * </p>
 *
 * <pre>
 * {
 *    ExecutorService executor = ...;
 *
 *    get("/async", promise(deferred {@literal ->} {
 *      executor.execute(deferred.run(() {@literal ->} {
 *        Object value = ...
 *        return value;
 *      }); // success value
 *    }));
 *  }
 * </pre>
 *
 * <p>
 * Application can produces a result from a different thread. Once result is ready, a call to
 * {@link #set(Object)} is required. Please note, a call to {@link #set(Object)} is required in case
 * of errors.
 * </p>
 *
 * <h2>error handling</h2>
 * <p>
 * Due the code will be handle by an external/new thread, you MUST be ready to handle errors,
 * usually with a try/catch statement. A call to {@link #set(Object)} is required too in case of an
 * error.
 * </p>
 * <p>
 * Checkout the utility method {@link #resolve(Callable)} and/or {@link #run(Callable)}. Both of
 * them catch and handle exceptions for you.
 * </p>
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
    void handle(Result result, Exception exception);
  }

  /** Current req. Optional. */
  private Request req;

  /** Deferred initializer. Optional. */
  private Initializer initializer;

  /** Deferred handler. Internal. */
  private Handler handler;

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param initializer An initializer.
   */
  public Deferred(final Initializer0 initializer) {
    requireNonNull(initializer, "Initializer is required.");
    this.initializer = (req, deferred) -> initializer.run(deferred);
  }

  /**
   * Creates a new {@link Deferred} with an initializer.
   *
   * @param req Current request.
   * @param initializer An initializer.
   */
  public Deferred(final Request req, final Initializer initializer) {
    this.req = requireNonNull(req, "Request is required.");
    this.initializer = requireNonNull(initializer, "Initializer is required.");
  }

  /**
   * Creates a new {@link Deferred}.
   */
  public Deferred() {
  }

  /**
   * {@link #resolve(Object)} or {@link #reject(Exception)} the given value.
   *
   * @param value Resolved value.
   */
  @Override
  public Result set(final Object value) {
    if (value instanceof Exception) {
      reject((Exception) value);
    } else {
      resolve(value);
    }
    return this;
  }

  /**
   * Resolve the deferred value and handle it. This method will send the response to a client and
   * cleanup and close all the resources.
   *
   * @param value A value for this deferred.
   */
  public void resolve(final Object value) {
    super.set(value);
    handler.handle(clone(), null);
  }

  /**
   * Resolve the deferred with an error and handle it. This method will handle the given exception,
   * send the response to a client and cleanup and close all the resources.
   *
   * @param cause A value for this deferred.
   */
  public void reject(final Exception cause) {
    handler.handle(null, cause);
  }

  /**
   * Produces a {@link Runnable} that runs the given {@link Callable} and {@link #resolve(Callable)}
   * or {@link #reject(Exception)} the deferred.
   *
   * Please note, the given {@link Callable} runs in the caller thread.
   *
   * @param block Callable that produces a result.
   * @param <T> Resulting type.
   * @return This deferred as {@link Runnable}.
   */
  public <T> Runnable run(final Callable<T> block) {
    return () -> {
      resolve(block);
    };
  }

  /**
   * Run the given {@link Callable} and {@link #resolve(Callable)} or {@link #reject(Exception)} the
   * deferred.
   *
   * Please note, the given {@link Callable} runs in the caller thread.
   *
   * @param block Callable that produces a result.
   * @param <T> Resulting type.
   */
  public <T> void resolve(final Callable<T> block) {
    try {
      resolve(block.call());
    } catch (Exception ex) {
      reject(ex);
    }
  }

  /**
   * Setup a handler for this deferred. Application code should never call this method: INTERNAL USE
   * ONLY.
   *
   * @param handler A response handler.
   * @throws Exception If initializer fails to start.
   */
  public void handler(final Handler handler) throws Exception {
    this.handler = requireNonNull(handler, "Handler is required.");
    if (initializer != null) {
      initializer.run(this.req, this);
    }
    this.req = null;
  }

}
