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
package org.jooby.thymeleaf;

import java.io.FileNotFoundException;
import java.util.Map;

import org.jooby.Env;
import org.jooby.MediaType;
import org.jooby.View;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

class ThlEngine implements View.Engine {

  private final Env env;

  private final TemplateEngine engine;

  public ThlEngine(final TemplateEngine engine, final Env env) {
    this.engine = engine;
    this.env = env;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void render(final View view, final Context ctx) throws FileNotFoundException, Exception {
    String vname = view.name();

    Map<String, Object> vars = ctx.locals();
    vars.putIfAbsent("_vname", vname);

    Map model = view.model();
    vars.forEach(model::putIfAbsent);
    model.putIfAbsent("xss", new Thlxss(env));

    IContext thlctx = new org.thymeleaf.context.Context(ctx.locale(), model);
    String output = this.engine.process(vname, thlctx);

    ctx.type(MediaType.html)
        .send(output);
  }

  @Override
  public String name() {
    return "thymeleaf";
  }
}
