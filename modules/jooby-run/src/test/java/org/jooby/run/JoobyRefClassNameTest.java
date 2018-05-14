package org.jooby.run;

import static org.junit.Assert.assertEquals;

import org.jooby.internal.run__.JoobyRef;
import org.junit.Test;

public class JoobyRefClassNameTest {

  @Test
  public void classNameShouldBeEq() {
    assertEquals(JoobyRef.class.getName(), Main.JOOBY_REF);
  }
}
