package org.jooby;

import org.junit.Test;

public class StatusTest {

  @Test(expected = IllegalArgumentException.class)
  public void badCode() {
    Status.valueOf(907);
  }
}
