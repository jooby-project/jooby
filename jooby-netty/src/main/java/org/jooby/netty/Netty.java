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
package org.jooby.netty;

import javax.inject.Singleton;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.netty.NettyServer;
import org.jooby.spi.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;

public class Netty implements Jooby.Module {

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    System.setProperty("io.netty.tmpdir", config.getString("application.tmpdir"));
    binder.bind(Server.class).to(NettyServer.class).in(Singleton.class);
  }

}
