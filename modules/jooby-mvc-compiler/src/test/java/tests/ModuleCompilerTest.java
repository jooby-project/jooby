package tests;

import io.jooby.Context;
import io.jooby.MockRouter;
import io.jooby.compiler.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.JavaBeanParam;
import source.Routes;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleCompilerTest {
  @Test
  public void routes() throws Exception {
    new MvcModuleCompilerRunner(new Routes())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/path", router.get("/path").value());
          assertEquals(Arrays.asList("/path/subpath"), router.get("/path/subpath").value());
          assertTrue(router.get("/path/object").value() instanceof Context);
          assertTrue(router.post("/path/post").value() instanceof JavaBeanParam);
        });
  }
}
