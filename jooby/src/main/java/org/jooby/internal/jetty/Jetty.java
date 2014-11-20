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
package org.jooby.internal.jetty;

import javax.inject.Singleton;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.Server;

import com.google.inject.Binder;
import com.google.inject.multibindings.OptionalBinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Jetty implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    OptionalBinder.newOptionalBinder(binder, Server.class).setDefault()
        .to(JettyServer.class)
        .in(Singleton.class);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "jetty.conf");
  }
}
