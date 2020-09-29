package tests.i2026;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;

public class Issue2026 {

  @Test
  public void shouldGenerateWriteMethodSignature() throws Exception {
    new MvcModuleCompilerRunner(new C2026())
        .example(Expected2026.class)
        .module(app -> {
          MockRouter router = new MockRouter(app);
          MockContext ctx = new MockContext();
          assertEquals("TODO...", router.get("/api/todo", ctx).value().toString());
        });
  }

}
