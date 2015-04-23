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
package org.skife.jdbi.v2;

import java.util.ArrayList;
import java.util.List;

import org.skife.jdbi.v2.tweak.ArgumentFactory;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

public class DBI2 extends DBI {

  public static final String ARG_FACTORIES = "__argumentFactories_";

  private List<ArgumentFactory<?>> factories = new ArrayList<ArgumentFactory<?>>();

  public DBI2(final ConnectionFactory connectionFactory) {
    super(connectionFactory);
    define(ARG_FACTORIES, factories);
  }

  @Override
  public void registerArgumentFactory(final ArgumentFactory<?> argumentFactory) {
    factories.add(argumentFactory);
    super.registerArgumentFactory(argumentFactory);
  }

}
