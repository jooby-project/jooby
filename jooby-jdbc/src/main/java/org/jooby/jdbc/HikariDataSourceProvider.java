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
package org.jooby.jdbc;

import static com.google.common.base.Preconditions.checkState;

import javax.inject.Provider;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class HikariDataSourceProvider implements Provider<DataSource> {

  private HikariDataSource dataSource;

  private HikariConfig config;

  public HikariDataSourceProvider(final HikariConfig config) {
    this.config = config;
  }

  public HikariConfig config() {
    return config;
  }

  public void start() {
    checkState(dataSource == null, "start can't be called it twice");
    dataSource = new HikariDataSource(config);
  }

  @Override
  public DataSource get() {
    return dataSource;
  }

  public void shutdown() throws Exception {
    if (dataSource != null) {
      dataSource.shutdown();
      dataSource = null;
    }
  }

  @Override
  public final String toString() {
    return config.getDataSourceProperties().getProperty("url");
  }
}
