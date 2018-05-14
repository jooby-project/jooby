package org.jooby.flyway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Issue1044 {

  @Test
  public void checkValidFlywayProperty() {
    assertFalse(Flywaydb.isFlywayProperty("run"));
    assertFalse(Flywaydb.isFlywayProperty("cachePrepStmts"));

    assertTrue(Flywaydb.isFlywayProperty("sqlMigrationSuffix"));
    assertTrue(Flywaydb.isFlywayProperty("url"));
    assertTrue(Flywaydb.isFlywayProperty("driver"));
    assertTrue(Flywaydb.isFlywayProperty("user"));
    assertTrue(Flywaydb.isFlywayProperty("password"));
    assertTrue(Flywaydb.isFlywayProperty("locations"));
    assertTrue(Flywaydb.isFlywayProperty("placeholderPrefix"));
  }
}
