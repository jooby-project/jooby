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
package org.jooby.pebble;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.LoaderException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;;

class PebbleRenderer implements View.Engine {

  private PebbleEngine pebble;

  public PebbleRenderer(final PebbleEngine pebble) {
    this.pebble = pebble;
  }

  @Override
  public void render(final View view, final Renderer.Context ctx) throws Exception {
    String vname = view.name();
    try {
      PebbleTemplate template = pebble.getTemplate(vname);
      Writer writer = new StringWriter();
      Map<String, Object> model = new HashMap<>();
      // push locals
      model.putAll(ctx.locals());
      model.putIfAbsent("_vname", vname);

      // put model
      model.putAll(view.model());

      // render and send
      template.evaluate(writer, model);
      ctx.type(MediaType.html)
          .send(writer.toString());
    } catch (LoaderException x) {
      throw new FileNotFoundException(vname);
    }
  }

  @Override
  public String name() {
    return "pebble";
  }

  @Override
  public String toString() {
    return name();
  }

}
