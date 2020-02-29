/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class ClassSource {
  private ClassLoader classLoader;

  public ClassSource(ClassLoader loader) {
    this.classLoader = loader;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public byte[] byteCode(String classname) {
    try (InputStream stream = classLoader.getResourceAsStream(classname.replace(".", "/") + ".class")) {
      if (stream == null) {
        throw new ClassNotFoundException(classname);
      }
      return IOUtils.toByteArray(stream);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
