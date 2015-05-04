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
package org.jooby.internal.elasticsearch;

import static java.util.Objects.requireNonNull;

import javax.inject.Provider;

import org.elasticsearch.client.Client;
import org.jooby.Managed;

public class ManagedClient implements Managed, Provider<Client> {

  private ManagedNode node;

  private Client client;

  public ManagedClient(final ManagedNode node) {
    this.node = requireNonNull(node, "A node is required.");
  }

  @Override
  public Client get() {
    return client;
  }

  @Override
  public void start() throws Exception {
    this.client = node.get().client();
  }

  @Override
  public void stop() throws Exception {
    if (this.client != null) {
      this.client.close();
      this.client = null;
    }
  }

}
