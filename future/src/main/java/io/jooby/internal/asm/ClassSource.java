package io.jooby.internal.asm;

import org.jooby.funzy.Throwing;

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
        return in.readAllBytes();
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    });
  }

  public void destroy() {
    bytecode.clear();
  }
}
