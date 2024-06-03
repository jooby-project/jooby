/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static com.google.testing.compile.Compiler.javac;

import java.io.IOException;
import java.util.List;

import javax.tools.JavaFileObject;

import com.squareup.javapoet.JavaFile;

public class MvcClassLoader extends ClassLoader {
  private final JavaFileObject classFile;
  private final String className;

  public MvcClassLoader(ClassLoader parent, JavaFile source) {
    super(parent);
    this.classFile = javac().compile(List.of(source.toJavaFileObject())).generatedFiles().get(0);
    this.className = source.packageName + "." + source.typeSpec.name;
  }

  public String getClassName() {
    return className;
  }

  protected Class<?> findClass(String name) throws ClassNotFoundException {
    if (name.equals(className)) {
      try (var in = classFile.openInputStream()) {
        var bytes = in.readAllBytes();
        return defineClass(name, bytes, 0, bytes.length);
      } catch (IOException c) {
        return super.findClass(name);
      }
    }
    return super.findClass(name);
  }
}
