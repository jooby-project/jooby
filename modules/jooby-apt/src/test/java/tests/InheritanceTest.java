package tests;

import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.EmptySubClassController;
import source.OverrideMethodSubClassController;
import source.SubController;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InheritanceTest {

  @Test
  public void shouldWorkWithSubClass() throws Exception {
    new MvcModuleCompilerRunner(new SubController())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/base", rsp -> {
            assertEquals("base", rsp.value());
          });
          router.get("/base/withPath", rsp -> {
            assertEquals("withPath", rsp.value());
          });
          router.get("/base/subPath", rsp -> {
            assertEquals("subPath", rsp.value());
          });
        });
  }

  @Test
  public void shouldProcessEmptySubclasses() throws Exception {
    new MvcModuleCompilerRunner(new EmptySubClassController())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/override", rsp -> {
            assertEquals("base", rsp.value());
          });
          router.get("/override/withPath", rsp -> {
            assertEquals("withPath", rsp.value());
          });
        });
  }

  @Test
  public void shouldProcessWithOverrideMethodSubclasses() throws Exception {
    new MvcModuleCompilerRunner(new OverrideMethodSubClassController())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          router.get("/overrideMethod", rsp -> {
            assertEquals("base", rsp.value());
          });
          router.get("/overrideMethod/newpath", rsp -> {
            assertEquals("withPath", rsp.value());
          });
        });
  }
}
