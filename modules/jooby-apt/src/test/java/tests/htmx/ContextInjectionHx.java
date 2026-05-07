/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import java.util.Map;

import io.jooby.annotation.*;
import io.jooby.annotation.htmx.*;
import io.jooby.htmx.HtmxContext;

@Path("/users")
public class ContextInjectionHx {

  /**
   * TEST: Context Injection (Imperative State) Verifies: The APT generator sees `HtmxContext`,
   * instantiates it dynamically using `new HtmxContext(ctx)`, and passes it in. Verifies JSON
   * encoding for the trigger payload.
   */
  @PUT("/{id}")
  @HxView("users/profile.hbs")
  @HxOob("components/notification_toast")
  public User3936 updateUser(@PathParam String id, UserDto3936 dto, HtmxContext hx) {
    // Read incoming HTMX state
    if (hx.isBoosted()) {
      hx.pushUrl("/users/" + id);
    }

    hx.trigger("userUpdated", Map.of("id", id, "changes", dto));

    return new User3936(id, dto.name(), dto.email());
  }
}
