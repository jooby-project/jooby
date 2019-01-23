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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Route {

  public interface Aware {
    void setRoute(Route route);
  }

  public interface Decorator {
    @Nonnull Handler apply(@Nonnull Handler next);

    @Nonnull default Decorator then(@Nonnull Decorator next) {
      return h -> apply(next.apply(h));
    }

    @Nonnull default Handler then(@Nonnull Handler next) {
      return ctx -> apply(next).apply(ctx);
    }
  }

  public interface Before extends Decorator {
    @Nonnull @Override default Handler apply(@Nonnull Handler next) {
      return ctx -> {
        before(ctx);
        return next.apply(ctx);
      };
    }

    void before(@Nonnull Context ctx) throws Exception;
  }

  public interface After {

    @Nonnull default After then(@Nonnull After next) {
      return (ctx, result) -> apply(ctx, next.apply(ctx, result));
    }

    @Nonnull Object apply(@Nonnull Context ctx, Object result) throws Exception;
  }

  public interface Handler extends Serializable {

    @Nonnull Object apply(@Nonnull Context ctx) throws Exception;

    @Nonnull default Object execute(@Nonnull Context ctx) {
      try {
        return apply(ctx);
      } catch (Throwable x) {
        ctx.sendError(x);
        return x;
      }
    }

    @Nonnull default Handler then(After next) {
      return ctx -> next.apply(ctx, apply(ctx));
    }
  }

  public static final Handler NOT_FOUND = ctx -> ctx.sendError(new Err(StatusCode.NOT_FOUND));

  public static final Handler METHOD_NOT_ALLOWED = ctx -> ctx.sendError(new Err(StatusCode.METHOD_NOT_ALLOWED));

  public static final Handler FAVICON = ctx -> ctx.sendStatusCode(StatusCode.NOT_FOUND);

  private final Map<String, Parser> parsers;

  private String pattern;

  private String method;

  private List<String> pathKeys;

  private Handler handler;

  private Handler pipeline;

  private Renderer renderer;

  private Type returnType;

  public Route(@Nonnull String method, @Nonnull String pattern, @Nonnull List<String> pathKeys,
      @Nonnull Type returnType, @Nonnull Handler handler, @Nonnull Handler pipeline,
      @Nonnull Renderer renderer, @Nonnull Map<String, Parser> parsers) {
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.returnType = returnType;
    this.handler = handler;
    this.pipeline = pipeline;
    this.renderer = renderer;
    this.pathKeys = pathKeys;
    this.parsers = parsers;
  }

  public Route(@Nonnull String method, @Nonnull String pattern, @Nonnull Type returnType,
      @Nonnull Handler handler, @Nonnull Handler pipeline, @Nonnull Renderer renderer,
      @Nonnull Map<String, Parser> parsers) {
    this(method, pattern, Router.pathKeys(pattern), returnType, handler, pipeline, renderer, parsers);
  }

  public @Nonnull String pattern() {
    return pattern;
  }

  public  @Nonnull String method() {
    return method;
  }

  public @Nonnull List<String> pathKeys() {
    return pathKeys;
  }

  public @Nonnull Handler handler() {
    return handler;
  }

  public @Nonnull Handler pipeline() {
    return pipeline;
  }

  public @Nonnull Route pipeline(Route.Handler pipeline) {
    this.pipeline = pipeline;
    return this;
  }

  public @Nonnull Renderer renderer() {
    return renderer;
  }

  public @Nonnull Type returnType() {
    return returnType;
  }

  public @Nonnull Route returnType(@Nonnull Type returnType) {
    this.returnType = returnType;
    return this;
  }

  public @Nonnull Parser parser(MediaType contentType) {
    return parsers.getOrDefault(contentType.value(), Parser.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override public String toString() {
    return method + " " + pattern;
  }
}
