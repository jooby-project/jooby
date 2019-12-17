/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

import static io.jooby.MediaType.html;
import static io.jooby.MediaType.json;
import static io.jooby.MediaType.text;

/**
 * Catch and encode application errors.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface ErrorHandler {

  /**
   * Default error handler with support for content-negotiation. It renders a html error page
   * or json.
   */
  ErrorHandler DEFAULT = (ctx, cause, statusCode) -> {
    ctx.getRouter().getLog().error(errorMessage(ctx, statusCode), cause);

    MediaType type = ctx.accept(Arrays.asList(html, json, text));
    if (json.equals(type)) {
      String message = Optional.ofNullable(cause.getMessage()).orElse(statusCode.reason());
      ctx.setResponseType(json)
          .setResponseCode(statusCode)
          .send("{\"message\":\"" + XSS.json(message) + "\",\"statusCode\":" + statusCode.value()
              + ",\"reason\":\"" + statusCode.reason() + "\"}");
    } else if (text.equals(type)) {
      StringBuilder message = new StringBuilder();
      message.append(ctx.getMethod()).append(" ").append(ctx.getRequestPath()).append(" ");
      message.append(statusCode.value()).append(" ").append(statusCode.reason());
      if (cause.getMessage() != null) {
        message.append("\n").append(XSS.json(cause.getMessage()));
      }
      ctx.setResponseType(text)
          .setResponseCode(statusCode)
          .send(message.toString());
    } else {
      String message = cause.getMessage();
      StringBuilder html = new StringBuilder("<!doctype html>\n")
          .append("<html>\n")
          .append("<head>\n")
          .append("<meta charset=\"utf-8\">\n")
          .append("<style>\n")
          .append("body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n")
          .append("h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n")
          .append("h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n")
          .append("footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n")
          .append("hr {background-color: #f7f7f9;}\n")
          .append("div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n")
          .append("p {padding-left: 20px;}\n")
          .append("p.tab {padding-left: 40px;}\n")
          .append("</style>\n")
          .append("<title>")
          .append(statusCode)
          .append("</title>\n")
          .append("<body>\n")
          .append("<h1>").append(statusCode.reason()).append("</h1>\n")
          .append("<hr>\n");

      if (message != null && !message.equals(statusCode.toString())) {
        html.append("<h2>message: ").append(XSS.html(message)).append("</h2>\n");
      }
      html.append("<h2>status code: ").append(statusCode.value()).append("</h2>\n");

      html.append("</body>\n")
          .append("</html>");

      ctx
          .setResponseType(MediaType.html)
          .setResponseCode(statusCode)
          .send(html.toString());
    }
  };

  /**
   * Produces an error response using the given exception and status code.
   *
   * @param ctx Web context.
   * @param cause Application error.
   * @param statusCode Status code.
   */
  @Nonnull void apply(@Nonnull Context ctx, @Nonnull Throwable cause,
      @Nonnull StatusCode statusCode);

  /**
   * Chain this error handler with next and produces a new error handler.
   *
   * @param next Next error handler.
   * @return A new error handler.
   */
  @Nonnull default ErrorHandler then(@Nonnull ErrorHandler next) {
    return (ctx, cause, statusCode) -> {
      apply(ctx, cause, statusCode);
      if (!ctx.isResponseStarted()) {
        next.apply(ctx, cause, statusCode);
      }
    };
  }

  /**
   * Build a line error message that describe the current web context and the status code.
   *
   * <pre>GET /path Status-Code Status-Reason</pre>
   *
   * @param ctx Web context.
   * @param statusCode Status code.
   * @return Single line message.
   */
  static @Nonnull String errorMessage(@Nonnull Context ctx, @Nonnull StatusCode statusCode) {
    return new StringBuilder()
        .append(ctx.getMethod())
        .append(" ")
        .append(ctx.getRequestPath())
        .append(" ")
        .append(statusCode.value())
        .append(" ")
        .append(statusCode.reason())
        .toString();
  }
}
