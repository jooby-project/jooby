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
import java.util.LinkedList;

/**
 * Collection of utility functions around EVENT_LOOP, exceptions, etc..
 *
 * @author edgar
 */
public final class Functions {

  public static class Closer implements AutoCloseable {

    private LinkedList<AutoCloseable> stack = new LinkedList<>();

    public Closer register(@Nonnull AutoCloseable closeable) {
      stack.addLast(closeable);
      return this;
    }

    @Override public void close() {
      Throwable root = null;
      while (!stack.isEmpty()) {
        AutoCloseable closeable = stack.removeFirst();
        try {
          closeable.close();
        } catch (Exception x) {
          if (root == null) {
            root = x;
          } else {
            root.addSuppressed(x);
          }
        }
      }
      stack = null;
      if (root != null) {
        throw Throwing.sneakyThrow(root);
      }
    }
  }

  public static Closer closer() {
    return new Closer();
  }
}
