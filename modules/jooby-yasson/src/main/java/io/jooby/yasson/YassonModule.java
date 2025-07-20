/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.yasson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;
import io.jooby.output.Output;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * JSON module using JSON-B: https://github.com/eclipse-ee4j/jsonb-api.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new YassonModule());
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
 * Complete documentation is available at: https://jooby.io/modules/jsonb.
 */
public class YassonModule implements Extension, MessageDecoder, MessageEncoder {

  private final Jsonb jsonb;

  /** Creates a new Jsonb module. */
  public YassonModule() {
    jsonb = JsonbBuilder.create();
  }

  /**
   * Creates a new module and use a Jsonb instance.
   *
   * @param jsonb Jsonb to use.
   */
  public YassonModule(@NonNull final Jsonb jsonb) {
    this.jsonb = jsonb;
  }

  @Override
  public void install(@NonNull final Jooby application) throws Exception {
    application.decoder(MediaType.json, this);
    application.encoder(MediaType.json, this);

    ServiceRegistry services = application.getServices();
    services.put(Jsonb.class, jsonb);
  }

  @NonNull @Override
  public Object decode(@NonNull final Context ctx, @NonNull final Type type) throws IOException {

    Body body = ctx.body();
    try (InputStream stream = body.stream()) {
      return jsonb.fromJson(new InputStreamReader(stream), type);
    }
  }

  @Nullable @Override
  public Output encode(@NonNull final Context ctx, @NonNull final Object value) {
    ctx.setDefaultResponseType(MediaType.json);
    var factory = ctx.getOutputFactory();
    var output = factory.newOutput();
    jsonb.toJson(value, output.asOutputStream());
    return output;
  }
}
