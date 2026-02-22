/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFilter;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.output.Output;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.type.TypeFactory;

/**
 * JSON module using Jackson3: https://jooby.io/modules/jackson3.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new Jackson3Module());
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
 * <p>For body decoding the client must specify the <code>Content-Type</code> header set to <code>
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
 * @author edgar, kliushnichenko
 * @since 4.1.0
 */
public class Jackson3Module implements Extension, MessageDecoder, MessageEncoder {
  // A hardcoded ID for our filter
  public static final String FILTER_ID = "jooby.projection";

  // Cache for ObjectWriters tied to specific projection strings
  private final Map<String, ObjectWriter> writerCache = new ConcurrentHashMap<>();

  private final MediaType mediaType;

  private final ObjectMapper mapper;
  private ObjectMapper projectionMapper;

  private final TypeFactory typeFactory;

  private final Set<Class<? extends JacksonModule>> modules = new HashSet<>();

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
  public Jackson3Module(@NonNull ObjectMapper mapper, @NonNull MediaType contentType) {
    this.mapper = mapper;
    this.typeFactory = mapper.getTypeFactory();
    this.mediaType = contentType;
  }

  /**
   * Creates a Jackson module.
   *
   * @param mapper Object mapper to use.
   */
  public Jackson3Module(@NonNull ObjectMapper mapper) {
    this(mapper, defaultTypes.getOrDefault(mapper.getClass().getSimpleName(), MediaType.json));
  }

  /**
   * Creates a Jackson module using the default object mapper from {@link
   * #create(JacksonModule...)}.
   */
  public Jackson3Module() {
    this(create());
  }

  /**
   * Add a Jackson module to the object mapper. This method require a dependency injection framework
   * which is responsible for provisioning a module instance.
   *
   * @param module Module type.
   * @return This module.
   */
  public Jackson3Module module(Class<? extends JacksonModule> module) {
    modules.add(module);
    return this;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void install(@NonNull Jooby application) {
    application.decoder(mediaType, this);
    application.encoder(mediaType, this);

    ServiceRegistry services = application.getServices();
    Class mapperType = mapper.getClass();
    services.put(mapperType, mapper);
    services.put(ObjectMapper.class, mapper);

    // Parsing exception as 400
    application.errorCode(StreamReadException.class, StatusCode.BAD_REQUEST);

    application.onStarting(() -> onStarting(application, services, mapperType));

    // 2. Branch off a specialized mapper JUST for Projections.
    // .rebuild() copies the user's configuration, and we add our global MixIn
    // strictly to this specialized instance.
    projectionMapper = mapper.rebuild().addMixIn(Object.class, ProjectionMixIn.class).build();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void onStarting(Jooby application, ServiceRegistry services, Class mapperType) {
    if (!modules.isEmpty()) {
      var builder = mapper.rebuild();
      for (Class<? extends JacksonModule> type : modules) {
        JacksonModule module = application.require(type);
        builder.addModule(module);
      }
      var newMapper = builder.build();
      services.put(mapperType, newMapper);
      services.put(ObjectMapper.class, newMapper);
    }
  }

  @Override
  public Output encode(@NonNull Context ctx, @NonNull Object value) {
    var factory = ctx.getOutputFactory();
    ctx.setDefaultResponseType(mediaType);
    if (value instanceof Projected<?> projected) {
      var p = projected.getProjection();

      var writer =
          writerCache.computeIfAbsent(
              p.getType().getName() + p.toView(),
              k -> {
                // Build the filter and writer only once per unique projection string
                var filters =
                    new SimpleFilterProvider().addFilter(FILTER_ID, new JacksonProjectionFilter(p));
                return projectionMapper.writer(filters);
              });
      return factory.wrap(writer.writeValueAsBytes(projected.getValue()));
    }
    // let jackson uses his own cache, so wrap the bytes
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
   * Default object mapper.
   *
   * @param modules Extra/additional modules to install.
   * @return Object mapper instance.
   */
  public static ObjectMapper create(JacksonModule... modules) {
    JsonMapper.Builder builder = JsonMapper.builder();

    Stream.of(modules).forEach(builder::addModule);

    return builder.build();
  }

  /** Global MixIn to force Jackson to apply our filter to ALL outgoing objects. */
  @JsonFilter(FILTER_ID)
  private interface ProjectionMixIn {}
}
