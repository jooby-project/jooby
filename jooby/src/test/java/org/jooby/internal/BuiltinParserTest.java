package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BuiltinParserTest {

  @Test
  public void values() {
    assertEquals(5, BuiltinParser.values().length);
  }

  @Test
  public void bytesValueOf() {
    assertEquals(BuiltinParser.Bytes, BuiltinParser.valueOf("Bytes"));
  }

}
