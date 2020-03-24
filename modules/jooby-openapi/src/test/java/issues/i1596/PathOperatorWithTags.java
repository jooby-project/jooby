package issues.i1596;

import io.jooby.Jooby;

public class PathOperatorWithTags extends Jooby {
  {
    path("/pets", () -> {

      get("/", ctx -> "...")
          .tags("local")
          .summary("List pets")
          .description("Pets ...");

    }).tags("top")
        .summary("API summary")
        .description("API description");
  }
}
