package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BuiltinRendererTest {

  @Test
  public void values() {
    assertEquals(5, BuiltinRenderer.values().length);
  }

  @Test
  public void bytesValueOf() {
    assertEquals(BuiltinRenderer.Bytes, BuiltinRenderer.valueOf("Bytes"));
  }

}
