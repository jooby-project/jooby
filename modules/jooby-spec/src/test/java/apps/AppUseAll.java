package apps;

import org.jooby.Jooby;

public class AppUseAll extends Jooby {

  {

    use("/admin")
        .all(() -> null)
        .all(() -> null)
        .get("/:id", () -> null)
        .get("/mail/:id", () -> null);
  }
}
