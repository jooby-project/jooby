package org.jooby.filewatcher;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.junit.Test;

public class WatchEventKindTest {

  @Test
  public void watchEvent() {
    WatchEventKind kind = new WatchEventKind("foo");
    assertEquals("FOO", kind.name());
    assertEquals("FOO", kind.toString());
    assertEquals(Path.class, kind.type());
  }
}
