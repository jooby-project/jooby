package org.jooby.quartz;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;

public class JdbcStore extends JobStoreSupport {

  private DataSource ds;

  public JdbcStore(final DataSource ds) {
    this.ds = requireNonNull(ds, "Data source is required.");
  }

  @Override
  protected Connection getNonManagedTXConnection() throws JobPersistenceException {
    try {
      return ds.getConnection();
    } catch (SQLException ex) {
      throw new JobPersistenceException("Can't acquire connection", ex);
    }
  }

  @Override
  protected <T> T executeInLock(final String lockName, final TransactionCallback<T> txCallback)
      throws JobPersistenceException {
    return executeInNonManagedTXLock(lockName, txCallback, null);
  }

}
