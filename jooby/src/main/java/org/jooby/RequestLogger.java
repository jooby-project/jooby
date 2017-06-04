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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>request logger</h1>
 * <p>
 * Log all the matched incoming requested using the
 * <a href="https://en.wikipedia.org/wiki/Common_Log_Format">NCSA format</a> (a.k.a common log
 * format).
 * </p>
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use("*", new RequestLog());
 *
 *   ...
 * }
 * }</pre>
 *
 * <p>
 * Output looks like:
 * </p>
 *
 * <pre>
 * 127.0.0.1 - - [04/Oct/2016:17:51:42 +0000] "GET / HTTP/1.1" 200 2
 * </pre>
 *
 * <p>
 * You probably want to configure the <code>RequestLog</code> logger to save output into a new file:
 * </p>
 *
 * <pre>
 * &lt;appender name="ACCESS" class="ch.qos.logback.core.rolling.RollingFileAppender"&gt;
 *   &lt;file&gt;access.log&lt;/file&gt;
 *   &lt;rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy"&gt;
 *     &lt;fileNamePattern&gt;access.%d{yyyy-MM-dd}.log&lt;/fileNamePattern&gt;
 *   &lt;/rollingPolicy&gt;
 *
 *   &lt;encoder&gt;
 *     &lt;pattern&gt;%msg%n&lt;/pattern&gt;
 *   &lt;/encoder&gt;
 * &lt;/appender&gt;
 *
 * &lt;logger name="org.jooby.RequestLogger" additivity="false"&gt;
 *   &lt;appender-ref ref="ACCESS" /&gt;
 * &lt;/logger&gt;
 * </pre>
 *
 * <p>
 * Due that authentication is provided via module or custom filter, there is no concept of
 * logged/authenticated user. Still you can log the current user by setting an user id provider at
 * construction time:
 * </p>
 *
 * <pre>{@code
 * {
 *
 *   use("*", (req, rsp) -> {
 *     // authenticate user and set local attribute
 *     String userId = ...;
 *     req.set("userId", userId);
 *   });
 *
 *   use("*", new RequestLogger(req -> {
 *     return req.get("userId");
 *   }));
 * }
 * }</pre>
 *
 * <p>
 * Here an application filter set an <code>userId</code> request attribute and then we provide that
 * <code>userId</code> to {@link RequestLogger}.
 * </p>
 *
 * <h2>custom log function</h2>
 * <p>
 * By default it uses the underlying logging system: <a href="http://logback.qos.ch">logback</a>.
 * That's why we previously show how to configure the <code>org.jooby.RequestLogger</code> in
 * <code>logback.xml</code>.
 * </p>
 *
 * <p>
 * If you want to log somewhere else and/or use a different technology then:
 * </p>
 *
 * <pre>{@code
 * {
 *   use("*", new ResponseLogger()
 *     .log(line -> {
 *       System.out.println(line);
 *     }));
 * }
 * }</pre>
 *
 * <p>
 * This is just an example but of course you can log the <code>NCSA</code> line to database, jms
 * queue, etc...
 * </p>
 *
 * <h2>latency</h2>
 *
 * <pre>{@code
 * {
 *   use("*", new RequestLogger()
 *       .latency());
 * }
 * }</pre>
 *
 * <p>
 * It add a new entry at the last of the <code>NCSA</code> output that represents the number of
 * <code>ms</code> it took to process the incoming release.
 *
 * <h2>extended</h2>
 * <p>
 * Extend the <code>NCSA</code> by adding the <code>Referer</code> and <code>User-Agent</code>
 * headers to the output.
 * </p>
 *
 * <h2>dateFormatter</h2>
 *
 * <pre>{@code
 * {
 *   use("*", new RequestLogger()
 *       .dateFormatter(ts -> ...));
 *
 *   // OR
 *   use("*", new RequestLogger()
 *       .dateFormatter(DateTimeFormatter...));
 * }
 * }</pre>
 *
 * <p>
 * Override, the default formatter for the request arrival time defined by:
 * {@link Request#timestamp()}. You can provide a function or an instance of
 * {@link DateTimeFormatter}.
 * </p>
 *
 * <p>
 * The default formatter use the default server time zone, provided by
 * {@link ZoneId#systemDefault()}. It's possible to just override the time zone (not the entirely
 * formatter) too:
 * </p>
 *
 * <pre>{@code
 * {
 *   use("*", new RequestLogger()
 *      .dateFormatter(ZoneId.of("UTC"));
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0
 */
public class RequestLogger implements Route.Handler {

  private static final String USER_AGENT = "User-Agent";

  private static final String REFERER = "Referer";

  private static final String CONTENT_LENGTH = "Content-Length";

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
      .withZone(ZoneId.systemDefault());

  private static final String DASH = "-";
  private static final char SP = ' ';
  private static final char BL = '[';
  private static final char BR = ']';
  private static final char Q = '\"';
  private static final char QUERY = '?';

  private static Function<Request, String> ANNON = req -> DASH;

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Function<Request, String> userId;

  private Consumer<String> logRecord = log::info;

  private Function<Long, String> df;

  private boolean latency;

  private boolean queryString;

  private boolean extended;

  /**
   * Creates a new {@link RequestLogger} and use the given function and userId provider. Please
   * note, if the user isn't present this function is allowed to returns <code>-</code> (dash
   * character).
   *
   * @param userId User ID provider.
   */
  public RequestLogger(final Function<Request, String> userId) {
    this.userId = requireNonNull(userId, "User ID provider required.");
    dateFormatter(FORMATTER);
  }

  /**
   * Creates a new {@link RequestLogger} without user identifier.
   */
  public RequestLogger() {
    this(ANNON);
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    /** Push complete callback . */
    rsp.complete((ereq, ersp, x) -> {
      StringBuilder sb = new StringBuilder(256);
      long timestamp = req.timestamp();
      sb.append(req.ip());
      sb.append(SP).append(DASH).append(SP);
      sb.append(userId.apply(req));
      sb.append(SP);
      sb.append(BL).append(df.apply(timestamp)).append(BR);
      sb.append(SP);
      sb.append(Q).append(req.method());
      sb.append(SP);
      sb.append(req.path());
      if (queryString) {
        req.queryString().ifPresent(s -> sb.append(QUERY).append(s));
      }
      sb.append(SP);
      sb.append(req.protocol());
      sb.append(Q).append(SP);
      int status = ersp.status().orElse(Status.OK).value();
      sb.append(status);
      sb.append(SP);
      sb.append(ersp.header(CONTENT_LENGTH).value(DASH));
      if (extended) {
        sb.append(SP);
        sb.append(Q).append(req.header(REFERER).value(DASH)).append(Q).append(SP);
        sb.append(Q).append(req.header(USER_AGENT).value(DASH)).append(Q);
      }
      if (latency) {
        long now = System.currentTimeMillis();
        sb.append(SP);
        sb.append(now - timestamp);
      }
      logRecord.accept(sb.toString());
    });
  }

  /**
   * Log an NCSA line to somewhere.
   *
   * <pre>{@code
   *  {
   *    use("*", new RequestLogger()
   *        .log(System.out::println)
   *    );
   *  }
   * }</pre>
   *
   * @param log Log callback.
   * @return This instance.
   */
  public RequestLogger log(final Consumer<String> log) {
    this.logRecord = requireNonNull(log, "Logger required.");
    return this;
  }

  /**
   * Override the default date formatter.
   *
   * @param formatter New formatter to use.
   * @return This instance.
   */
  public RequestLogger dateFormatter(final DateTimeFormatter formatter) {
    requireNonNull(formatter, "Formatter required.");
    return dateFormatter(ts -> formatter.format(Instant.ofEpochMilli(ts)));
  }

  /**
   * Override the default date formatter.
   *
   * @param formatter New formatter to use.
   * @return This instance.
   */
  public RequestLogger dateFormatter(final Function<Long, String> formatter) {
    requireNonNull(formatter, "Formatter required.");
    this.df = formatter;
    return this;
  }

  /**
   * Keep the default formatter but use the provided timezone.
   *
   * @param zoneId Zone id.
   * @return This instance.
   */
  public RequestLogger dateFormatter(final ZoneId zoneId) {
    return dateFormatter(FORMATTER.withZone(zoneId));
  }

  /**
   * Log latency (how long does it takes to process an incoming request) at the end of the NCSA
   * line.
   *
   * @return This instance.
   */
  public RequestLogger latency() {
    this.latency = true;
    return this;
  }

  /**
   * Log full path of the request including query string.
   *
   * @return This instance.
   */
  public RequestLogger queryString() {
    this.queryString = true;
    return this;
  }

  /**
   * Append <code>Referer</code> and <code>User-Agent</code> entries to the NCSA line.
   *
   * @return This instance.
   */
  public RequestLogger extended() {
    this.extended = true;
    return this;
  }

}
