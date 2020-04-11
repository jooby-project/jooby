package issues;

import io.jooby.Jooby;
import io.jooby.MockRouter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1637 {

  @Test
  public void shouldForwardRoute() {
    Jooby app = new Jooby();

    app.get("/foo/{var}", ctx -> ctx.forward("/bar/" + ctx.path("var").value()));

    app.get("/bar/{id}", ctx -> ctx.path("id").intValue());

    MockRouter router = new MockRouter(app);

    assertEquals(123, router.get("/foo/123").value());
  }
}
