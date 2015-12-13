package org.jooby.internal.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StoredIdTest {

  @Test
  public void defs() {
    StoredId storedId = new StoredId();
    assertEquals("", storedId.className);
    assertEquals(1L, storedId.value.longValue());
  }

  @Test
  public void named() {
    StoredId storedId = new StoredId("X");
    assertEquals("X", storedId.className);
    assertEquals(1L, storedId.value.longValue());
  }

}
