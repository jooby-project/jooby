/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import com.squareup.javapoet.JavaFile;
import io.jooby.internal.newapt.ConsoleMessager;

public class TestMvcSourceCodeProcessor extends MvcSourceCodeProcessor {
  private JavaFile source;

  public TestMvcSourceCodeProcessor() {
    super(new ConsoleMessager());
  }

  public MvcClassLoader createClassLoader() {
    // Objects.requireNonNull(source);
    return new MvcClassLoader(getClass().getClassLoader(), source);
  }

  public JavaFile getSource() {
    return source;
  }

  @Override
  protected void onGeneratedSource(JavaFile source) {
    this.source = source;
  }
}
