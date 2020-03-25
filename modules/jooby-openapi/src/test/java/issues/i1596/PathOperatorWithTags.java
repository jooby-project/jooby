package issues.i1596;

import io.jooby.Jooby;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "super")
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
