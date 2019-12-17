package starter.domain;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {

  public static class Mapper implements RowMapper<User> {
    @Override public User map(final ResultSet rs, final StatementContext ctx) throws SQLException {
      return new User(rs.getLong("id"), rs.getString("username"), rs.getString("password"));
    }
  }

  private long id;

  private String username;

  private String password;

  public User(long id, String username, String password) {
    this.id = id;
    this.username = username;
    this.password = password;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
