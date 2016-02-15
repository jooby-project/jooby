package org.jooby.jooq;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

public class DSConnectionProvider implements ConnectionProvider {

  private Provider<DataSource> ds;

  public DSConnectionProvider(final Provider<DataSource> ds) {
    this.ds = ds;
  }

  @Override
  public Connection acquire() {
    try {
      return ds.get().getConnection();
    } catch (SQLException ex) {
      throw new DataAccessException("Error getting connection from data source", ex);
    }
  }

  @Override
  public void release(final Connection connection) {
    try {
      connection.close();
    } catch (SQLException ex) {
      throw new DataAccessException("Error closing connection " + connection, ex);
    }
  }
}
