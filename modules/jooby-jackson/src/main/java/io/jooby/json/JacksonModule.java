/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
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
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class JacksonModule implements Extension, MessageDecoder, MessageEncoder {
  private final ObjectMapper mapper;

  private final Set<Class<?extends Module>> modules = new HashSet<>();

  public JacksonModule(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public JacksonModule() {
    this(create());
  }

  public @Nonnull Set<Class<? extends Module>> getModules() {
    return modules;
  }

  @Override public void install(@Nonnull Jooby application) {
    application.parser(MediaType.json, this);
    application.renderer(MediaType.json, this);

    ServiceRegistry services = application.getServices();
    services.put(ObjectMapper.class, mapper);

    application.onStarted(()-> {
      for (Class<? extends Module> type : modules) {
        Module module = application.require(type);
        mapper.registerModule(module);
      }
    });
  }

  @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    ctx.setDefaultResponseType(MediaType.json);
    return mapper.writeValueAsBytes(value);
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

  public static final @Nonnull ObjectMapper create() {
    ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new AfterburnerModule());

    return objectMapper;
  }
}
