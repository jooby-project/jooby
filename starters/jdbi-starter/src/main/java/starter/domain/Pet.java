package starter.domain;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Pet {

  public static class Mapper implements RowMapper<Pet> {
    @Override public Pet map(final ResultSet rs, final StatementContext ctx) throws SQLException {
      return new Pet(rs.getLong("id"), rs.getString("name"));
    }
  }

  private Long id;

  private String name;

  public Pet(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
