package io.jooby;

import org.objectweb.asm.util.ASMifier;

import java.io.IOException;

public class ByteCodePrinter {
  public static void main(String[] args) throws IOException {
    ASMifier.main(new String[] {"io.jooby.internal.mvc.KotlinMvc"});
  }
}
