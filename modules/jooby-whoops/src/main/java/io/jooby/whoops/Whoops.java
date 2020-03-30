package io.jooby.whoops;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Session;
import io.jooby.StatusCode;
import io.jooby.internal.whoops.Frame;
import io.jooby.internal.whoops.SourceLocator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Whoops {

  public static class Result {
    public final boolean success;

    public final String output;

    public final Throwable failure;

    public Result(boolean success, String output) {
      this.success = success;
      this.output = output;
      this.failure = null;
    }

    public Result(Throwable failure) {
      this.success = false;
      this.output = null;
      this.failure = null;
    }
  }

  private final Handlebars engine;

  private final SourceLocator locator;

  public Whoops(Path basedir) {
    engine = new Handlebars(new ClassPathTemplateLoader("/whoops/views", ".html"))
        .prettyPrint(true);
    engine.registerHelper("minus", (size, options) -> {
      return ((Number) size).intValue() - ((Integer) options.param(0)) - 1;
    });
    engine.registerHelper("empty", (list, options) -> {
      return ((Collection) list).isEmpty();
    });
    this.locator = new SourceLocator(basedir, getClass().getClassLoader());
  }

  public Whoops() {
    this(Paths.get(System.getProperty("user.dir")));
  }

  public Result render(Context ctx, Throwable cause, StatusCode responseCode) {
    try {
      List<Frame> frames = Frame.toFrames(locator, cause);

      if (frames.isEmpty()) {
        return new Result(true, null);
      }
      StringWriter stacktrace = new StringWriter();
      cause.printStackTrace(new PrintWriter(stacktrace));

      Map<String, Object> model = new HashMap<>();
      model.put("stylesheet", "/whoops/css/whoops.base.css");
      model.put("prettify", "/whoops/js/prettify.min.js");
      model.put("clipboard", "/whoops/js/clipboard.min.js");
      model.put("zepto", "/whoops/js/zepto.min.js");
      model.put("javascript", "/whoops/js/whoops.base.js");

      model.put("frames", frames);
      model.put("cause", cause);
      model.put("causeName", Arrays.asList(cause.getClass().getName().split("\\.")));
      model.put("stacktrace", stacktrace.toString());
      model.put("code", responseCode);

      // environment
      model.put("env", environment(ctx, responseCode));

      return new Result(true, engine.compile("layout").apply(com.github.jknack.handlebars.Context
          .newBuilder(model)
          .push(MethodValueResolver.INSTANCE)
          .build()
      ));
    } catch (Exception x) {
      return new Result(x);
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

  private Map<String, Object> multimap(Map<String, List<String>> input) {
    if (input.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> e : input.entrySet()) {
      if (e.getValue().size() == 1) {
        map.put(e.getKey(), e.getValue().get(0));
      } else {
        map.put(e.getKey(), e.getValue());
      }
    }
    return map;
  }

  private Map<String, Object> mapOf(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      Object value = values[i + 1];
      if (!isEmpty(value)) {
        map.put(values[i].toString(), value);
      }
    }
    return map;
  }

  private boolean isEmpty(Object value) {
    if (value instanceof Map) {
      return ((Map) value).isEmpty();
    }
    return Handlebars.Utils.isEmpty(value);
  }

}
