/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Converter;
import io.jooby.MediaType;
import io.jooby.Reified;

import javax.annotation.Nonnull;

public class Jackson extends Converter {

  private final ObjectMapper mapper;

  public Jackson(ObjectMapper mapper) {
    super(MediaType.JSON);
    this.mapper = mapper;
  }

  public Jackson() {
    this(new ObjectMapper());
  }

  @Override public void render(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof CharSequence) {
      // Ignore string/charsequence responses, those are going to be processed by the default renderer and let route to return raw JSON
      return;
    }
    ctx.type(MediaType.JSON).sendBytes(mapper.writeValueAsBytes(value));
  }

  @Override public <T> T parse(Context ctx, Reified<T> type) throws Exception {
    JavaType javaType = mapper.getTypeFactory().constructType(type.getType());
    return mapper.readValue(ctx.body().stream(), javaType);
  }
}
