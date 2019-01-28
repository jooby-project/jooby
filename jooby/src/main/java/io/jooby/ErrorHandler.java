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

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static io.jooby.MediaType.html;
import static io.jooby.MediaType.json;
import static java.lang.String.format;

public interface ErrorHandler {

  static ErrorHandler log(Logger log, StatusCode... quiet) {
    Set<StatusCode> silent = new HashSet<>(Arrays.asList(quiet));
    return (ctx, cause, statusCode) -> {
      String msg = new StringBuilder()
          .append(ctx.method())
          .append(" ")
          .append(ctx.pathString())
          .append(" ")
          .append(statusCode.value())
          .append(" ")
          .append(statusCode.reason())
          .toString();
      if (silent.contains(statusCode)) {
        log.debug(msg, cause);
      } else {
        log.error(msg, cause);
      }
    };
  }

  ErrorHandler DEFAULT = (ctx, cause, statusCode) -> {
    new ContentNegotiation()
        .accept(json, () -> {
          String message = Optional.ofNullable(cause.getMessage()).orElse(statusCode.reason());
          return ctx.responseType(json)
              .statusCode(statusCode)
              .sendString("{\"message\":\"" + message + "\",\"statusCode\":" + statusCode.value()
                  + ",\"reason\":\"" + statusCode.reason() + "\"}");
        })
        .accept(html, () -> {
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
            html.append("<h2>message: ").append(message).append("</h2>\n");
          }
          html.append("<h2>status code: ").append(statusCode.value()).append("</h2>\n");

          html.append("</body>\n")
              .append("</html>");

          return ctx
              .responseType(MediaType.html)
              .statusCode(statusCode)
              .sendString(html.toString());
        }).render(ctx);
  };

  @Nonnull void apply(@Nonnull Context ctx, @Nonnull Throwable cause,
      @Nonnull StatusCode statusCode);

  @Nonnull default ErrorHandler then(@Nonnull ErrorHandler next) {
    return (ctx, cause, statusCode) -> {
      apply(ctx, cause, statusCode);
      if (!ctx.isResponseStarted()) {
        next.apply(ctx, cause, statusCode);
      }
    };
  }
}
