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
package com.impossibl.postgres.jdbc;

import java.net.InetSocketAddress;

import com.impossibl.postgres.jdbc.ConnectionUtil.ConnectionSpecifier;

// Hack documented here: https://github.com/impossibl/pgjdbc-ng/issues/323
public class PGDataSourceWithUrl extends PGDataSource {

  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
    ConnectionSpecifier specifier = ConnectionUtil.parseURL(url);
    setDatabase(specifier.getDatabase());
    InetSocketAddress address = specifier.getAddresses().get(0);
    setHost(address.getHostString());
    setPort(address.getPort());
  }

}
