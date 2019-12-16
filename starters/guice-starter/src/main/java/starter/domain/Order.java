package starter.domain;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Order {

  public static class Mapper implements RowMapper<Order> {
    @Override public Order map(final ResultSet rs, final StatementContext ctx) throws SQLException {
      Order order = new Order();
      order.setId(rs.getLong("id"));
      order.setName(rs.getString("name"));
      return order;
    }
  }

  private long id;

  private String name;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
