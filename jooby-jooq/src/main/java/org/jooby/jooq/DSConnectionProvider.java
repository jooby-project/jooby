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
