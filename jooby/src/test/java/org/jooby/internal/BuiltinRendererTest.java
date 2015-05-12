package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.io.Closeable;

import org.jooby.MockUnit;
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

  @Test
  public void close() throws Exception {
    new MockUnit(Closeable.class)
        .expect(unit -> {
          Closeable closeable = unit.get(Closeable.class);
          closeable.close();
        })
        .run(unit -> {
          BuiltinRenderer.close(unit.get(Closeable.class));
        });
  }

  @Test
  public void closeIgnored() throws Exception {
    new MockUnit()
        .run(unit -> {
          BuiltinRenderer.close(new Object());
        });
  }

}
