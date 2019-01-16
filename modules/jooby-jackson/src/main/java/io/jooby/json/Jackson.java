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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Parser;
import io.jooby.Renderer;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.reflect.Type;

public class Jackson implements Extension, Parser, Renderer {

  private final ObjectMapper mapper;

  public Jackson(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Jackson() {
    this(defaultObjectMapper());
  }

  public static final ObjectMapper defaultObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new AfterburnerModule());

    return objectMapper;
  }

  @Override public void install(Jooby application) {
    application.parser(MediaType.JSON, this);
    application.renderer(MediaType.JSON, this);
  }

  @Override public boolean render(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof CharSequence) {
      // Ignore string/charsequence responses, those are going to be processed by the default
      // renderer and let route to return raw JSON
      ctx.type(MediaType.json)
          .sendText(value.toString());
    } else {
      ctx.type(MediaType.json)
          .sendBytes(mapper.writeValueAsBytes(value));
    }
    return true;
  }

  @Override public <T> T parse(Context ctx, Type type) throws Exception {
    JavaType javaType = mapper.getTypeFactory().constructType(type);
    Body body = ctx.body();
    if (body.isInMemory()) {
      return mapper.readValue(body.bytes(), javaType);
    } else {
      try (InputStream stream = body.stream()) {
        return mapper.readValue(stream, javaType);
      }
    }
  }
}
