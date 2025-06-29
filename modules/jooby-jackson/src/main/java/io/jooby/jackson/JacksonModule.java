/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;
import io.jooby.StatusCode;
import io.jooby.output.BufferedOutput;

/**
 * JSON module using Jackson: https://jooby.io/modules/jackson.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new JacksonModule());
 *
 *   get("/", ctx -> {
 *     MyObject myObject = ...;
 *     // send json
 *     return myObject;
 *   });
 *
 *   post("/", ctx -> {
 *     // read json
 *     MyObject myObject = ctx.body(MyObject.class);
 *     // send json
 *     return myObject;
 *   });
 * }
 * }</pre>
 *
 * For body decoding the client must specify the <code>Content-Type</code> header set to <code>
 * application/json</code>.
 *
 * <p>You can retrieve the {@link ObjectMapper} via require call:
 *
 * <pre>{@code
 * {
 *
 *   ObjectMapper mapper = require(ObjectMapper.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/jackson.
 *
 * @author edgar
 * @since 2.0.0
 */
public class JacksonModule implements Extension, MessageDecoder, MessageEncoder {
  private final MediaType mediaType;

  private final ObjectMapper mapper;

  private final TypeFactory typeFactory;

  private final Set<Class<? extends Module>> modules = new HashSet<>();

  private static final Map<String, MediaType> defaultTypes = new HashMap<>();

  static {
    defaultTypes.put("XmlMapper", MediaType.xml);
  }

  /**
   * Creates a Jackson module.
   *
   * @param mapper Object mapper to use.
   * @param contentType Content type.
   */
  public JacksonModule(@NonNull ObjectMapper mapper, @NonNull MediaType contentType) {
    this.mapper = mapper;
    this.typeFactory = mapper.getTypeFactory();
    this.mediaType = contentType;
  }

  /**
   * Creates a Jackson module.
   *
   * @param mapper Object mapper to use.
   */
  public JacksonModule(@NonNull ObjectMapper mapper) {
    this(mapper, defaultTypes.getOrDefault(mapper.getClass().getSimpleName(), MediaType.json));
  }

  /** Creates a Jackson module using the default object mapper from {@link #create(Module...)}. */
  public JacksonModule() {
    this(create());
  }

  /**
   * Add a Jackson module to the object mapper. This method require a dependency injection framework
   * which is responsible for provisioning a module instance.
   *
   * @param module Module type.
   * @return This module.
   */
  public JacksonModule module(Class<? extends Module> module) {
    modules.add(module);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) {
    application.decoder(mediaType, this);
    application.encoder(mediaType, this);

    ServiceRegistry services = application.getServices();
    Class mapperType = mapper.getClass();
    services.put(mapperType, mapper);
    services.put(ObjectMapper.class, mapper);

    // Parsing exception as 400
    application.errorCode(JsonParseException.class, StatusCode.BAD_REQUEST);

    application.onStarting(
        () -> {
          for (Class<? extends Module> type : modules) {
            Module module = application.require(type);
            mapper.registerModule(module);
          }
        });
  }

  @Override
  public BufferedOutput encode(@NonNull Context ctx, @NonNull Object value) throws Exception {
    var factory = ctx.getOutputFactory();
    ctx.setDefaultResponseType(mediaType);
    // let jackson uses his own cache, so just wrap the bytes
    return factory.wrap(mapper.writeValueAsBytes(value));
  }

  @Override
  public Object decode(Context ctx, Type type) throws Exception {
    Body body = ctx.body();
    if (body.isInMemory()) {
      if (type == JsonNode.class) {
        return mapper.readTree(body.bytes());
      }
      return mapper.readValue(body.bytes(), typeFactory.constructType(type));
    } else {
      try (InputStream stream = body.stream()) {
        if (type == JsonNode.class) {
          return mapper.readTree(stream);
        }
        return mapper.readValue(stream, typeFactory.constructType(type));
      }
    }
  }

  /**
   * Default object mapper. Install {@link Jdk8Module}, {@link JavaTimeModule}, {@link
   * ParameterNamesModule}.
   *
   * @param modules Extra/additional modules to install.
   * @return Object mapper instance.
   */
  public static @NonNull ObjectMapper create(Module... modules) {
    JsonMapper.Builder builder =
        JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule());

    Stream.of(modules).forEach(builder::addModule);

    return builder.build();
  }
}
