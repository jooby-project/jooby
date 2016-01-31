/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.jade;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.JadeTemplate;
import org.jooby.MediaType;
import org.jooby.View;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class Engine implements View.Engine {

  private final JadeConfiguration jadeConfiguration;
  private final String suffix;

  public Engine(final JadeConfiguration jadeConfiguration, final String suffix) {
    this.jadeConfiguration = requireNonNull(jadeConfiguration, "jade config is required.");
    this.suffix = suffix;
  }

  @Override
  public void render(View view, Context ctx) throws FileNotFoundException, Exception {
    String name = view.name() + suffix;

    JadeTemplate template = jadeConfiguration.getTemplate(name);

    Map<String, Object> hash = new HashMap<>();

    hash.put("_vname", view.name());
    hash.put("_vpath", name);

    // locals & model
    hash.putAll(ctx.locals());
    hash.putAll(view.model());

    String output = jadeConfiguration.renderTemplate(template, hash);
    ctx.type(MediaType.html).send(output);
  }

  @Override
  public String toString() {
    return "jade";
  }
}
