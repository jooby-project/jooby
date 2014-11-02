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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

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
   * Default err handler with content negotation. If an error is found when accept header is
   * <code>text/html</code> this err handler will delegate err rendering to a template processor.
   *
   * @author edgar
   * @since 0.1.0
   */
  public static class Default implements Err.Handler {

    @Override
    public void handle(final Request req, final Response rsp, final Exception ex)
        throws Exception {
      LoggerFactory.getLogger(Err.class).error("execution of: " + req.path() +
          " resulted in exception", ex);

      Map<String, Object> err = err(req, rsp, ex);

      rsp.format()
          .when(MediaType.html, () -> View.of(errPage(req, rsp, ex), err))
          .when(MediaType.all, () -> err)
          .send();
    }

  }

  /**
   * Route error handler, it creates a default err model and default view name.
   *
   * The default err handler does content negotation on error, see {@link Default}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Handler {

    /**
     * Build a err model from exception. The model it's map with the following attributes:
     *
     * <pre>
     *   message: String
     *   stacktrace: String[]
     *   status: int
     *   reason: String
     *   referer: String
     * </pre>
     *
     * <p>
     * NOTE: {@link Response#status()} it was set by default to status code {@code >} 400. This is
     * the default behavior you can use the generated status code and/or override it.
     * </p>
     *
     * @param req A HTTP Request.
     * @param rsp A HTTP Response with a default err status code ({@code >} 400).
     * @param ex Current exception object.
     * @return A err model.
     */
    default Map<String, Object> err(final Request req, final Response rsp, final Exception ex) {
      Map<String, Object> error = new LinkedHashMap<>();
      Status status = rsp.status().get();
      String message = ex.getMessage();
      message = message == null ? status.reason() : message;
      error.put("message", message);
      StringWriter writer = new StringWriter();
      ex.printStackTrace(new PrintWriter(writer));
      String[] stacktrace = writer.toString().replace("\r", "").split("\\n");
      error.put("stacktrace", stacktrace);
      error.put("status", status.value());
      error.put("reason", status.reason());
      error.put("referer", req.header("referer"));

      return error;
    }

    /**
     * Convert current err to a view location, defaults is: <code>/err</code>.
     *
     * @param req HTTP request.
     * @param rsp HTTP Response.
     * @param ex Error found.
     * @return An err page to be render by a template processor.
     */
    default String errPage(final Request req, final Response rsp, final Exception ex) {
      return "/err";
    }

    /**
     * Handle a route exception by probably logging the error and sending a err response to the
     * client.
     *
     * @param req HTTP request.
     * @param rsp HTTP response.
     * @param ex Error found.
     * @throws Exception If something goes wrong.
     */
    void handle(Request req, Response rsp, Exception ex) throws Exception;
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
  public Err(final Status status, final String message, final Exception cause) {
    super(message(status, message), cause);
    this.status = status.value();
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
   * @param cause The cause of the problem.
   */
  public Err(final Status status, final Exception cause) {
    super(message(status, ""), cause);
    this.status = status.value();
  }

  /**
   * Creates a new {@link Err}.
   *
   * @param status A HTTP status. Required.
   */
  public Err(final Status status) {
    super(message(status, ""));
    this.status = status.value();
  }

  /**
   * @return The status code to send as response.
   */
  public int statusCode() {
    return status;
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
    return status.reason() + "(" + status.value() + "): " + tail;
  }
}
