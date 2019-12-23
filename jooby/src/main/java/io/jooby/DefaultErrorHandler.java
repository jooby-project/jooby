/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.jooby.MediaType.html;
import static io.jooby.MediaType.json;
import static io.jooby.MediaType.text;

/**
 * Default error handler with content negotiation support and optionally mute log statement base
 * on status code or exception types.
 *
 * @author edgar
 * @since 2.4.1
 */
public class DefaultErrorHandler implements ErrorHandler {

  private Set<StatusCode> muteCodes = new HashSet<>();

  private Set<Class> muteTypes = new HashSet<>();

  /**
   * Generate a log.debug call if any of the status code error occurs as exception.
   *
   * @param statusCodes Status codes to mute.
   * @return This error handler.
   */
  public @Nonnull DefaultErrorHandler mute(@Nonnull StatusCode... statusCodes) {
    Stream.of(statusCodes).forEach(muteCodes::add);
    return this;
  }

  /**
   * Generate a log.debug call if any of the exception types occurs.
   *
   * @param exceptionTypes Exception types to mute.
   * @return This error handler.
   */
  public @Nonnull DefaultErrorHandler mute(@Nonnull Class<? extends Exception>... exceptionTypes) {
    Stream.of(exceptionTypes).forEach(muteTypes::add);
    return this;
  }

  @Nonnull @Override public void apply(@Nonnull Context ctx, @Nonnull Throwable cause,
      @Nonnull StatusCode statusCode) {
    Logger log = ctx.getRouter().getLog();
    if (mute(cause, statusCode)) {
      log.debug(ErrorHandler.errorMessage(ctx, statusCode), cause);
    } else {
      log.error(ErrorHandler.errorMessage(ctx, statusCode), cause);
    }

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
  }

  private boolean mute(Throwable cause, StatusCode statusCode) {
    return muteCodes.contains(statusCode)
        // same class filter
        || muteTypes.stream().anyMatch(type -> type == cause.getClass())
        // sub-class filter
        || muteTypes.stream().anyMatch(type -> type.isInstance(cause));
  }
}
