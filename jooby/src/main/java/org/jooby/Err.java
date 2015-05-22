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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * An exception that carry a {@link Status}. The status field will be set in the HTTP
 * response.
 *
 * See {@link Err.Handler} for more details on how to deal with exceptions.
 *
 * @author edgar
 * @since 0.1.0
 */
@SuppressWarnings("serial")
public class Err extends RuntimeException {

  /**
   * Default err handler it does content negotation. On <code>text/html</code> requests the err
   * handler creates an <code>err</code> view and set as model the {@link Err#toMap()}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public static class DefHandler implements Err.Handler {

    /** Default err view. */
    public static final String VIEW = "err";

    /** logger, logs!. */
    private final Logger log = LoggerFactory.getLogger(Err.class);

    @Override
    public void handle(final Request req, final Response rsp, final Err ex)
        throws Exception {
      log.error("execution of: " + req.method() + " " + req.path() + " resulted in exception", ex);

      rsp.send(
          Results
              .when(MediaType.html, () -> Results.html(VIEW).put("err", ex.toMap()))
              .when(MediaType.all, () -> ex.toMap())
          );
    }

  }

  /**
   * Handle and render exceptions. Error handlers are executed in the order they were provided, the
   * first err handler that send an output wins!
   *
   * The default err handler does content negotation on error, see {@link DefHandler}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Handler {

    /**
     * Handle a route exception by probably logging the error and sending a err response to the
     * client.
     *
     * @param req HTTP request.
     * @param rsp HTTP response.
     * @param ex Error found and status code.
     * @throws Exception If something goes wrong.
     */
    void handle(Request req, Response rsp, Err ex) throws Exception;
  }

  /**
   * The status code. Required.
   */
  private int status;

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param message A error message. Required.
   * @param cause The cause of the problem.
   */
  public Err(final Status status, final String message, final Throwable cause) {
    super(message(status, message), cause);
    this.status = status.value();
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param message A error message. Required.
   * @param cause The cause of the problem.
   */
  public Err(final int status, final String message, final Throwable cause) {
    this(Status.valueOf(status), message, cause);
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param message A error message. Required.
   */
  public Err(final Status status, final String message) {
    super(message(status, message));
    this.status = status.value();
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param message A error message. Required.
   */
  public Err(final int status, final String message) {
    this(Status.valueOf(status), message);
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param cause The cause of the problem.
   */
  public Err(final Status status, final Throwable cause) {
    super(message(status, null), cause);
    this.status = status.value();
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   * @param cause The cause of the problem.
   */
  public Err(final int status, final Throwable cause) {
    this(Status.valueOf(status), cause);
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   */
  public Err(final Status status) {
    super(message(status, null));
    this.status = status.value();
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   */
  public Err(final int status) {
    this(Status.valueOf(status));
  }

  /**
   * @return The status code to send as response.
   */
  public int statusCode() {
    return status;
  }

  /**
   * Produces a friendly view of the err, resulting map has these attributes:
   *
   * <pre>
   *  message: exception message (if present)
   *  stacktrace: array with the stacktrace
   *  status: status code
   *  reason: a status code reason
   * </pre>
   *
   * @return A lightweight view of the err.
   */
  public Map<String, Object> toMap() {
    Status status = Status.valueOf(this.status);
    Throwable cause = Optional.ofNullable(getCause()).orElse(this);
    String message = Optional.ofNullable(cause.getMessage()).orElse(status.reason());

    String[] stacktrace = Throwables.getStackTraceAsString(cause).replace("\r", "").split("\\n");

    Map<String, Object> err = new LinkedHashMap<>();
    err.put("message", message);
    err.put("stacktrace", stacktrace);
    err.put("status", status.value());
    err.put("reason", status.reason());

    return err;
  }

  /**
   * Build an error message using the HTTP status.
   *
   * @param status The HTTP Status.
   * @param tail A message to append.
   * @return An error message.
   */
  private static String message(final Status status, final String tail) {
    requireNonNull(status, "A HTTP Status is required.");
    return status.reason() + "(" + status.value() + ")" + (tail == null ? "" : ": " + tail);
  }
}
