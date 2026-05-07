/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import java.util.Map;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.annotation.DELETE;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;
import io.jooby.htmx.HtmxResponse;

@Path("/users")
public class DynamicResponseHx {

  /**
   * TEST: The Dynamic Response Builder Verifies: The APT recognizes `HtmxResponse`, skips standard
   * view wrapping, and calls `((HtmxResponse) result).writeHeaders(ctx)` before returning.
   */
  @DELETE("/{id}")
  public HtmxResponse deleteUser(@PathParam String id, Context ctx) {
    boolean deleted = true; // Assume DB call

    if (deleted) {
      // Event-only response (200 OK, no content, just triggers)
      return HtmxResponse.empty()
          .trigger("userDeleted", id)
          .triggerAfterSwap("showToast", "User permanently removed.");
    } else {
      // Dynamic view routing based on logic
      return HtmxResponse.view("errors/notfound", Map.of("id", id))
          .status(StatusCode.NOT_FOUND)
          .target("#error-container")
          .swap("innerHTML");
    }
  }

  @DELETE("/{id}")
  public HtmxResponse deleteTask(@PathParam String id) {
    return HtmxResponse.empty().addOob("views/task_counter.hbs");
  }
}
