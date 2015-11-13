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
package org.jooby.internal.pac4j;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("rawtypes")
public class ClientsProvider implements Provider<Clients> {

  private Clients clients;

  @Inject
  public ClientsProvider(@Named("auth.callback") final String callback,
      final Set<Client> clients) {
    this.clients = new Clients(callback, ImmutableList.copyOf(clients));
  }

  @Override
  public Clients get() {
    return clients;
  }

}
