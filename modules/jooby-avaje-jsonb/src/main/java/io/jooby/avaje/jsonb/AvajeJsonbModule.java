/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.io.InputStream;
import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.avaje.jsonb.Jsonb;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;
import io.jooby.internal.avaje.jsonb.BufferedJsonOutput;
import io.jooby.output.BufferedOutput;

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

  @NonNull @Override
  public BufferedOutput encode(@NonNull Context ctx, @NonNull Object value) {
    ctx.setDefaultResponseType(MediaType.json);
    var factory = ctx.getOutputFactory();
    var buffer = factory.newBufferedOutput();
    try (var writer = jsonb.writer(new BufferedJsonOutput(buffer))) {
      jsonb.toJson(value, writer);
      return buffer;
    }
  }
}
