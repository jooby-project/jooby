package io.jooby;

import javax.annotation.Nonnull;

public interface Route {

  interface Filter {
    @Nonnull Handler apply(@Nonnull Handler next);

    @Nonnull default Filter then(@Nonnull Filter next) {
      return h -> apply(next.apply(h));
    }

    @Nonnull default Handler then(@Nonnull Handler next) {
      return ctx -> apply(next).apply(ctx);
    }
  }

  interface Before extends Filter {
    @Nonnull @Override default Handler apply(@Nonnull Handler next) {
      return ctx -> {
        before(ctx);
        return next.apply(ctx);
      };
    }

    void before(@Nonnull Context ctx) throws Exception;
  }

  interface After {
    @Nonnull default After then(@Nonnull After next) {
      return (ctx, result) -> apply(ctx, next.apply(ctx, result));
    }

    @Nonnull Object apply(@Nonnull Context ctx, Object result) throws Exception;
  }

  interface Handler {

    @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

    default RootHandler root() {
      return ctx -> {
        try {
          Object result = apply(ctx);
          if (!ctx.isResponseStarted() && ctx != result) {
            ctx.send(result);
          }
          return result;
        } catch (Throwable x) {
          ctx.sendError(x);
          return x;
        } finally {
          if (ctx.isResponseStarted()) {
            ctx.destroy();
          }
        }
      };
    }
  }

  interface DetachHandler extends Handler {
    @Nonnull @Override default Object apply(@Nonnull Context ctx) throws Exception {
      handle(ctx);
      return ctx;
    }

    void handle(@Nonnull Context ctx) throws Exception;
  }

  interface RootHandler extends Handler {
    @Nonnull Object apply(@Nonnull Context ctx);

    @Override default RootHandler root() {
      return this;
    }
  }

  interface ErrorHandler {

    ErrorHandler DEFAULT = (ctx, cause, statusCode) -> {
      // TODO: use a log
      cause.printStackTrace();
      String message = cause.getMessage();
      StringBuilder html = new StringBuilder("<!doctype html>\n")
          .append("<html>\n")
          .append("<head>\n")
          //        .append("<meta charset=\"").append(ctx.charset().name()).append("\">\n")
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
          .append("<title>\n")
          .append(statusCode).append(" ").append(statusCode.reason())
          .append("\n</title>\n")
          .append("<body>\n")
          .append("<h1>").append(statusCode.reason()).append("</h1>\n")
          .append("<hr>");

      html.append("<h2>message: ").append(message).append("</h2>\n");
      html.append("<h2>status: ").append(statusCode).append("</h2>\n");

      html.append("</body>\n")
          .append("</html>");

      ctx.statusCode(statusCode)
          .sendText(html.toString());
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

  interface RootErrorHandler {
    void apply(@Nonnull Context ctx, @Nonnull Throwable cause);
  }

  RootHandler NOT_FOUND = ctx -> ctx.sendError(new Err(StatusCode.NOT_FOUND));

  RootHandler METHOD_NOT_ALLOWED = ctx -> ctx.sendError(new Err(StatusCode.METHOD_NOT_ALLOWED));

  RootHandler FAVICON = ctx -> ctx.sendStatusCode(StatusCode.NOT_FOUND);

  String pattern();

  String method();

  Handler handler();

  RootHandler pipeline();

  Renderer renderer();

  Route.After after();
}


