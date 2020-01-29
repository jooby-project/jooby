package io.jooby.openapi;

import examples.RouteIdioms;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.util.ASMifier;

public class ByteCodePrinter {

  @Test
  public void appA() throws Exception {
    ASMifier.main(new String[]{RouteIdioms.class.getName()});
  }

}

