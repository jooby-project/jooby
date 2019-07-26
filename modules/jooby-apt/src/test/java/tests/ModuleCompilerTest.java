package tests;

import io.jooby.Context;
import io.jooby.MockRouter;
import io.jooby.compiler.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.JavaBeanParam;
import source.RouteWithMimeTypes;
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

  @Test
  public void routesWithMimeTypes() throws Exception {
    new MvcModuleCompilerRunner(new RouteWithMimeTypes())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("/consumes", router.get("/consumes").value());
          assertEquals("/consumes2", router.get("/consumes2").value());
          assertEquals("/produces", router.get("/produces").value());
          assertEquals("/consumes/produces", router.get("/consumes/produces").value());
          assertEquals("/method/produces", router.get("/method/produces").value());
          assertEquals("/class/produces", router.get("/class/produces").value());
          assertEquals("/method/consumes", router.get("/method/consumes").value());
          assertEquals("/class/consumes", router.get("/class/consumes").value());
        });
  }
}
