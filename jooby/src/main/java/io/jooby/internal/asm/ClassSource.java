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
package io.jooby.internal.asm;

import io.jooby.Throwing;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClassSource {
  private Map<String, Object> bytecode = new HashMap<>();
  private ClassLoader loader;

  public ClassSource(ClassLoader loader) {
    this.loader = loader;
  }

  public byte[] byteCode(Class source) {
    return (byte[]) bytecode.computeIfAbsent(source.getName(), k -> {
      try (InputStream in = loader.getResourceAsStream(k.replace(".", "/") + ".class")) {
        return IOUtils.toByteArray(in);
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    });
  }

  public void destroy() {
    bytecode.clear();
    bytecode = null;
    loader = null;
  }
}
