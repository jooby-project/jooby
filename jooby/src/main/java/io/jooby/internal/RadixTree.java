/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

import java.util.List;
import java.util.function.Predicate;

interface RadixTree {
  void insert(String method, String pattern, Route route);

  RouterMatch find(Context context, String path, MessageEncoder encoder, List<RadixTree> more);

  default RadixTree with(Predicate<Context> predicate) {
    return new RadixTree() {
      @Override public void insert(String method, String pattern, Route route) {
        RadixTree.this.insert(method, pattern, route);
      }

      @Override
      public RouterMatch find(Context context, String path, MessageEncoder encoder,
          List<RadixTree> more) {
        if (!predicate.test(context)) {
          return new RouterMatch()
              .missing(context.getMethod(), context.pathString(), encoder);
        }
        return RadixTree.this.find(context, path, encoder, more);
      }

      @Override public void destroy() {
        RadixTree.this.destroy();
      }
    };
  }

  default RadixTree options(boolean ignoreCase, boolean ignoreTrailingSlash) {
    return new RadixTree() {
      @Override public void insert(String method, String pattern, Route route) {
        RadixTree.this.insert(method, pattern, route);
      }

      @Override public RouterMatch find(Context context, String path, MessageEncoder encoder,
          List<RadixTree> more) {
        return RadixTree.this
            .find(context, Router.normalizePath(path, ignoreCase, ignoreTrailingSlash), encoder,
                more);
      }

      @Override public void destroy() {
        RadixTree.this.destroy();
      }
    };
  }

  void destroy();
}
