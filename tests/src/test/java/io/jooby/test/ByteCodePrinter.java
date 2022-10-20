/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.io.IOException;

import org.objectweb.asm.util.ASMifier;

public class ByteCodePrinter {
  public static void main(String[] args) throws IOException {
    ASMifier.main(new String[] {"io.jooby.internal.mvc.KotlinMvc"});
  }
}
