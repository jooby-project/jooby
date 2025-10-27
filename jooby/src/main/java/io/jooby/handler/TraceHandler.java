/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Route;
import io.jooby.Router;

/**
 * The TRACE method performs a message loop-back test along the path to the target resource.
 *
 * @author edgar
 * @since 2.0.4
 */
public class TraceHandler implements Route.Filter {
  private static final String CRLF = "\r\n";

  /** Default constructor. */
  public TraceHandler() {}

  @Override
  public Route.Handler apply(@NonNull Route.Handler next) {
    return ctx -> {
      if (ctx.getMethod().equals(Router.TRACE)) {
        // Handle trace
        StringBuilder buffer =
            new StringBuilder(Router.TRACE)
                .append(" ")
                .append(ctx.getRequestPath())
                .append(" ")
                .append(ctx.getProtocol());

        for (Map.Entry<String, List<String>> entry : ctx.header().toMultimap().entrySet()) {
          buffer
              .append(CRLF)
              .append(entry.getKey())
              .append(": ")
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

  @NonNull @Override
  public void setRoute(@NonNull Route route) {
    route.setHttpTrace(true);
  }
}
