package org.jooby.couchbase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class N1QTest {

  @Test
  public void from() {
    new N1Q();
    assertEquals("SELECT N.* FROM `default` N WHERE N._class = \"org.jooby.couchbase.N1QTest\"",
        N1Q.from(N1QTest.class).toString());
  }

  @Test
  public void fromBucket() {
    assertEquals("SELECT N.* FROM `x` N WHERE N._class = \"org.jooby.couchbase.N1QTest\"",
        N1Q.from("x", N1QTest.class).toString());
  }

  @Test
  public void qId() {
    assertEquals("org.jooby.couchbase.N1QTest::1", N1Q.qualifyId(N1QTest.class, 1).toString());
    assertEquals("q::1", N1Q.qualifyId("q", 1).toString());
  }
}
