/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gson;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import io.jooby.json.JsonCodec;

public class GsonJsonCodec implements JsonCodec {
  private final Gson gson;

  public GsonJsonCodec(Gson gson) {
    this.gson = gson;
  }

  @Override
  public <T> T decode(String json, Type type) {
    return gson.fromJson(json, type);
  }

  @Override
  public String encode(Object value) {
    return gson.toJson(value);
  }
}
