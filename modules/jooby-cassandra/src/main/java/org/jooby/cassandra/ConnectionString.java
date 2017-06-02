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
package org.jooby.cassandra;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.datastax.driver.core.ProtocolOptions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import javaslang.Tuple;
import javaslang.Tuple2;

class ConnectionString {

  public static final String SCHEMA = "cassandra://";
  private static final String SAMPLE = SCHEMA + "host[, host]*[:port]/keyspace";

  private String[] address;

  private int port;

  private String keyspace;

  private ConnectionString(final String[] address, final int port, final String keyspace) {
    this.address = address;
    this.port = port;
    this.keyspace = keyspace;
  }

  public String[] contactPoints() {
    return address;
  }

  public int port() {
    return port;
  }

  public String keyspace() {
    return keyspace;
  }

  public static ConnectionString parse(final String connectionString) {
    if (!connectionString.startsWith(SCHEMA)) {
      throw new IllegalArgumentException(
          "Unknown schema " + connectionString + ", expected " + SAMPLE);
    }
    List<String> segments = Splitter.on('/')
        .trimResults()
        .omitEmptyStrings()
        .splitToList(connectionString.replace(SCHEMA, ""));
    if (segments.size() != 2) {
      throw new IllegalArgumentException("Invalid " + connectionString + ", expected " + SAMPLE);
    }
    // host[, host]*
    Set<Tuple2<String, Integer>> values = Splitter.on(',')
        .trimResults()
        .omitEmptyStrings()
        .splitToList(segments.get(0))
        .stream()
        .map(v -> {
          String host = v;
          int idx = v.indexOf(':');
          int port = ProtocolOptions.DEFAULT_PORT;
          if (idx > 0) {
            port = Integer.parseInt(v.substring(idx + 1));
            host = v.substring(0, idx);
          }
          return Tuple.of(host, port);
        })
        .collect(Collectors.toSet());

    List<String> hosts = values.stream()
        .map(Tuple2::_1)
        .collect(Collectors.toList());

    Set<Integer> port = values.stream()
        .map(Tuple2::_2)
        .collect(Collectors.toSet());
    if (port.size() > 1) {
      throw new IllegalArgumentException(
          "Found multiple ports: " + port + ", only one port must be present. See " + SAMPLE);
    }
    String keyspace = segments.get(1);
    return new ConnectionString(hosts.toArray(new String[hosts.size()]), port.iterator().next(),
        keyspace);
  }

  @Override
  public String toString() {
    return SCHEMA + Joiner.on(',').join(address) + ':' + port + '/' + keyspace;
  }
}
