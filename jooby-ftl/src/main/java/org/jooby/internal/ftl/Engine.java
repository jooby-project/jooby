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
package org.jooby.internal.ftl;

import static java.util.Objects.requireNonNull;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNotFoundException;

public class Engine implements View.Engine {

  private Configuration freemarker;

  private String suffix;

  public Engine(final Configuration freemarker, final String suffix) {
    this.freemarker = requireNonNull(freemarker, "Freemarker config is required.");
    this.suffix = suffix;
  }

  @Override
  public void render(final View view, final Renderer.Context ctx) throws Exception {
    String name = view.name() + suffix;

    Template template = template(name, ctx.charset());

    Map<String, Object> hash = new HashMap<>();

    hash.put("_vname", view.name());
    hash.put("_vpath", template.getName());

    // locals
    hash.putAll(ctx.locals());

    // model
    hash.putAll(view.model());
    TemplateModel model = new SimpleHash(hash, new FtlWrapper(freemarker.getObjectWrapper()));

    // TODO: remove string writer
    StringWriter writer = new StringWriter();

    // output
    template.process(model, writer);
    ctx.type(MediaType.html)
        .send(writer.toString());

  }

  private Template template(final String name, final Charset charset) throws Exception {
    try {
      return freemarker.getTemplate(name, charset.name());
    } catch (TemplateNotFoundException ex) {
      throw new FileNotFoundException(ex.getTemplateName());
    }
  }

  @Override
  public String toString() {
    return "freemarker";
  }

}
