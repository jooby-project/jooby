package org.jooby.jdbi;

import java.sql.Types;
import java.util.Optional;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

class OptionalArgumentFactory implements ArgumentFactory<Optional<?>> {

  @Override
  public boolean accepts(final Class<?> expectedType, final Object value, final StatementContext ctx) {
    return value instanceof Optional;
  }

  @Override
  public Argument build(final Class<?> expectedType, final Optional<?> value,
      final StatementContext sctx) {
    return (pos, stmt, ctx) -> {
      if (value.isPresent()) {
        stmt.setObject(pos, value.get());
      } else {
        stmt.setNull(pos, Types.OTHER);
      }
    };
  }

}
