/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.MediaType;

public class MediaTypeSchemaApp  extends Jooby {
  {
    get("/media-type", this::mediaType);
  }

  public MediaType mediaType(Context ctx) {
    return MediaType.json;
  }
}
