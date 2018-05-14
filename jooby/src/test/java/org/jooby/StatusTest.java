package org.jooby;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatusTest {

  @Test
  public void customCode() {
    Status status = Status.valueOf(444);
    assertEquals("444", status.reason());
    assertEquals("444 (444)", status.toString());
    assertEquals(444, status.value());
  }

}
