/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * <h1>Access Log Handler</h1>
 * <p>
 * Log incoming requested using the
 * <a href="https://en.wikipedia.org/wiki/Common_Log_Format">NCSA format</a> (a.k.a common log
 * format).
 * </p>
 *
 * If you run behind a reverse proxy that has been configured to send the X-Forwarded-* header,
 * please consider to set {@link Router#setTrustProxy(boolean)} option.
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   decorator(new AccessLogHandler());
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
 * You probably want to configure the <code>AccessLogHandler</code> logger to save output into a new file:
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
 * &lt;logger name="io.jooby.AccessLogHandler" additivity="false"&gt;
 *   &lt;appender-ref ref="ACCESS" /&gt;
 * &lt;/logger&gt;
 * </pre>
 *
 * <p>
 * By defaults it log the available user context: {@link Context#getUser()}. To override this:
 * </p>
 *
 * <pre>{@code
 * {
 *
 *   decorator("*", new AccessLogHandler(ctx -> {
 *     // retrieve user ID from context.
 *   }));
 * }
 * }</pre>
 *
 * <h2>custom log function</h2>
 * <p>
 * By default it uses the underlying logging system: <a href="http://logback.qos.ch">logback</a>.
 * That's why we previously show how to configure the <code>io.jooby.AccessLogHandler</code> in
 * <code>logback.xml</code>.
 * </p>
 *
 * <p>
 * If you want to log somewhere else and/or use a different method then:
 * </p>
 *
 * <pre>{@code
 * {
 *   use("*", new AccessLogHandler()
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
 * <h2>request and response headers</h2>
 * <p>
 * You can add extra headers using the {@link AccessLogHandler#requestHeader(String...)} and
 * {@link AccessLogHandler#responseHeader(String...)}
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
 * You can provide a function or an instance of {@link DateTimeFormatter}.
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
 * @since 2.5.2
 */
public class AccessLogHandler implements Route.Decorator {
  private static final String USER_AGENT = "User-Agent";

  private static final String REFERER = "Referer";

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
      .withZone(ZoneId.systemDefault());

  private static final String DASH = "-";
  private static final char SP = ' ';
  private static final char BL = '[';
  private static final char BR = ']';
  private static final char Q = '\"';

  private static final Function<Context, String> USER_OR_DASH = ctx ->
      Optional.ofNullable(ctx.getUser())
          .map(Object::toString)
          .orElse(DASH);
  /** Default buffer size. */
  private static final int MESSAGE_SIZE = 256;

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Function<Context, String> userId;

  private Consumer<String> logRecord = log::info;

  private Function<Long, String> df;

  private List<String> requestHeaders = Collections.emptyList();

  private List<String> responseHeaders = Collections.emptyList();

  /**
   * Creates a new {@link AccessLogHandler} and use the given function and userId provider. Please
   * note, if the user isn't present this function is allowed to returns <code>-</code> (dash
   * character).
   *
   * @param userId User ID provider.
   */
  public AccessLogHandler(@Nonnull Function<Context, String> userId) {
    this.userId = requireNonNull(userId, "User ID provider required.");
    dateFormatter(FORMATTER);
  }

  /**
   * Creates a new {@link AccessLogHandler} without user identifier.
   */
  public AccessLogHandler() {
    this(USER_OR_DASH);
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    long timestamp = System.currentTimeMillis();
    return ctx -> {
      ctx.onComplete(context -> {
        StringBuilder sb = new StringBuilder(MESSAGE_SIZE);
        sb.append(ctx.getRemoteAddress());
        sb.append(SP).append(DASH).append(SP);
        sb.append(userId.apply(ctx));
        sb.append(SP);
        sb.append(BL).append(df.apply(timestamp)).append(BR);
        sb.append(SP);
        sb.append(Q).append(ctx.getMethod());
        sb.append(SP);
        sb.append(ctx.getRequestPath());
        sb.append(ctx.queryString());
        sb.append(SP);
        sb.append(ctx.getProtocol());
        sb.append(Q).append(SP);
        sb.append(ctx.getResponseCode().value());
        sb.append(SP);
        long responseLength = ctx.getResponseLength();
        sb.append(responseLength >= 0 ? responseLength : DASH);
        long now = System.currentTimeMillis();
        sb.append(SP);
        sb.append(now - timestamp);
        appendHeaders(sb, requestHeaders, h -> ctx.header(h).valueOrNull());
        appendHeaders(sb, responseHeaders, h -> ctx.getResponseHeader(h));
        logRecord.accept(sb.toString());
      });
      return next.apply(ctx);
    };
  }

  private void appendHeaders(StringBuilder buff, List<String> requestHeaders,
      Function<String, String> headers) {
    for (String header : requestHeaders) {
      String value = headers.apply(header);
      if (value == null) {
        buff.append(SP).append(Q).append(DASH).append(Q);
      } else {
        buff.append(SP).append(Q).append(value).append(Q);
      }
    }
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
  public @Nonnull AccessLogHandler log(@Nonnull Consumer<String> log) {
    this.logRecord = requireNonNull(log, "Consumer is required.");
    return this;
  }

  /**
   * Override the default date formatter.
   *
   * @param formatter New formatter to use.
   * @return This instance.
   */
  public @Nonnull AccessLogHandler dateFormatter(@Nonnull DateTimeFormatter formatter) {
    return dateFormatter(ts -> formatter.format(Instant.ofEpochMilli(ts)));
  }

  /**
   * Override the default date formatter.
   *
   * @param formatter New formatter to use.
   * @return This instance.
   */
  public @Nonnull AccessLogHandler dateFormatter(final Function<Long, String> formatter) {
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
  public @Nonnull AccessLogHandler dateFormatter(@Nonnull ZoneId zoneId) {
    return dateFormatter(FORMATTER.withZone(zoneId));
  }

  /**
   * Append <code>Referer</code> and <code>User-Agent</code> entries to the NCSA line.
   *
   * @return This instance.
   */
  public @Nonnull AccessLogHandler extended() {
    return requestHeader(USER_AGENT, REFERER);
  }

  /**
   * Append request headers to the end of line.
   *
   * @param names Header names.
   * @return This instance.
   */
  public @Nonnull AccessLogHandler requestHeader(@Nonnull String... names) {
    this.requestHeaders = Arrays.asList(names);
    return this;
  }

  /**
   * Append response headers to the end of line.
   *
   * @param names Header names.
   * @return This instance.
   */
  public @Nonnull AccessLogHandler responseHeader(@Nonnull String... names) {
    this.responseHeaders = Arrays.asList(names);
    return this;
  }
}
