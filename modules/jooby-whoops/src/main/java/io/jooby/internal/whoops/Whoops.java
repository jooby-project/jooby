/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.attributes.methodaccess.NoOpMethodAccessValidator;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import io.jooby.Context;
import io.jooby.ErrorHandler;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.Session;
import io.jooby.StatusCode;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static io.jooby.ErrorHandler.errorMessage;
import static io.jooby.internal.whoops.Utils.mapOf;
import static io.jooby.internal.whoops.Utils.multimap;

public class Whoops implements ErrorHandler {

  public static class Result {
    private boolean skip;
    private String html;
    private Throwable cause;

    public Result(boolean skip) {
      this.skip = skip;
    }

    public static Result skip() {
      return new Result(true);
    }

    public static Result success(String html) {
      return new Result(false).html(html);
    }

    public static Result failure(Throwable cause) {
      return new Result(false).cause(cause);
    }

    public void handle(BiConsumer<String, Throwable> consumer) {
      if (!skip) {
        consumer.accept(html, cause);
      }
    }

    private Result cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    private Result html(String html) {
      this.html = html;
      return this;
    }
  }

  private final PebbleEngine engine;

  private final SourceLocator locator;

  private final Logger log;

  public Whoops(Path basedir, Logger log) {
    this.engine = engine();
    this.locator = new SourceLocator(basedir);
    this.log = log;
  }

  @Nonnull @Override public void apply(@Nonnull Context ctx,
      @Nonnull Throwable cause, @Nonnull StatusCode code) {
    if (ctx.accept(MediaType.html)) {
      render(ctx, cause, code).handle((html, failure) -> {
        if (failure == null) {
          // Handle the exception
          log.error(errorMessage(ctx, code), cause);
          ctx.setResponseType(MediaType.html)
              .setResponseCode(code)
              .send(html);
        } else {
          // most probably its a bug in whoops: log it and move to next error handler
          log.error("whoops resulted in exception", failure);
        }
      });
    }
  }

  public Result render(Context ctx, Throwable cause, StatusCode code) {
    try {
      List<Frame> frames = Frame.toFrames(locator, cause);

      if (frames.isEmpty()) {
        return Result.skip();
      }
      StringWriter stacktrace = new StringWriter();
      cause.printStackTrace(new PrintWriter(stacktrace));

      Map<String, Object> model = new HashMap<>();
      String cpath = ctx.getContextPath();
      if (cpath.equals("/")) {
        cpath = "";
      }
      model.put("stylesheet", cpath + "/whoops/css/whoops.base.css");
      model.put("prettify", cpath + "/whoops/js/prettify.min.js");
      model.put("clipboard", cpath + "/whoops/js/clipboard.min.js");
      model.put("zepto", cpath + "/whoops/js/zepto.min.js");
      model.put("javascript", cpath + "/whoops/js/whoops.base.js");

      model.put("frames", frames);
      model.put("cause", cause);
      model.put("causeName", Arrays.asList(cause.getClass().getName().split("\\.")));
      model.put("stacktrace", stacktrace.toString());
      model.put("code", code);

      // environment
      model.put("env", environment(ctx, code));

      StringWriter writer = new StringWriter();
      engine.getTemplate("layout").evaluate(writer, model);
      return Result.success(writer.toString());
    } catch (Exception x) {
      return Result.failure(x);
    }
  }

  private Map<String, Object> environment(Context ctx, StatusCode statusCode) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("Response", mapOf("code", statusCode));

    map.put("Request", mapOf(
        "path", ctx.getRequestPath(),
        "headers", multimap(ctx.queryMultimap()),
        "path", ctx.pathMap(),
        "query", multimap(ctx.queryMultimap()),
        "form", multimap(ctx.multipartMultimap()),
        "attributes", ctx.getAttributes()
        )
    );
    map.put("Session", Optional.ofNullable(ctx.sessionOrNull()).map(Session::toMap).orElse(
        Collections.emptyMap()));

    Route route = ctx.getRoute();
    map.put("Route", mapOf(
        "method", route.getMethod(),
        "pattern", route.getPattern(),
        "attributes", route.getAttributes(),
        "consumes", route.getConsumes(),
        "produces", route.getProduces()
        )
    );
    return map;
  }

  static PebbleEngine engine() {
    ClasspathLoader loader = new ClasspathLoader();
    loader.setPrefix("io/jooby/whoops/views");
    loader.setSuffix(".html");
    return new PebbleEngine.Builder()
        .methodAccessValidator(new NoOpMethodAccessValidator())
        .loader(loader)
        .build();
  }
}
