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

interface RadixTree {
  void insert(String method, String pattern, Route route);

  boolean find(String method, String path);

  RouterMatch find(Context context, String path, MessageEncoder encoder);

  default RadixTree options(boolean ignoreCase, boolean ignoreTrailingSlash) {
    return new RadixTree() {

      @Override public void insert(String method, String pattern, Route route) {
        RadixTree.this.insert(method, pattern, route);
      }

      @Override public boolean find(String method, String path) {
        return RadixTree.this
            .find(method, Router.normalizePath(path, ignoreCase, ignoreTrailingSlash));
      }

      @Override public RouterMatch find(Context context, String path, MessageEncoder encoder) {
        return RadixTree.this
            .find(context, Router.normalizePath(path, ignoreCase, ignoreTrailingSlash), encoder);
      }

      @Override public void destroy() {
        RadixTree.this.destroy();
      }
    };
  }

  void destroy();
}
