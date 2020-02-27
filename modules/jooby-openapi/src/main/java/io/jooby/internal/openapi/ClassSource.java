package io.jooby.internal.openapi;

import io.jooby.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class ClassSource {
  private ClassLoader loader;

  public ClassSource(ClassLoader loader) {
    this.loader = loader;
  }

  public byte[] byteCode(String classname) {
    try (InputStream stream = loader.getResourceAsStream(classname.replace(".", "/") + ".class")) {
      if (stream == null) {
        throw new ClassNotFoundException(classname);
      }
      return IOUtils.toByteArray(stream);
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
