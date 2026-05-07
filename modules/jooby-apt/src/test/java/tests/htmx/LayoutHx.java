/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import java.util.Map;

import org.jspecify.annotations.NonNull;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.annotation.htmx.*;

@Path("/users")
public class LayoutHx {

  @GET
  @HxView(value = "users/profile.hbs", layout = "layout.hbs")
  @HxTrigger("pageLoaded")
  public Map<String, Object> layout() {
    return Map.of();
  }

  @GET("/{id}")
  @HxView(value = "users/profile.hbs")
  @HxTrigger("userRead")
  public User3936 nolayout(@PathParam @NonNull String id) {
    return new User3936(id, "Edgar", "edgar@example.com");
  }
}
