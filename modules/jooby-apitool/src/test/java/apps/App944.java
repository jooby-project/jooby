package apps;

import org.jooby.Jooby;
import org.jooby.Results;

public class App944 extends Jooby {
  {
    delete("/", () -> {
      return Results.noContent();
    });
  }
}
