/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import java.util.Map;

import org.jspecify.annotations.NonNull;

import io.jooby.ModelAndView;
import io.jooby.annotation.*;
import io.jooby.annotation.htmx.*;

@Path("/users")
public class BasicUserHx {

  /**
   * TEST 1: The Basics (View Rendering) Verifies: @HxView wraps the return object into
   * ModelAndView.
   */
  @GET("/{id}")
  @HxView("users/profile.hbs")
  public User3936 getUser(@PathParam @NonNull String id) {
    return new User3936(id, "Edgar", "edgar@example.com");
  }

  /**
   * TEST 2: The Basics (View Rendering) Verifies: @HxView wraps the return object into
   * MapModelAndView.
   */
  @GET("/{id}/map")
  @HxView("users/profile.hbs")
  public Map<String, Object> getUserMap(@PathParam String id) {
    return Map.of("id", id, "email", "edgar@example.com");
  }

  /**
   * TEST 3: The Basics (View Rendering) Verifies: @HxView keep existing model and view as they are
   */
  @GET("/{id}/map")
  @HxView("users/profile.hbs")
  public ModelAndView getUserModelAndView(@PathParam String id) {
    return new ModelAndView("users/profile-ext.hbs", getUser(id));
  }

  /**
   * TEST: The Declarative Powerhouse (OOB + Headers) Verifies: Multiple @HxOob appends, declarative
   * header generation, and trigger aggregation. The APT should generate `ctx.setResponseHeader()`
   * calls securely without reflection.
   */
  @POST
  @HxView("users/row.hbs")
  @HxOob("components/notification_toast")
  @HxOob("components/stats_counter")
  @HxTarget("#user-table")
  @HxSwap("beforeend")
  @HxTrigger("userCreated")
  @HxTrigger("updateGraph")
  public Map<String, Object> createUser(UserDto3936 dto) {
    // Save to DB...
    return Map.of("user", dto, "message", "User " + dto.name() + " created successfully!");
  }
}
