package org.jooby.filewatcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WatchEventModifierTest {

  @Test
  public void watchEventModifier() {
    WatchEventModifier mod = new WatchEventModifier("foo");
    assertEquals("FOO", mod.name());
    assertEquals("FOO", mod.toString());
  }
}
