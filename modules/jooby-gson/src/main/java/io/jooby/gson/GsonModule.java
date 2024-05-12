/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gson;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;
import io.jooby.buffer.DataBuffer;

/**
 * JSON module using Gson: https://github.com/google/gson.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new GsonModule());
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
 * <p>You can retrieve the {@link Gson} object via require call:
 *
 * <pre>{@code
 * {
 *
 *   Gson gson = require(Gson.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/gson.
 *
 * @author edgar
 * @since 2.7.2
 */
public class GsonModule implements Extension, MessageDecoder, MessageEncoder {

  private final Gson gson;

  /**
   * Creates a new module and use a Gson instance.
   *
   * @param gson Gson to use.
   */
  public GsonModule(@NonNull Gson gson) {
    this.gson = gson;
  }

  /** Creates a new Gson module. */
  public GsonModule() {
    this(new GsonBuilder().create());
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    application.decoder(MediaType.json, this);
    application.encoder(MediaType.json, this);

    ServiceRegistry services = application.getServices();
    services.put(Gson.class, gson);
  }

  @NonNull @Override
  public Object decode(@NonNull Context ctx, @NonNull Type type) throws Exception {
    Body body = ctx.body();
    if (body.isInMemory()) {
      return gson.fromJson(
          new InputStreamReader(new ByteArrayInputStream(body.bytes()), UTF_8), type);
    } else {
      try (InputStream stream = body.stream()) {
        return gson.fromJson(new InputStreamReader(stream, UTF_8), type);
      }
    }
  }

  @NonNull @Override
  public DataBuffer encode(@NonNull Context ctx, @NonNull Object value) {
    var buffer = ctx.getBufferFactory().allocateBuffer();
    ctx.setDefaultResponseType(MediaType.json);
    gson.toJson(value, buffer.asWriter());
    return buffer;
  }
}
