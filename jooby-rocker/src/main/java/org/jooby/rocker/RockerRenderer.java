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
package org.jooby.rocker;

import java.nio.channels.Channels;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Route;
import org.jooby.View;

import com.fizzed.rocker.Rocker;
import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;

class RockerRenderer implements Renderer {

  private String prefix;

  private String suffix;

  public RockerRenderer(final String prefix, final String suffix) {
    this.prefix = prefix;
    this.suffix = suffix;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void render(final Object value, final Context ctx) throws Exception {
    Object model = value;
    /** View? */
    if (value instanceof View) {
      View view = (View) value;
      String path = path(Route.normalize(prefix + "/" + view.name() + suffix));

      Map data = view.model();
      model = Rocker.template(path).bind(data);
    }
    /** RockerModel: */
    if (model instanceof RockerModel) {
      ArrayOfByteArraysOutput output = ((RockerModel) model).render(ArrayOfByteArraysOutput.FACTORY,
          template -> {
            if (template instanceof RequestRockerTemplate) {
              RequestRockerTemplate rrt = (RequestRockerTemplate) template;
              rrt.locals = ctx.locals();
            }
          });
      ctx.type(MediaType.html)
          .length(output.getByteLength())
          // FIXME: make more efficient. Context should provide a way to send partial results
          .send(Channels.newInputStream(output.asReadableByteChannel()));
    }
  }

  static String path(final String path) {
    if (path.startsWith("/")) {
      return path.substring(1);
    }
    return path;
  }

}
