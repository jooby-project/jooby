package io.jooby.compiler;

import io.jooby.MockRouter;
import org.junit.jupiter.api.Test;
import source.Routes;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MvcModuleCompilerTest {
  @Test
  public void routes() throws Exception {
    new MvcModuleCompilerRunner(new Routes())
        .module(true, app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/path", router.get("/path").value());
          assertEquals("/path/subpath", router.get("/path/subpath").value());
        });
  }
}
