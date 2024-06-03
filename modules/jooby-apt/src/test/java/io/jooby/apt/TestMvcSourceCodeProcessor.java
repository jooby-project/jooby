/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import com.squareup.javapoet.JavaFile;

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

class ConsoleMessager implements Messager {
  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
    println(kind, msg);
  }

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
    println(kind, msg, e);
  }

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
    println(kind, msg, e, " @", a);
  }

  @Override
  public void printMessage(
      Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
    println(kind, msg, e, " @", a, "=", v);
  }

  private void println(Diagnostic.Kind kind, CharSequence message, Object... args) {
    var out = kind == Diagnostic.Kind.ERROR ? System.err : System.out;
    out.println(
        kind
            + ": "
            + message
            + " "
            + Stream.of(args)
                .filter(Objects::nonNull)
                .map(Objects::toString)
                .collect(Collectors.joining(" ")));
  }
}
