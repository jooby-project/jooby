package org.jooby.quartz;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.quartz.utils.ConnectionProvider;

public class QuartzConnectionProvider implements ConnectionProvider {

  private Provider<DataSource> ds;

  public QuartzConnectionProvider(final Provider<DataSource> ds) {
    this.ds = requireNonNull(ds, "Data source is required.");
  }

  @Override
  public Connection getConnection() throws SQLException {
    return ds.get().getConnection();
  }

  @Override
  public void shutdown() throws SQLException {
    // NOOP
  }

  @Override
  public void initialize() throws SQLException {
    // NOOP
  }

}
