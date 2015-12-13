package org.jooby.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IdGenTest {

  @Test
  public void global() {
    assertEquals("Global", IdGen.GLOBAL.value(getClass()));
  }

  @Test
  public void local() {
    assertEquals("org.jooby.mongodb.IdGenTest", IdGen.LOCAL.value(getClass()));
  }

  @Test
  public void enums() {
    assertEquals(2, IdGen.values().length);
    assertEquals(IdGen.GLOBAL, IdGen.valueOf("GLOBAL"));
  }

}
