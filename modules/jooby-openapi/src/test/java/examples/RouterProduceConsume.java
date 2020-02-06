package examples;

import io.jooby.Jooby;
import io.jooby.MediaType;

public class RouterProduceConsume extends Jooby {

  {
    get("/", ctx -> "..")
        .consumes(MediaType.json, MediaType.js)
        .produces(MediaType.html, MediaType.text, MediaType.valueOf("some/type"));

    get("/json", ctx -> "..")
        .consumes(MediaType.json)
        .produces(MediaType.json);

    path("/api", () -> {
      get("/people", ctx -> "..")
          .consumes(MediaType.yaml)
          .produces(MediaType.yaml);
    });
  }

}
