/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.ebean;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.sql.DataSource;

public class ForwardingDataSource implements DataSource {

  private Provider<DataSource> ds;

  public ForwardingDataSource(final Provider<DataSource> ds) {
    this.ds = ds;
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return ds.get().getLogWriter();
  }

  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    ds.get().setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    ds.get().setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return ds.get().getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return ds.get().getParentLogger();
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return ds.get().unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return ds.get().isWrapperFor(iface);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return ds.get().getConnection();
  }

  @Override
  public Connection getConnection(final String username, final String password)
      throws SQLException {
    return ds.get().getConnection(username, password);
  }

}
