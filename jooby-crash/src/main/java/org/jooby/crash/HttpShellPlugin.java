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
package org.jooby.crash;

import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.shell.ShellProcess;
import org.crsh.shell.ShellProcessContext;
import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.MediaType;

import com.typesafe.config.Config;

public class HttpShellPlugin extends CRaSHPlugin<HttpShellPlugin> {

  @Override
  public HttpShellPlugin getImplementation() {
    return this;
  }

  static void install(final Env env, final Config conf) {
    String path = conf.getString("crash.httpshell.path");
    env.router().get(path + "/{cmd:.*}", req -> {
      MediaType type = req.accepts(MediaType.json).map(it -> MediaType.json)
          .orElse(MediaType.plain);

      return new Deferred(deferred -> {
        PluginContext ctx = req.require(PluginContext.class);
        ShellFactory shellFactory = ctx.getPlugin(ShellFactory.class);
        Shell shell = shellFactory.create(null);
        String cmd = req.param("cmd").value().replaceAll("/", " ");
        ShellProcess process = shell.createProcess(cmd);
        ShellProcessContext spc = new SimpleProcessContext(
            result -> deferred.resolve(result.type(type)));
        process.execute(spc);
      });
    });
  }

}
