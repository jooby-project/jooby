/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.MessageDecoder;
import io.jooby.MessageEncoder;
import io.jooby.ServiceRegistry;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JSON module using Gson: https://github.com/google/gson.
 *
 * Usage:
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
 * For body decoding the client must specify the <code>Content-Type</code> header set to
 * <code>application/json</code>.
 *
 * You can retrieve the {@link Gson} object via require call:
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

  private Gson gson;

  /**
   * Creates a new module and use a Gson instance.
   *
   * @param gson Gson to use.
   */
  public GsonModule(@Nonnull Gson gson) {
    this.gson = gson;
  }

  /**
   * Creates a new Gson module.
   */
  public GsonModule() {
    this(new GsonBuilder().create());
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    application.decoder(MediaType.json, this);
    application.encoder(MediaType.json, this);

    ServiceRegistry services = application.getServices();
    services.put(Gson.class, gson);
  }

  @Nonnull @Override public Object decode(@Nonnull Context ctx, @Nonnull Type type)
      throws Exception {
    Body body = ctx.body();
    if (body.isInMemory()) {
      return gson.fromJson(new InputStreamReader(new ByteArrayInputStream(body.bytes())), type);
    } else {
      try (InputStream stream = body.stream()) {
        return gson.fromJson(new InputStreamReader(stream), type);
      }
    }
  }

  @Nonnull @Override public byte[] encode(@Nonnull Context ctx, @Nonnull Object value) {
    ctx.setDefaultResponseType(MediaType.json);
    return gson.toJson(value).getBytes(UTF_8);
  }
}
