package org.jooby.assets;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

class AssetClassLoader {

  public static ClassLoader classLoader(final ClassLoader parent) throws IOException {
    requireNonNull(parent, "ClassLoader required.");
    File publicDir = new File("public");
    if (publicDir.exists()) {
      return new URLClassLoader(new URL[]{publicDir.toURI().toURL() }, parent);
    }
    return parent;
  }
}