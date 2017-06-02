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
package org.jooby.internal.ebean;

import java.io.File;

import javax.inject.Provider;

import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.typesafe.config.Config;

public class EbeanManaged implements Provider<EbeanServer> {

  private Supplier<EbeanServer> ebean;

  public EbeanManaged(final Config config, final ServerConfig conf) {
    ebean = Suppliers.memoize(() -> {
      EbeanServer ebean = EbeanServerFactory.create(conf);
      // move .sql file to tmp directory... didn't find any other way of dealing with this
      if (conf.isDdlGenerate()) {
        String tmpdir = config.getString("application.tmpdir");
        move(conf.getName() + "-drop-all.sql", tmpdir);
        move(conf.getName() + "-create-all.sql", tmpdir);
      }
      return ebean;
    });
  }

  private void move(final String fname, final String tmpdir) {
    new File(fname).renameTo(new File(tmpdir, fname));
  }

  public void start() throws Exception {
    ebean.get();
  }

  public void stop() throws Exception {
    if (ebean != null) {
      ebean.get().shutdown(false, false);
      ebean = null;
    }
  }

  @Override
  public EbeanServer get() {
    return ebean.get();
  }

}
