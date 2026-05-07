/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import java.util.Map;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.htmx.HxTrigger;
import io.jooby.annotation.htmx.HxView;

@Path("/users")
public class TriggersHx {

  @GET
  @HxView(value = "users/profile.hbs")
  @HxTrigger(value = "t1", phase = HxTrigger.Phase.TRIGGER)
  @HxTrigger(value = "t2", phase = HxTrigger.Phase.AFTER_SETTLE)
  @HxTrigger(value = "t3", phase = HxTrigger.Phase.AFTER_SWAP)
  public Map<String, Object> triggers() {
    return Map.of();
  }
}
