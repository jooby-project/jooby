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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public interface Router {

  interface Match {
    boolean matches();

    Route route();

    void execute(Context context);

    Map<String, String> pathMap();
  }

  /** HTTP Methods: */
  String GET = "GET";
  String POST = "POST";
  String PUT = "PUT";
  String DELETE = "DELETE";
  String PATCH = "PATCH";
  String HEAD = "HEAD";
  String CONNECT = "CONNECT";
  String OPTIONS = "OPTIONS";
  String TRACE = "TRACE";

  /** HTTP Methods: */
  List<String> METHODS = List.of(GET, POST, PUT, DELETE, PATCH, HEAD, CONNECT, OPTIONS, TRACE);

  @Nonnull Router caseSensitive(boolean caseSensitive);

  @Nonnull Router ignoreTrailingSlash(boolean ignoreTrailingSlash);

  @Nonnull Router basePath(@Nonnull String basePath);

  @Nonnull String basePath();

  @Nonnull Router use(@Nonnull Predicate<Context> predicate, @Nonnull Router router);

  @Nonnull Router use(@Nonnull String path, @Nonnull Router router);

  @Nonnull Router use(@Nonnull Router router);

  @Nonnull List<Route> routes();

  @Nonnull Router renderer(@Nonnull Renderer renderer);

  @Nonnull Router renderer(@Nonnull MediaType contentType, @Nonnull Renderer renderer);

  @Nonnull Path tmpdir();

  @Nonnull Router parser(@Nonnull MediaType contentType, @Nonnull Parser parser);

  @Nonnull Executor worker();

  @Nonnull Router worker(Executor worker);

  @Nonnull Router decorate(@Nonnull Route.Decorator decorator);

  @Nonnull Router before(@Nonnull Route.Before before);

  @Nonnull Router after(@Nonnull Route.After after);

  @Nonnull Router group(@Nonnull Runnable action);

  @Nonnull Router group(@Nonnull String pattern, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Executor executor, @Nonnull Runnable action);

  @Nonnull Router group(@Nonnull Executor executor, @Nonnull String pattern,
      @Nonnull Runnable action);

  @Nonnull default Route get(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(GET, pattern, handler);
  }

  @Nonnull default Route post(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(POST, pattern, handler);
  }

  @Nonnull default Route put(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(PUT, pattern, handler);
  }

  @Nonnull default Route delete(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(DELETE, pattern, handler);
  }

  @Nonnull default Route patch(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(PATCH, pattern, handler);
  }

  @Nonnull default Route head(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(HEAD, pattern, handler);
  }

  @Nonnull default Route connect(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(CONNECT, pattern, handler);
  }

  @Nonnull default Route options(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(OPTIONS, pattern, handler);
  }

  @Nonnull default Route trace(@Nonnull String pattern, @Nonnull Route.Handler handler) {
    return route(TRACE, pattern, handler);
  }

  default @Nonnull Route assets(@Nonnull String pattern, @Nonnull Path source) {
    return assets(pattern, AssetSource.create(source));
  }

  default @Nonnull Route assets(@Nonnull String pattern, @Nonnull String source) {
    return assets(pattern, AssetSource.create(getClass().getClassLoader(), source));
  }

  default @Nonnull Route assets(@Nonnull String pattern, @Nonnull AssetSource... source) {
    return assets(pattern, new AssetHandler(source));
  }

  default @Nonnull Route assets(@Nonnull String pattern, @Nonnull AssetHandler handler) {
    return route(GET, pattern, handler);
  }

  @Nonnull Route route(@Nonnull String method, @Nonnull String pattern, @Nonnull
      Route.Handler handler);

  /**
   * Find a matching route using the method name and path. Please note that method name must be in
   * uppercase (GET, POST, etc.).
   *
   * If no match exists this method returns a route with a <code>404</code> handler.
   * See {@link Route.Handler#NOT_FOUND}.
   *
   * @param ctx Web Context.
   * @return A route.
   */
  @Nonnull Match match(@Nonnull Context ctx);

  /* Error handler: */
  @Nonnull Router errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode);

  @Nonnull StatusCode errorCode(@Nonnull Throwable cause);

  @Nonnull
  default Router error(@Nonnull StatusCode statusCode, @Nonnull ErrorHandler handler) {
    return error(statusCode::equals, handler);
  }

  @Nonnull
  default Router error(@Nonnull Class<? extends Throwable> type,
      @Nonnull ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (type.isInstance(x) || type.isInstance(x.getCause())) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull
  default Router error(@Nonnull Predicate<StatusCode> predicate,
      @Nonnull ErrorHandler handler) {
    return error((ctx, x, statusCode) -> {
      if (predicate.test(statusCode)) {
        handler.apply(ctx, x, statusCode);
      }
    });
  }

  @Nonnull Router error(@Nonnull ErrorHandler handler);

  @Nonnull ErrorHandler errorHandler();

  Logger log();

  static String normalizePath(@Nonnull String path, boolean caseSensitive,
      boolean ignoreTrailingSlash) {
    if (path == null || path.length() == 0 || path.equals("/")) {
      return "/";
    }
    boolean modified = false;
    StringBuilder buff = new StringBuilder(path.length());
    if (path.charAt(0) != '/') {
      buff.append('/');
      modified = true;
    }
    char prev = Character.MIN_VALUE;
    for (int i = 0; i < path.length(); i++) {
      char ch = path.charAt(i);
      if (ch != '/') {
        if (caseSensitive) {
          buff.append(ch);
        } else {
          char low = Character.toLowerCase(ch);
          if (low != ch) {
            modified = true;
          }
          buff.append(low);
        }
      } else if (prev != '/') {
        buff.append(ch);
      } else {
        modified = true;
      }
      prev = ch;
    }
    if (buff.length() > 1 && buff.charAt(buff.length() - 1) == '/' && ignoreTrailingSlash) {
      buff.setLength(buff.length() - 1);
      modified = true;
    }
    // creates string?
    if (modified) {
      return buff.toString();
    }
    return path;
  }

  static List<String> pathKeys(String pattern) {
    List<String> result = new ArrayList<>();
    int start = -1;
    int end = Integer.MAX_VALUE;
    int len = pattern.length();
    for (int i = 0; i < len; i++) {
      char ch = pattern.charAt(i);
      switch (ch) {
        case '{': {
          start = i + 1;
          end = Integer.MAX_VALUE;
        }
        break;
        case ':': {
          end = i;
        }
        break;
        case '}': {
          String id = pattern.substring(start, Math.min(i, end));
          result.add(id);
          start = -1;
          end = Integer.MAX_VALUE;
        }
        break;
        case '*': {
          if (i == len - 1) {
            result.add("*");
          } else {
            result.add(pattern.substring(i + 1));
          }
          i = len;
        }
        break;
      }
    }
    switch (result.size()) {
      case 0:
        return Collections.emptyList();
      case 1:
        return Collections.singletonList(result.get(0));
      default:
        return Collections.unmodifiableList(result);
    }
  }
}
