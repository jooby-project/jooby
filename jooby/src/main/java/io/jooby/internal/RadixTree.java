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
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Renderer;

import java.util.List;
import java.util.function.Predicate;

interface RadixTree {
  void insert(String method, String pattern, RouteImpl route);

  RouterMatch find(Context context, Renderer renderer, List<RadixTree> more);

  default RadixTree with(Predicate<Context> predicate) {
    return new RadixTree() {
      @Override public void insert(String method, String pattern, RouteImpl route) {
        RadixTree.this.insert(method, pattern, route);
      }

      @Override
      public RouterMatch find(Context context, Renderer renderer, List<RadixTree> more) {
        if (predicate != null && !predicate.test(context)) {
          return new RouterMatch()
              .missing(context.method(), context.pathString(), renderer);
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
