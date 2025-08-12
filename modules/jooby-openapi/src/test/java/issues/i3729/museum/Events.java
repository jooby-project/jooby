/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import issues.i3729.museum.model.CreateSpecialEventRequest;

@Path("/special-events")
public class Events {

  /**
   * Create special event.
   *
   * @param request
   * @return
   * @throws IllegalArgumentException <code>400</code>
   * @throws java.util.NoSuchElementException <code>404</code>
   */
  @POST
  public CreateSpecialEventRequest createSpecialEvent(CreateSpecialEventRequest request) {
    return request;
  }
}
