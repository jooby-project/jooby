/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The TRACE method performs a message loop-back test along the path to the target resource.
 *
 * @author edgar
 * @since 2.0.4
 */
public class TraceHandler implements Route.Decorator {
  private static final String CRLF = "\r\n";

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      if (ctx.getMethod().equals(Router.TRACE)) {
        // Handle trace
        StringBuilder buffer = new StringBuilder(Router.TRACE).append(" ").append(ctx.pathString())
            .append(" ").append(ctx.getProtocol());

        for (Map.Entry<String, List<String>> entry : ctx.headerMultimap().entrySet()) {
          buffer.append(CRLF).append(entry.getKey()).append(": ")
              .append(entry.getValue().stream().collect(Collectors.joining(", ")));
        }

        buffer.append(CRLF);

        ctx.setResponseType("message/http");
        return ctx.send(buffer.toString());
      } else {
        return next.apply(ctx);
      }
    };
  }

  @Nonnull @Override public void setRoute(@Nonnull Route route) {
    route.setHttpTrace(true);
  }
}
