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
package org.jooby.internal.hbs;

import static java.util.Objects.requireNonNull;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;

public class HbsEngine implements View.Engine {

  private Handlebars handlebars;

  private ValueResolver[] resolvers;

  public HbsEngine(final Handlebars handlebars, final ValueResolver[] resolvers) {
    this.handlebars = requireNonNull(handlebars, "Handlebars is required.");
    this.resolvers = requireNonNull(resolvers, "Resolvers are required.");
  }

  @Override
  public String name() {
    return "hbs";
  }

  @Override
  public void render(final View view, final Renderer.Context ctx) throws Exception {
    Template template = handlebars.compile(view.name());

    com.github.jknack.handlebars.Context context = com.github.jknack.handlebars.Context
        .newBuilder(view.model())
        // merge request locals (req+sessions locals)
        .combine(ctx.locals())
        .resolver(resolvers)
        .build();

    // rendering it
    ctx.type(MediaType.html)
        .send(template.apply(context));
  }

  @Override
  public String toString() {
    return name();
  }

}
