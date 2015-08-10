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
package org.jooby.handlers;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Handle preflight and simple CORS requests. CORS options are set via: Cors.
 *
 * @author edgar
 * @since 0.8.0
 * @see Cors
 */
public class CorsHandler implements Route.Filter {

  private static final String ORIGIN = "Origin";

  private static final String ANY_ORIGIN = "*";

  private static final String AC_REQUEST_METHOD = "Access-Control-Request-Method";

  private static final String AC_REQUEST_HEADERS = "Access-Control-Request-Headers";

  private static final String AC_MAX_AGE = "Access-Control-Max-Age";

  private static final String AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  private static final String AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  private static final String AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  private static final String AC_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  private static final String AC_ALLOW_METHODS = "Access-Control-Allow-Methods";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Cors.class);

  private Optional<Cors> cors = Optional.empty();

  /**
   * Creates a new {@link CorsHandler}.
   *
   * @param cors Cors options, or empty for using default options.
   */
  public CorsHandler(final Cors cors) {
    this.cors = Optional.of(requireNonNull(cors, "Cors is required."));
  }

  /**
   * Creates a new {@link CorsHandler}.
   */
  public CorsHandler() {
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    Optional<String> origin = req.header("Origin").toOptional();
    Cors cors = this.cors.orElseGet(() -> req.require(Cors.class));
    if (cors.enabled() && origin.isPresent()) {
      cors(cors, req, rsp, origin.get());
    }
    chain.next(req, rsp);
  }

  private void cors(final Cors cors, final Request req, final Response rsp,
      final String origin) throws Exception {
    if (cors.allowOrigin(origin)) {
      log.debug("allowed origin: {}", origin);
      if (preflight(req)) {
        log.debug("handling preflight for: {}", origin);
        preflight(cors, req, rsp, origin);
      } else {
        log.debug("handling simple cors for: {}", origin);
        if ("null".equals(origin)) {
          rsp.header(AC_ALLOW_ORIGIN, ANY_ORIGIN);
        } else {
          rsp.header(AC_ALLOW_ORIGIN, origin);
          if (!cors.anyOrigin()) {
            rsp.header("Vary", ORIGIN);
          }
          if (cors.credentials()) {
            rsp.header(AC_ALLOW_CREDENTIALS, true);
          }
          if (!cors.exposedHeaders().isEmpty()) {
            rsp.header(AC_EXPOSE_HEADERS, join(cors.exposedHeaders()));
          }
        }
      }
    }
  }

  private boolean preflight(final Request req) {
    return req.method().equals("OPTIONS") && req.header(AC_REQUEST_METHOD).isSet();
  }

  private void preflight(final Cors cors, final Request req, final Response rsp,
      final String origin) {
    /**
     * Allowed method
     */
    boolean allowMethod = req.header(AC_REQUEST_METHOD).toOptional()
        .map(cors::allowMethod)
        .orElse(false);
    if (!allowMethod) {
      return;
    }

    /**
     * Allowed headers
     */
    List<String> headers = req.header(AC_REQUEST_HEADERS).toOptional().map(header ->
        Splitter.on(',').trimResults().omitEmptyStrings().splitToList(header)
        ).orElse(Collections.emptyList());
    if (!cors.allowHeaders(headers)) {
      return;
    }

    /**
     * Allowed methods
     */
    rsp.header(AC_ALLOW_METHODS, join(cors.allowedMethods()));

    List<String> allowedHeaders = cors.anyHeader() ? headers : cors.allowedHeaders();
    rsp.header(AC_ALLOW_HEADERS, join(allowedHeaders));

    /**
     * Allow credentials
     */
    if (cors.credentials()) {
      rsp.header(AC_ALLOW_CREDENTIALS, true);
    }

    if (cors.maxAge() > 0) {
      rsp.header(AC_MAX_AGE, cors.maxAge());
    }

    rsp.header(AC_ALLOW_ORIGIN, origin);

    if (!cors.anyOrigin()) {
      rsp.header("Vary", ORIGIN);
    }

    rsp.status(Status.OK).end();
  }

  private String join(final List<String> values) {
    return Joiner.on(',').join(values);
  }

}
