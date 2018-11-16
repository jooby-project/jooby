package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Renderer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

interface RadixTree {
  void insert(String method, String pattern, RouteImpl route);

  RouterMatch find(Context context, Renderer renderer, List<RadixTree> more);

  default RadixTree with(Predicate<Context> predicate) {
    return new RadixTree() {
      @Override public void insert(String method, String pattern, RouteImpl route) {
        RadixTree.this.insert(method, pattern, route);
      }

      @Override public RouterMatch find(Context context, Renderer renderer, List<RadixTree> more) {
        if (predicate != null && !predicate.test(context)) {
          return new RouterMatch()
              .missing(context.executor(), context.method(), context.path(), renderer);
        }
        return RadixTree.this.find(context, renderer, more);
      }

      @Override public void destroy() {
        RadixTree.this.destroy();
      }
    };
  }

  void destroy();
}
