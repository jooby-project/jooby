package org.jooby.filewatcher;

import static org.junit.Assert.assertEquals;

import org.jooby.spi.WatchEventModifier;
import org.junit.Test;

import java.nio.file.WatchEvent;

public class WatchEventModifierTest {

  @Test
  public void watchEventModifier() {
    WatchEvent.Modifier mod = WatchEventModifier.modifier("FOO");
    assertEquals("FOO", mod.name());
  }
}
