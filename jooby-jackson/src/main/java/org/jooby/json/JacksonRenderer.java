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
package org.jooby.json;

import org.jooby.MediaType;
import org.jooby.Renderer;

import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonRenderer implements Renderer {

  private ObjectMapper mapper;

  private MediaType type;

  public JacksonRenderer(final ObjectMapper mapper, final MediaType type) {
    this.mapper = mapper;
    this.type = type;
  }

  @Override
  public void render(final Object value, final Context ctx) throws Exception {
    if (ctx.accepts(type) && mapper.canSerialize(value.getClass())) {
      ctx.type(type);
      // use UTF-8 and get byte version
      byte[] bytes = mapper.writer().writeValueAsBytes(value);
      ctx.length(bytes.length)
          .send(bytes);
    }
  }

  @Override
  public String toString() {
    return "json";
  }

}
