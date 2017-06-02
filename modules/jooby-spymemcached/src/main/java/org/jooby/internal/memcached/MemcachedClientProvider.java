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
package org.jooby.internal.memcached;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import javaslang.control.Try;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

public class MemcachedClientProvider implements Provider<MemcachedClient> {

  private ConnectionFactoryBuilder builder;

  private List<InetSocketAddress> servers;

  private MemcachedClient client;

  private long shutdownTimeout;

  public MemcachedClientProvider(final ConnectionFactoryBuilder builder,
      final List<InetSocketAddress> servers, final long shutdownTimeout) {
    this.builder = builder;
    this.servers = servers;
    this.shutdownTimeout = shutdownTimeout;
  }

  public void destroy() {
    if (client != null) {
      client.shutdown(shutdownTimeout, TimeUnit.MILLISECONDS);
      client = null;
    }
  }

  @Override
  public MemcachedClient get() {
    client = Try.of(() -> {
      ConnectionFactory connectionFactory = builder.build();
      this.builder = null;
      return new MemcachedClient(connectionFactory, servers);
    }).get();
    return client;
  }

}
