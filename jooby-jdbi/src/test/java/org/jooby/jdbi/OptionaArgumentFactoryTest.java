package org.jooby.jdbi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Optional;

import org.jooby.MockUnit;
import org.junit.Test;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;

public class OptionaArgumentFactoryTest {

  @Test
  public void accepts() {
    assertTrue(new OptionalArgumentFactory().accepts(Optional.class, Optional.empty(), null));
    assertFalse(new OptionalArgumentFactory().accepts(Optional.class, new Object(), null));
  }

  @Test
  public void empty() throws Exception {
    new MockUnit(StatementContext.class, PreparedStatement.class)
        .expect(unit -> {
          PreparedStatement stmt = unit.get(PreparedStatement.class);
          stmt.setNull(1, Types.OTHER);
        })
        .run(unit -> {
          Argument arg = new OptionalArgumentFactory()
              .build(Optional.class, Optional.empty(), unit.get(StatementContext.class));
          assertNotNull(arg);
          arg.apply(1, unit.get(PreparedStatement.class), unit.get(StatementContext.class));
        });
  }

  @Test
  public void value() throws Exception {
    new MockUnit(StatementContext.class, PreparedStatement.class)
        .expect(unit -> {
          PreparedStatement stmt = unit.get(PreparedStatement.class);
          stmt.setObject(1, "x");
        })
        .run(unit -> {
          Argument arg = new OptionalArgumentFactory()
              .build(Optional.class, Optional.of("x"), unit.get(StatementContext.class));
          assertNotNull(arg);
          arg.apply(1, unit.get(PreparedStatement.class), unit.get(StatementContext.class));
        });
  }

}
