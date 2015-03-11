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

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Managed;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class HikariDataSourceProvider implements Provider<DataSource>, Managed {

  private HikariDataSource dataSource;

  private HikariConfig config;

  public HikariDataSourceProvider(final HikariConfig config) {
    this.config = config;
  }

  public HikariConfig config() {
    return config;
  }

  @Override
  public void start() {
    if (dataSource == null) {
      dataSource = new HikariDataSource(config);
    }
  }

  @Override
  public DataSource get() {
    start();
    return dataSource;
  }

  @Override
  public void stop() {
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
