/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.asm;

import io.jooby.Sneaky;
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

  public ClassLoader getLoader() {
    return loader;
  }

  public byte[] byteCode(Class source) {
    return (byte[]) bytecode.computeIfAbsent(source.getName(), k -> {
      try (InputStream in = loader.getResourceAsStream(k.replace(".", "/") + ".class")) {
        return IOUtils.toByteArray(in);
      } catch (IOException x) {
        throw Sneaky.propagate(x);
      }
    });
  }

  public void destroy() {
    bytecode.clear();
    bytecode = null;
    loader = null;
  }
}
