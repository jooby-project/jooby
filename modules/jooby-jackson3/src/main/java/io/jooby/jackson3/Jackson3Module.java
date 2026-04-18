/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFilter;
import io.jooby.*;
import io.jooby.internal.jackson3.*;
import io.jooby.json.JsonCodec;
import io.jooby.json.JsonDecoder;
import io.jooby.json.JsonEncoder;
import io.jooby.output.Output;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.*;
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

  private ObjectMapper mapper;
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
  public Jackson3Module(ObjectMapper mapper, MediaType contentType) {
    this.mapper = mapper;
    this.typeFactory = mapper.getTypeFactory();
    this.mediaType = contentType;
  }

  /**
   * Creates a Jackson module.
   *
   * @param mapper Object mapper to use.
   */
  public Jackson3Module(ObjectMapper mapper) {
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
  public void install(Jooby application) {
    application.decoder(mediaType, this);
    application.encoder(mediaType, this);

    var services = application.getServices();
    bindMapper(services, mapper);
    // Json Codec
    var jsonCodec = new JacksonJsonCodec(mapper);
    services.putIfAbsent(JsonCodec.class, jsonCodec);
    services.putIfAbsent(JsonEncoder.class, jsonCodec);
    services.putIfAbsent(JsonDecoder.class, jsonCodec);

    // Parsing exception as 400
    application.errorCode(StreamReadException.class, StatusCode.BAD_REQUEST);
    application.errorCode(DatabindException.class, StatusCode.BAD_REQUEST);

    application.onStarting(() -> onStarting(application, services));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void bindMapper(ServiceRegistry services, ObjectMapper mapper) {
    Class mapperType = mapper.getClass();
    services.put(mapperType, this.mapper);
    services.put(ObjectMapper.class, this.mapper);
  }

  private void onStarting(Jooby application, ServiceRegistry services) {
    var finalMapper = this.mapper;
    var modules = computeModules(application);
    if (!modules.isEmpty()) {
      var builder = mapper.rebuild();
      modules.forEach(builder::addModule);
      // re-bind
      finalMapper = builder.build();
      this.mapper = finalMapper;
      bindMapper(services, finalMapper);
    }
    // Branch off a specialized mapper JUST for Projections.
    projectionMapper = finalMapper.rebuild().addMixIn(Object.class, ProjectionMixIn.class).build();
  }

  private List<JacksonModule> computeModules(Jooby application) {
    List<JacksonModule> result = new ArrayList<>();
    for (Class<? extends JacksonModule> type : modules) {
      var module = application.require(type);
      result.add(module);
    }
    List<JacksonModule> moreModules =
        application.getServices().getOrNull(Reified.list(JacksonModule.class));
    if (moreModules != null) {
      result.addAll(moreModules);
    }
    return result;
  }

  @Override
  public Output encode(Context ctx, Object value) {
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
  public static JsonMapper create(JacksonModule... modules) {
    JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();

    Stream.of(modules).forEach(builder::addModule);

    return builder.build();
  }

  /** Global MixIn to force Jackson to apply our filter to ALL outgoing objects. */
  @JsonFilter(FILTER_ID)
  private interface ProjectionMixIn {}
}
