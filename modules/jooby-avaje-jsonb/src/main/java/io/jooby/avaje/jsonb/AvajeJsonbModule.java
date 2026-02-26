/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.JsonView;
import io.avaje.jsonb.Jsonb;
import io.jooby.*;
import io.jooby.internal.avaje.jsonb.BufferedJsonOutput;
import io.jooby.output.Output;

/**
 * JSON module using Avaje-JsonB: <a
 * href="https://github.com/avaje/avaje-jsonb">https://github.com/avaje/avaje-jsonb</a>.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new AvajeJsonbModule());
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
 * <p>You can retrieve the {@link Jsonb} object via require call:
 *
 * <pre>{@code
 * {
 *
 *   Jsonb jsonb = require(Jsonb.class);
 *
 * }
 * }</pre>
 *
 * <p>
 *
 * @author ZY (kzou227@qq.com)
 * @since 3.0.7
 */
public class AvajeJsonbModule implements Extension, MessageDecoder, MessageEncoder {

  private final ConcurrentMap<String, JsonView<?>> viewCache = new ConcurrentHashMap<>();

  private final Jsonb jsonb;

  /**
   * Creates a new module and use an Avaje-JsonB instance.
   *
   * @param jsonb Gson to use.
   */
  public AvajeJsonbModule(@NonNull Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  /** Creates a new Avaje-JsonB module. */
  public AvajeJsonbModule() {
    this(Jsonb.builder().build());
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    application.decoder(MediaType.json, this);
    application.encoder(MediaType.json, this);

    ServiceRegistry services = application.getServices();
    services.put(Jsonb.class, jsonb);
  }

  @NonNull @Override
  public Object decode(@NonNull Context ctx, @NonNull Type type) throws Exception {
    Body body = ctx.body();
    if (body.isInMemory()) {
      return jsonb.type(type).fromJson(body.bytes());
    } else {
      try (InputStream stream = body.stream()) {
        return jsonb.type(type).fromJson(stream);
      }
    }
  }

  @Override
  public Output encode(@NonNull Context ctx, @NonNull Object value) {
    ctx.setDefaultResponseType(MediaType.json);
    var factory = ctx.getOutputFactory();
    var buffer = factory.allocate();
    try (var writer = jsonb.writer(new BufferedJsonOutput(buffer))) {
      if (value instanceof Projected<?> projected) {
        encodeProjection(writer, projected);
      } else {
        jsonb.toJson(value, writer);
      }
      return buffer;
    }
  }

  @SuppressWarnings("unchecked")
  private void encodeProjection(JsonWriter writer, Projected<?> projected) {
    // Generate the Avaje-compatible view string (e.g., "(id,name,address(city))")
    var value = projected.getValue();
    var projection = projected.getProjection();
    var viewString = projection.toView();
    var type = projection.getType();
    var view =
        (JsonView<Object>)
            viewCache.computeIfAbsent(
                value.getClass().getName() + viewString,
                k -> {
                  var jsonbType = jsonb.type(type);
                  jsonbType =
                      switch (value) {
                        case Set<?> ignored -> jsonbType.set();
                        case Collection<?> ignored -> jsonbType.list();
                        default -> jsonbType;
                      };
                  return jsonbType.view(viewString);
                });
    if (value instanceof Optional<?> optional) {
      if (optional.isEmpty()) {
        writer.serializeNulls(true);
        writer.nullValue();
      } else {
        view.toJson(optional.get(), writer);
      }
    } else {
      view.toJson(value, writer);
    }
  }
}
